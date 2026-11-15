package app.curser.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.curser.core.HostOs
import app.curser.core.Orientation
import app.curser.core.Settings
import app.curser.core.TrackpadController
import app.curser.core.UiState
import app.curser.ui.components.BottomSheet
import app.curser.ui.components.Cell
import app.curser.ui.components.CellDivider
import app.curser.ui.components.GroupedSection
import app.curser.ui.components.SegmentedControl
import app.curser.ui.components.SliderCell
import app.curser.ui.components.SwitchCell
import app.curser.ui.theme.CurserTheme
import app.curser.ui.theme.CurserType
import java.util.Locale

private val ORIENTATION_OPTIONS = listOf("Auto", "Portrait", "Landscape")
private val HOST_OPTIONS = listOf("Auto", "Mac", "Windows", "iPad")

@Composable
fun SettingsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    state: UiState,
    settings: Settings,
    controller: TrackpadController,
) {
    BottomSheet(visible = visible, title = "Trackpad", onDismiss = onDismiss) {
        val colors = CurserTheme.colors

        val hostIndex = if (state.hostOsOverridden) 1 + HostOs.entries.indexOf(state.hostOs) else 0
        GroupedSection(
            header = "COMPUTER",
            footer = if (state.hostOsOverridden) {
                "Using ${state.hostOs.label}, chosen by you. Pick Auto to detect it again."
            } else {
                "Detected: ${state.hostOs.label}. Gestures and shortcuts follow this system."
            },
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                SegmentedControl(
                    options = HOST_OPTIONS,
                    selectedIndex = hostIndex,
                    onSelect = { i -> controller.setHostOs(if (i == 0) null else HostOs.entries[i - 1]) },
                )
            }
        }

        GroupedSection(header = "SPEED") {
            SliderCell(
                title = "Tracking Speed",
                valueText = formatSpeed(settings.pointerSpeed),
                value = settings.pointerSpeed,
                onValueChange = { v -> controller.updateSettings { it.copy(pointerSpeed = v) } },
                valueRange = 0.4f..3.0f,
            )
            CellDivider()
            SliderCell(
                title = "Scroll Speed",
                valueText = formatSpeed(settings.scrollSpeed),
                value = settings.scrollSpeed,
                onValueChange = { v -> controller.updateSettings { it.copy(scrollSpeed = v) } },
                valueRange = 0.4f..3.0f,
            )
        }

        GroupedSection(header = "BEHAVIOR") {
            SwitchCell("Natural Scrolling", settings.naturalScroll) { v ->
                controller.updateSettings { it.copy(naturalScroll = v) }
            }
            CellDivider()
            SwitchCell("Tap to Click", settings.tapToClick) { v ->
                controller.updateSettings { it.copy(tapToClick = v) }
            }
            CellDivider()
            SwitchCell("Tap and Drag", settings.tapAndDrag) { v ->
                controller.updateSettings { it.copy(tapAndDrag = v) }
            }
            CellDivider()
            SwitchCell("Haptics", settings.haptics) { v ->
                controller.updateSettings { it.copy(haptics = v) }
            }
        }

        GroupedSection(footer = "Three-finger swipes are off while this is on.") {
            SwitchCell("Three-finger drag", settings.threeFingerDrag) { v ->
                controller.updateSettings { it.copy(threeFingerDrag = v) }
            }
        }

        GroupedSection(header = "DISPLAY") {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                SegmentedControl(
                    options = ORIENTATION_OPTIONS,
                    selectedIndex = settings.orientation.ordinal,
                    onSelect = { i -> controller.updateSettings { it.copy(orientation = Orientation.entries[i]) } },
                )
            }
        }

        val rows = controller.gestureGuide(state.hostOs)
        GroupedSection(header = "GESTURES ON ${state.hostOs.label.uppercase(Locale.ROOT)}") {
            rows.forEachIndexed { i, row ->
                if (i > 0) CellDivider()
                Cell {
                    Column(Modifier.fillMaxWidth()) {
                        Text(row.action, style = CurserType.Body, color = colors.label)
                        Text(row.gesture, style = CurserType.Footnote, color = colors.secondary)
                    }
                }
            }
        }
    }
}

private fun formatSpeed(v: Float): String = String.format(Locale.US, "%.1fx", v)
