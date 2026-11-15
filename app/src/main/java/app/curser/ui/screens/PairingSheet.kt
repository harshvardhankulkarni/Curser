package app.curser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.curser.core.HostOs
import app.curser.ui.components.BottomSheet
import app.curser.ui.components.Cell
import app.curser.ui.components.CellDivider
import app.curser.ui.components.GroupedSection
import app.curser.ui.components.SegmentedControl
import app.curser.ui.theme.CurserTheme
import app.curser.ui.theme.CurserType

private fun stepsFor(os: HostOs): List<String> = when (os) {
    HostOs.MAC -> listOf(
        "Open System Settings, then Bluetooth.",
        "Find this phone under Nearby Devices.",
        "Click Connect.",
        "If the Keyboard Setup Assistant window appears, close it.",
    )
    HostOs.WINDOWS -> listOf(
        "Open Settings, then Bluetooth & devices.",
        "Click Add device, then Bluetooth.",
        "Pick this phone from the list.",
    )
    HostOs.IPAD -> listOf(
        "Open Settings, then Bluetooth.",
        "Pick this phone under Other Devices.",
    )
}

@Composable
fun PairingSheet(visible: Boolean, onDismiss: () -> Unit, initialOs: HostOs) {
    var os by remember { mutableStateOf(initialOs) }
    BottomSheet(visible = visible, title = "Pair New Device", onDismiss = onDismiss) {
        val colors = CurserTheme.colors
        GroupedSection(
            footer = "This phone is visible to other devices for 5 minutes. Keep this screen open while you pair.",
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                SegmentedControl(
                    options = HostOs.entries.map { it.label },
                    selectedIndex = HostOs.entries.indexOf(os),
                    onSelect = { i -> os = HostOs.entries[i] },
                )
            }
        }
        GroupedSection(header = "ON YOUR ${os.label.uppercase()}") {
            stepsFor(os).forEachIndexed { i, text ->
                if (i > 0) CellDivider(startInset = 56.dp)
                Cell {
                    Box(
                        Modifier.size(24.dp).background(colors.blue, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = CurserType.Footnote, color = androidx.compose.ui.graphics.Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(text, style = CurserType.Body, color = colors.label, modifier = Modifier.weight(1f))
                }
            }
        }
        GroupedSection(header = "ALREADY PAIRED BEFORE?") {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    "If this phone was paired with the computer before you installed Curser, remove (\"Forget\") the pairing on both sides, then pair again while this screen is open.",
                    style = CurserType.Body,
                    color = colors.label,
                )
            }
        }
    }
}
