package app.curser.core

enum class HostOs(val label: String) { MAC("Mac"), WINDOWS("Windows"), IPAD("iPad") }

data class HostDevice(val address: String, val name: String, val os: HostOs)

enum class BtStatus {
    UNSUPPORTED,
    NO_PERMISSION,
    DISABLED,
    REGISTERING,
    READY
}

sealed interface Connection {
    data object Disconnected : Connection
    data class Connecting(val device: HostDevice) : Connection
    data class Connected(val device: HostDevice) : Connection
}

data class UiState(
    val bt: BtStatus = BtStatus.REGISTERING,
    val bonded: List<HostDevice> = emptyList(),
    val connection: Connection = Connection.Disconnected,
    val hostOs: HostOs = HostOs.WINDOWS,
    val hostOsOverridden: Boolean = false
)

data class Settings(
    val pointerSpeed: Float = 1.2f,
    val scrollSpeed: Float = 1.0f,
    val naturalScroll: Boolean = true,
    val tapToClick: Boolean = true,
    val tapAndDrag: Boolean = true,
    val haptics: Boolean = true,
    val threeFingerDrag: Boolean = false,
    val orientation: Orientation = Orientation.AUTO
)

enum class Orientation { AUTO, PORTRAIT, LANDSCAPE }

data class TouchPoint(val id: Int, val x: Float, val y: Float)
data class TouchFrame(val timeMs: Long, val points: List<TouchPoint>)

data class Feedback(val label: String, val kind: Kind) {
    enum class Kind { CLICK, DRAG, GESTURE }
}

data class GestureGuideRow(val gesture: String, val action: String)
