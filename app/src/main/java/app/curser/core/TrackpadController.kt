package app.curser.core

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import app.curser.core.detect.HostDetector
import app.curser.core.gesture.GestureEngine
import app.curser.core.gesture.GestureEvent
import app.curser.core.hid.HidPeripheral
import app.curser.core.keyboard.KeyMap
import app.curser.core.keyboard.Shortcut
import app.curser.core.mapping.GestureMapper
import app.curser.core.mapping.GestureTable
import app.curser.core.mapping.HidAction
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TrackpadController(context: Context) {

    private companion object {
        const val PREFS = "curser"
        const val KEY_OS = "os_"
        val HIDDEN_MAJOR_CLASSES = setOf(
            BluetoothClass.Device.Major.AUDIO_VIDEO,
            BluetoothClass.Device.Major.WEARABLE,
            BluetoothClass.Device.Major.PERIPHERAL
        )
    }

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val engine = GestureEngine()
    private val mapper = GestureMapper()
    private val numLockSeen: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private var kbMods = 0
    private var kbKey = 0

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _feedback = MutableSharedFlow<Feedback>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val feedback: SharedFlow<Feedback> = _feedback.asSharedFlow()

    private val timeout = Runnable {
        dispatch(engine.onTimeout(SystemClock.uptimeMillis()))
        rearm()
    }

    private val peripheral = HidPeripheral(app, object : HidPeripheral.Listener {
        override fun onStatus(status: BtStatus) {
            _state.update { it.copy(bt = status) }
        }

        override fun onConnection(device: BluetoothDevice, state: Int) = handleConnection(device, state)

        override fun onNumLock(device: BluetoothDevice) {
            if (numLockSeen.add(device.address)) rebuildConnection(device.address)
        }
    })

    init {
        syncEngine(_settings.value)
    }

    fun start() = refresh()

    fun stop() {
        resetInput(sendZeros = true)
        peripheral.stop()
        setConnection(Connection.Disconnected)
    }

    fun refresh() {
        val pre = peripheral.preStatus()
        if (pre != null) {
            peripheral.stop()
            resetInput(sendZeros = false)
            _state.update { it.copy(bt = pre, connection = Connection.Disconnected, bonded = bonded()) }
        } else {
            peripheral.start()
            _state.update { it.copy(bonded = bonded()) }
        }
    }

    fun connect(address: String) {
        val dev = peripheral.deviceFor(address) ?: return
        val cur = currentDevice()
        if (cur != null && cur.address != address) disconnect()
        setConnection(Connection.Connecting(hostDevice(dev)))
        peripheral.connect(dev)
    }

    fun disconnect() {
        val cur = currentDevice() ?: return
        resetInput(sendZeros = true)
        peripheral.deviceFor(cur.address)?.let { peripheral.disconnect(it) }
        setConnection(Connection.Disconnected)
    }

    fun setHostOs(os: HostOs?) {
        val addr = (currentDevice() ?: return).address
        val e = prefs.edit()
        if (os == null) e.remove(KEY_OS + addr) else e.putString(KEY_OS + addr, os.name)
        e.apply()
        rebuildConnection(addr)
        _state.update { it.copy(bonded = bonded()) }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val n = transform(_settings.value).let {
            it.copy(
                pointerSpeed = it.pointerSpeed.coerceIn(0.4f, 3.0f),
                scrollSpeed = it.scrollSpeed.coerceIn(0.4f, 3.0f)
            )
        }
        _settings.value = n
        syncEngine(n)
        prefs.edit()
            .putFloat("pointerSpeed", n.pointerSpeed)
            .putFloat("scrollSpeed", n.scrollSpeed)
            .putBoolean("naturalScroll", n.naturalScroll)
            .putBoolean("tapToClick", n.tapToClick)
            .putBoolean("tapAndDrag", n.tapAndDrag)
            .putBoolean("haptics", n.haptics)
            .putBoolean("threeFingerDrag", n.threeFingerDrag)
            .putString("orientation", n.orientation.name)
            .apply()
    }

    fun onTouch(frame: TouchFrame) {
        if (_state.value.connection !is Connection.Connected) return
        dispatch(engine.onFrame(frame))
        rearm()
    }

    fun gestureGuide(os: HostOs): List<GestureGuideRow> = GestureTable.guide(os)

    /** Absolute state of the on-screen keyboard: these modifiers and this key are down. usage 0 = no key. (0, 0) = all released. */
    fun setKeyboard(mods: Int, usage: Int) {
        if (!connected()) return
        kbMods = mods
        kbKey = usage
        peripheral.key(mods, usage)
    }

    /** One press and release, e.g. a shortcut. Leaves the setKeyboard state as it was. */
    fun tapKey(mods: Int, usage: Int) {
        if (!connected()) return
        peripheral.key(mods, usage)
        peripheral.key(kbMods, kbKey)
    }

    /** Types the text (voice input). Returns the number of characters skipped because KeyMap.stroke gave null. */
    fun typeText(text: String): Int {
        if (!connected()) return text.length
        var skipped = 0
        for (c in text) {
            val s = KeyMap.stroke(c)
            if (s == null) {
                skipped++
                continue
            }
            peripheral.key(s.mods, s.usage)
            peripheral.key(0, 0)
        }
        if (kbMods != 0 || kbKey != 0) peripheral.key(kbMods, kbKey)
        return skipped
    }

    fun shortcuts(os: HostOs): List<Shortcut> = KeyMap.shortcuts(os)

    private fun connected() = _state.value.connection is Connection.Connected

    private fun loadSettings(): Settings {
        val d = Settings()
        return Settings(
            pointerSpeed = prefs.getFloat("pointerSpeed", d.pointerSpeed),
            scrollSpeed = prefs.getFloat("scrollSpeed", d.scrollSpeed),
            naturalScroll = prefs.getBoolean("naturalScroll", d.naturalScroll),
            tapToClick = prefs.getBoolean("tapToClick", d.tapToClick),
            tapAndDrag = prefs.getBoolean("tapAndDrag", d.tapAndDrag),
            haptics = prefs.getBoolean("haptics", d.haptics),
            threeFingerDrag = prefs.getBoolean("threeFingerDrag", d.threeFingerDrag),
            orientation = Orientation.values().firstOrNull { it.name == prefs.getString("orientation", null) }
                ?: d.orientation
        )
    }

    private fun syncEngine(s: Settings) {
        engine.tapToClick = s.tapToClick
        engine.tapAndDrag = s.tapAndDrag
        engine.threeFingerDrag = s.threeFingerDrag
    }

    private fun dispatch(events: List<GestureEvent>) {
        if (events.isEmpty()) return
        val os = _state.value.hostOs
        val s = _settings.value
        for (e in events) {
            val m = mapper.map(e, os, s)
            for (a in m.actions) execute(a)
            m.feedback?.let { _feedback.tryEmit(it) }
        }
    }

    private fun execute(a: HidAction) {
        when (a) {
            is HidAction.MouseMove -> peripheral.mouseMove(a.dx, a.dy)
            is HidAction.Wheel -> if (a.queued) peripheral.wheelQueued(a.v, a.h) else peripheral.wheel(a.v, a.h)
            is HidAction.Buttons -> peripheral.setButtons(a.mask)
            // all-zero returns to the keyboard's state so a latched modifier stays down
            is HidAction.Key -> if (a.mods == 0 && a.key == 0) peripheral.key(kbMods, kbKey) else peripheral.key(a.mods, a.key)
            is HidAction.Consumer -> peripheral.consumer(a.usage)
        }
    }

    private fun rearm() {
        main.removeCallbacks(timeout)
        engine.deadlineMs?.let { main.postAtTime(timeout, it) }
    }

    private fun resetInput(sendZeros: Boolean) {
        main.removeCallbacks(timeout)
        engine.reset()
        mapper.reset()
        kbMods = 0
        kbKey = 0
        if (sendZeros && _state.value.connection is Connection.Connected) peripheral.releaseAll()
    }

    private fun currentDevice(): HostDevice? = when (val c = _state.value.connection) {
        is Connection.Connected -> c.device
        is Connection.Connecting -> c.device
        Connection.Disconnected -> null
    }

    // Runs on the Bluetooth executor thread.
    private fun handleConnection(device: BluetoothDevice, st: Int) {
        when (st) {
            BluetoothProfile.STATE_CONNECTING -> setConnection(Connection.Connecting(hostDevice(device)))
            BluetoothProfile.STATE_CONNECTED -> setConnection(Connection.Connected(hostDevice(device)))
            BluetoothProfile.STATE_DISCONNECTED -> if (currentDevice()?.address == device.address) {
                setConnection(Connection.Disconnected)
                main.post { resetInput(sendZeros = false) }
            }
        }
    }

    private fun setConnection(c: Connection) {
        _state.update { s ->
            when (c) {
                is Connection.Connected ->
                    s.copy(connection = c, hostOs = c.device.os, hostOsOverridden = hasOverride(c.device.address))
                is Connection.Connecting ->
                    s.copy(connection = c, hostOs = c.device.os, hostOsOverridden = hasOverride(c.device.address))
                Connection.Disconnected -> s.copy(connection = c, hostOsOverridden = false)
            }
        }
    }

    private fun rebuildConnection(addr: String) {
        val c = _state.value.connection
        val cur = currentDevice() ?: return
        if (cur.address != addr) return
        val dev = peripheral.deviceFor(addr) ?: return
        val nd = hostDevice(dev)
        setConnection(if (c is Connection.Connected) Connection.Connected(nd) else Connection.Connecting(nd))
    }

    private fun hasOverride(addr: String) = prefs.contains(KEY_OS + addr)

    private fun osFor(i: HidPeripheral.Info): HostOs {
        val saved = prefs.getString(KEY_OS + i.address, null)
            ?.let { s -> HostOs.values().firstOrNull { it.name == s } }
        return saved ?: HostDetector.detect(
            HostDetector.Signals(i.name, i.majorClass, i.deviceClass, i.uuids, i.address in numLockSeen)
        )
    }

    private fun hostDevice(i: HidPeripheral.Info) = HostDevice(i.address, i.name ?: i.address, osFor(i))

    private fun hostDevice(d: BluetoothDevice) = hostDevice(peripheral.info(d))

    private fun bonded(): List<HostDevice> = peripheral.bondedDevices()
        .map { peripheral.info(it) }
        .filter { it.majorClass !in HIDDEN_MAJOR_CLASSES }
        .map { hostDevice(it) }
        .sortedBy { it.name.lowercase() }
}
