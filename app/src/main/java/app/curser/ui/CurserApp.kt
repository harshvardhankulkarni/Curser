package app.curser.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.curser.core.Connection
import app.curser.core.HostDevice
import app.curser.core.Orientation
import app.curser.core.TrackpadController
import app.curser.ui.screens.DevicesScreen
import app.curser.ui.screens.ImmersiveEffect
import app.curser.ui.screens.PairingSheet
import app.curser.ui.screens.SettingsSheet
import app.curser.ui.screens.TrackpadScreen
import app.curser.ui.screens.findActivity
import app.curser.ui.theme.CurserTheme
import app.curser.ui.theme.Motion

private fun launchGuarded(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    intent: Intent,
    onFail: () -> Unit,
) {
    try {
        launcher.launch(intent)
    } catch (e: SecurityException) {
        onFail()
    } catch (e: ActivityNotFoundException) {
        onFail()
    }
}

@Composable
fun CurserApp(controller: TrackpadController) {
    val colors = CurserTheme.colors
    val state by controller.state.collectAsStateWithLifecycle()
    val settings by controller.settings.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { controller.refresh() }
    val enableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { controller.refresh() }
    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { controller.refresh() }

    var showPairing by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var onTrackpad by remember { mutableStateOf(false) }
    var lastHost by remember { mutableStateOf<HostDevice?>(null) }

    val connection = state.connection
    val isConnected = connection is Connection.Connected
    LaunchedEffect(isConnected) { onTrackpad = isConnected }
    LaunchedEffect(connection) {
        if (connection is Connection.Connected) lastHost = connection.device
    }
    LaunchedEffect(onTrackpad) { if (!onTrackpad) showSettings = false }

    val progress by animateFloatAsState(if (onTrackpad) 1f else 0f, Motion.sheet(), label = "push")

    val leaveTrackpad = {
        onTrackpad = false
        controller.disconnect()
    }

    val context = LocalContext.current
    LaunchedEffect(settings.orientation) {
        context.findActivity()?.requestedOrientation = when (settings.orientation) {
            Orientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            Orientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Orientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    ImmersiveEffect(enabled = onTrackpad)
    BackHandler(enabled = onTrackpad) { leaveTrackpad() }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = -size.width * 0.3f * progress.coerceIn(0f, 1f) },
        ) {
            DevicesScreen(
                state = state,
                onConnect = { address -> controller.connect(address) },
                onPairNew = {
                    launchGuarded(
                        discoverableLauncher,
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300),
                        onFail = { controller.refresh() },
                    )
                    showPairing = true
                },
                onAllowBluetooth = {
                    if (Build.VERSION.SDK_INT >= 31) {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE),
                        )
                    } else {
                        controller.refresh()
                    }
                },
                onTurnOnBluetooth = {
                    launchGuarded(
                        enableLauncher,
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        onFail = { controller.refresh() },
                    )
                },
            )
            // dims the outgoing screen as the trackpad slides over it
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(Color.Black, alpha = 0.18f * progress.coerceIn(0f, 1f)) },
            )
        }

        if (onTrackpad || progress > 0.001f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = size.width * (1f - progress.coerceIn(0f, 1f)) },
            ) {
                TrackpadScreen(
                    controller = controller,
                    settings = settings,
                    deviceName = lastHost?.name ?: "Trackpad",
                    os = state.hostOs,
                    onBack = { leaveTrackpad() },
                    onOpenSettings = { showSettings = true },
                )
            }
        }

        PairingSheet(visible = showPairing, onDismiss = { showPairing = false }, initialOs = state.hostOs)
        SettingsSheet(
            visible = showSettings,
            onDismiss = { showSettings = false },
            state = state,
            settings = settings,
            controller = controller,
        )
    }
}
