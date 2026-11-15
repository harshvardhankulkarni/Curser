package app.curser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curser.core.BtStatus
import app.curser.core.Connection
import app.curser.core.HostDevice
import app.curser.core.UiState
import app.curser.ui.components.ActivityIndicator
import app.curser.ui.components.Cell
import app.curser.ui.components.CellDivider
import app.curser.ui.components.CellText
import app.curser.ui.components.Glyph
import app.curser.ui.components.GlyphIcon
import app.curser.ui.components.GroupedSection
import app.curser.ui.components.IconTile
import app.curser.ui.components.LargeTitleScaffold
import app.curser.ui.components.PrimaryButton
import app.curser.ui.components.glyph
import app.curser.ui.theme.CurserTheme
import app.curser.ui.theme.CurserType

@Composable
fun DevicesScreen(
    state: UiState,
    onConnect: (String) -> Unit,
    onPairNew: () -> Unit,
    onAllowBluetooth: () -> Unit,
    onTurnOnBluetooth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CurserTheme.colors
    LargeTitleScaffold(title = "Curser", modifier = modifier) {
        when (state.bt) {
            BtStatus.NO_PERMISSION -> Banner(
                title = "Bluetooth Access Needed",
                body = "Curser needs permission to use Bluetooth so your computer can see this phone as a trackpad.",
                action = "Allow Bluetooth",
                onAction = onAllowBluetooth,
            )
            BtStatus.DISABLED -> Banner(
                title = "Bluetooth Is Off",
                body = "Turn on Bluetooth to connect to your computer.",
                action = "Turn On Bluetooth",
                onAction = onTurnOnBluetooth,
            )
            BtStatus.UNSUPPORTED -> Banner(
                title = "Not Supported",
                body = "This phone cannot act as a Bluetooth input device.",
                action = null,
                onAction = {},
            )
            else -> {}
        }

        val ready = state.bt == BtStatus.READY
        GroupedSection(header = "MY DEVICES") {
            if (state.bonded.isEmpty()) {
                EmptyDevices()
            } else {
                state.bonded.forEachIndexed { i, device ->
                    if (i > 0) CellDivider(startInset = 58.dp)
                    DeviceCell(device, state.connection, enabled = ready, onClick = { onConnect(device.address) })
                }
            }
        }

        val canPair = state.bt != BtStatus.UNSUPPORTED && state.bt != BtStatus.NO_PERMISSION
        GroupedSection(
            footer = "Curser turns this phone into a Bluetooth trackpad for your Mac, Windows PC or iPad. Nothing to install on the computer.",
        ) {
            Cell(onClick = onPairNew, enabled = canPair) {
                IconTile(Glyph.Plus, colors.blue)
                Spacer(Modifier.width(12.dp))
                Text("Pair New Device", style = CurserType.Body, color = colors.blue, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DeviceCell(device: HostDevice, connection: Connection, enabled: Boolean, onClick: () -> Unit) {
    val colors = CurserTheme.colors
    Cell(onClick = onClick, enabled = enabled) {
        IconTile(device.os.glyph(), colors.blue)
        Spacer(Modifier.width(12.dp))
        CellText(device.name, device.os.label)
        Spacer(Modifier.width(8.dp))
        when {
            connection is Connection.Connecting && connection.device.address == device.address ->
                ActivityIndicator(colors.secondary)
            connection is Connection.Connected && connection.device.address == device.address ->
                Text("Connected", style = CurserType.Body, color = colors.secondary)
            else -> GlyphIcon(Glyph.ChevronRight, colors.secondary, size = 16.dp)
        }
    }
}

@Composable
private fun EmptyDevices() {
    val colors = CurserTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlyphIcon(Glyph.Laptop, colors.secondary, size = 40.dp)
        Spacer(Modifier.height(12.dp))
        Text("No Computers Yet", style = CurserType.Headline, color = colors.label, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap Pair New Device below, then pick this phone in your computer's Bluetooth settings.",
            style = CurserType.Footnote,
            color = colors.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Banner(title: String, body: String, action: String?, onAction: () -> Unit) {
    val colors = CurserTheme.colors
    GroupedSection {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Glyph.Bluetooth, if (action == null) colors.red else colors.orange)
                Spacer(Modifier.width(12.dp))
                Text(title, style = CurserType.Headline, color = colors.label)
            }
            Text(body, style = CurserType.Body, color = colors.secondary)
            if (action != null) PrimaryButton(action, onAction)
        }
    }
}
