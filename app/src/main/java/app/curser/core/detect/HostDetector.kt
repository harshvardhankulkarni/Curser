package app.curser.core.detect

import app.curser.core.HostOs

object HostDetector {
    data class Signals(
        val name: String?,
        val majorClass: Int?,
        val deviceClass: Int?,
        val uuids: List<String>,
        val numLockSeen: Boolean
    )

    // Hard-coded BluetoothClass values so this file has no Android imports.
    private const val COMPUTER_DESKTOP = 0x0104
    private const val COMPUTER_LAPTOP = 0x010C
    private const val COMPUTER_HANDHELD = 0x0110
    private const val COMPUTER_PALM = 0x0114
    private const val MAJOR_PHONE = 0x0200

    private val macNames = listOf("macbook", "imac", "mac mini", "mac studio", "mac pro")
    private val macWord = Regex("\\bmac\\b")
    private val pcWord = Regex("\\bpc\\b")

    fun detect(s: Signals): HostOs {
        val n = s.name?.lowercase().orEmpty()
        if ("ipad" in n) return HostOs.IPAD
        if (macNames.any { it in n } || macWord.containsMatchIn(n)) return HostOs.MAC
        if (s.numLockSeen) return HostOs.WINDOWS
        if (n.startsWith("desktop-") || n.startsWith("laptop-") ||
            "windows" in n || "surface" in n || pcWord.containsMatchIn(n)
        ) return HostOs.WINDOWS
        if (s.uuids.any { "deca-fade" in it.lowercase() }) {
            val dc = s.deviceClass
            return when {
                dc == COMPUTER_LAPTOP || dc == COMPUTER_DESKTOP -> HostOs.MAC
                s.majorClass == MAJOR_PHONE || dc == COMPUTER_HANDHELD || dc == COMPUTER_PALM -> HostOs.IPAD
                else -> HostOs.MAC
            }
        }
        return HostOs.WINDOWS
    }
}
