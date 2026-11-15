package app.curser.core.hid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import app.curser.core.BtStatus
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** All Bluetooth work runs on one single-thread executor; listener callbacks arrive on that thread. */
@SuppressLint("MissingPermission")
class HidPeripheral(context: Context, private val listener: Listener) {

    interface Listener {
        fun onStatus(status: BtStatus)
        fun onConnection(device: BluetoothDevice, state: Int)
        fun onNumLock(device: BluetoothDevice)
    }

    data class Info(
        val address: String,
        val name: String?,
        val majorClass: Int?,
        val deviceClass: Int?,
        val uuids: List<String>
    )

    private companion object {
        const val MOUSE_INTERVAL_NS = 8_000_000L
        const val QUEUE_GAP_NS = 15_000_000L
        const val PROXY_TIMEOUT_MS = 4_000L
    }

    private val app = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private val exec: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "curser-bt").apply { isDaemon = true }
    }

    @Volatile private var hid: BluetoothHidDevice? = null
    @Volatile private var host: BluetoothDevice? = null
    @Volatile private var wanted = false
    @Volatile private var proxyRequested = false
    @Volatile private var registered = false

    private val lock = Any()
    private var pendX = 0
    private var pendY = 0
    private var pendV = 0
    private var pendH = 0
    private var buttons = 0
    private var lastFlushNs = 0L
    private var flushQueued = false

    // Touched only on the executor thread.
    private val queue = ArrayDeque<() -> Unit>()
    private var lastQueuedNs = System.nanoTime() - QUEUE_GAP_NS
    private var pumpScheduled = false

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private inline fun attempt(block: () -> Unit) {
        if (!hasPermission()) return
        try {
            block()
        } catch (_: SecurityException) {
        }
    }

    private inline fun <T> attemptOr(default: T, block: () -> T): T {
        if (!hasPermission()) return default
        return try {
            block()
        } catch (_: SecurityException) {
            default
        }
    }

    /** Non-null when the HID app cannot be registered yet. */
    fun preStatus(): BtStatus? = when {
        adapter == null -> BtStatus.UNSUPPORTED
        !hasPermission() -> BtStatus.NO_PERMISSION
        !attemptOr(false) { adapter?.isEnabled == true } -> BtStatus.DISABLED
        else -> null
    }

    fun bondedDevices(): Set<BluetoothDevice> =
        attemptOr(emptySet()) { adapter?.bondedDevices ?: emptySet() }

    fun deviceFor(address: String): BluetoothDevice? =
        try {
            adapter?.getRemoteDevice(address)
        } catch (_: IllegalArgumentException) {
            null
        }

    fun info(d: BluetoothDevice): Info =
        attemptOr(Info(d.address, null, null, null, emptyList())) {
            val c = d.bluetoothClass
            Info(d.address, d.name, c?.majorDeviceClass, c?.deviceClass, d.uuids?.map { it.toString() } ?: emptyList())
        }

    fun start() = exec.execute { startOnExec() }

    fun stop() = exec.execute {
        wanted = false
        val h = hid
        if (h != null) {
            attempt { h.unregisterApp() }
            attempt { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, h) }
        }
        hid = null
        host = null
        registered = false
        proxyRequested = false
    }

    fun connect(device: BluetoothDevice) = exec.execute {
        val h = hid
        val ok = h != null && attemptOr(false) { h.connect(device) }
        if (!ok) listener.onConnection(device, BluetoothProfile.STATE_DISCONNECTED)
    }

    fun disconnect(device: BluetoothDevice) = exec.execute {
        attempt { hid?.disconnect(device) }
    }

    fun mouseMove(dx: Int, dy: Int) {
        synchronized(lock) {
            pendX += dx
            pendY += dy
            if (flushQueued) return
            flushQueued = true
            val wait = (MOUSE_INTERVAL_NS - (System.nanoTime() - lastFlushNs)).coerceAtLeast(0L)
            exec.schedule(Runnable { flush(scheduled = true) }, wait, TimeUnit.NANOSECONDS)
        }
    }

    fun wheel(v: Int, h: Int) {
        synchronized(lock) {
            pendV += v
            pendH += h
        }
        exec.execute { flush() }
    }

    fun setButtons(mask: Int) {
        // Set on the executor: a press and release queued back to back must each send their own mask.
        exec.execute {
            synchronized(lock) { buttons = mask }
            flush(force = true)
        }
    }

    fun key(mods: Int, key: Int) =
        enqueue { send(Hid.REPORT_KEYBOARD, Hid.keyboardReport(mods, key)) }

    fun consumer(usage: Int) =
        enqueue { send(Hid.REPORT_CONSUMER, Hid.consumerReport(usage)) }

    /** Wheel report in order with key and consumer reports. */
    fun wheelQueued(v: Int, h: Int) = enqueue {
        val b = synchronized(lock) { buttons }
        send(Hid.REPORT_MOUSE, Hid.mouseReport(b, 0, 0, v, h))
    }

    /** Drops pending motion and queued reports, then sends all-zero mouse, keyboard and consumer reports. */
    fun releaseAll() {
        synchronized(lock) {
            pendX = 0
            pendY = 0
            pendV = 0
            pendH = 0
            buttons = 0
        }
        exec.execute {
            queue.clear()
            send(Hid.REPORT_MOUSE, Hid.mouseReport(0, 0, 0, 0, 0))
            send(Hid.REPORT_KEYBOARD, Hid.keyboardReport(0, 0))
            send(Hid.REPORT_CONSUMER, Hid.consumerReport(0))
        }
    }

    private fun enqueue(item: () -> Unit) = exec.execute {
        queue.addLast(item)
        pump()
    }

    // Executor thread only. Sends queued items in order, at least QUEUE_GAP_NS apart.
    private fun pump() {
        if (pumpScheduled) return
        while (queue.isNotEmpty()) {
            val wait = QUEUE_GAP_NS - (System.nanoTime() - lastQueuedNs)
            if (wait > 0) {
                pumpScheduled = true
                exec.schedule(Runnable { pumpScheduled = false; pump() }, wait, TimeUnit.NANOSECONDS)
                return
            }
            queue.removeFirst()()
            lastQueuedNs = System.nanoTime()
        }
    }

    private fun flush(scheduled: Boolean = false, force: Boolean = false) {
        val report = synchronized(lock) {
            if (scheduled) flushQueued = false
            if (!force && pendX == 0 && pendY == 0 && pendV == 0 && pendH == 0) return
            val r = Hid.mouseReport(buttons, pendX, pendY, pendV, pendH)
            // shortcut: motion beyond the report clamp is dropped, fine for 8 ms of finger travel
            pendX = 0
            pendY = 0
            pendV = 0
            pendH = 0
            lastFlushNs = System.nanoTime()
            r
        }
        send(Hid.REPORT_MOUSE, report)
    }

    private fun send(id: Int, payload: ByteArray) {
        val h = hid ?: return
        val d = host ?: return
        attempt { h.sendReport(d, id, payload) }
    }

    private fun startOnExec() {
        wanted = true
        val pre = preStatus()
        if (pre != null) {
            listener.onStatus(pre)
            return
        }
        val h = hid
        if (h != null) {
            if (registered) listener.onStatus(BtStatus.READY) else register(h)
            return
        }
        if (proxyRequested) return
        val a = adapter ?: return
        listener.onStatus(BtStatus.REGISTERING)
        val ok = attemptOr(false) { a.getProfileProxy(app, proxyListener, BluetoothProfile.HID_DEVICE) }
        if (!ok) {
            listener.onStatus(BtStatus.UNSUPPORTED)
            return
        }
        proxyRequested = true
        exec.schedule(Runnable {
            if (wanted && hid == null) listener.onStatus(BtStatus.UNSUPPORTED)
        }, PROXY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }

    private fun register(h: BluetoothHidDevice) {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "Curser Trackpad", "Trackpad and keyboard", "Curser",
            BluetoothHidDevice.SUBCLASS1_COMBO, Hid.DESCRIPTOR
        )
        val ok = attemptOr(false) { h.registerApp(sdp, null, null, exec, callback) }
        if (!ok) listener.onStatus(BtStatus.UNSUPPORTED)
    }

    private val proxyListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            exec.execute {
                val h = proxy as? BluetoothHidDevice ?: return@execute
                hid = h
                if (wanted) {
                    register(h)
                } else {
                    attempt { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, h) }
                    hid = null
                    proxyRequested = false
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            exec.execute {
                hid = null
                host = null
                registered = false
                proxyRequested = false
                if (wanted) listener.onStatus(BtStatus.REGISTERING)
            }
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, isRegistered: Boolean) {
            registered = isRegistered
            listener.onStatus(if (isRegistered) BtStatus.READY else BtStatus.REGISTERING)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            device ?: return
            if (state == BluetoothProfile.STATE_CONNECTED) {
                host = device
            } else if (state == BluetoothProfile.STATE_DISCONNECTED && host?.address == device.address) {
                host = null
            }
            listener.onConnection(device, state)
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            device ?: return
            val size = when {
                id.toInt() == Hid.REPORT_MOUSE -> Hid.MOUSE_PAYLOAD_SIZE
                id.toInt() == Hid.REPORT_CONSUMER -> Hid.CONSUMER_PAYLOAD_SIZE
                type == BluetoothHidDevice.REPORT_TYPE_OUTPUT -> 1
                else -> Hid.KEYBOARD_PAYLOAD_SIZE
            }
            attempt { hid?.replyReport(device, type, id, ByteArray(size)) }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            device ?: return
            if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT && id.toInt() == Hid.REPORT_KEYBOARD) {
                onLed(device, data)
            }
            attempt { hid?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS) }
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            device ?: return
            if (reportId.toInt() == Hid.REPORT_KEYBOARD) onLed(device, data)
        }
    }

    private fun onLed(device: BluetoothDevice, data: ByteArray?) {
        if (data != null && data.isNotEmpty() && (data[0].toInt() and 0x01) != 0) listener.onNumLock(device)
    }
}
