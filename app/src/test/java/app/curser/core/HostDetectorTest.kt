package app.curser.core

import app.curser.core.detect.HostDetector
import app.curser.core.detect.HostDetector.Signals
import org.junit.Assert.assertEquals
import org.junit.Test

class HostDetectorTest {

    private fun sig(
        name: String? = null,
        major: Int? = null,
        device: Int? = null,
        uuids: List<String> = emptyList(),
        numLock: Boolean = false
    ) = Signals(name, major, device, uuids, numLock)

    private val apple = listOf("decafade-deca-fade-deca-deafdecacafe")

    @Test fun rule1IpadName() = assertEquals(HostOs.IPAD, HostDetector.detect(sig("Sam's iPad")))

    @Test fun rule2MacNames() {
        assertEquals(HostOs.MAC, HostDetector.detect(sig("MacBook Pro")))
        assertEquals(HostOs.MAC, HostDetector.detect(sig("Sam's Mac")))
        assertEquals(HostOs.MAC, HostDetector.detect(sig("iMac")))
    }

    @Test fun rule3NumLockBeatsAppleUuid() =
        assertEquals(HostOs.WINDOWS, HostDetector.detect(sig("Box", uuids = apple, numLock = true)))

    @Test fun rule4WindowsNames() {
        assertEquals(HostOs.WINDOWS, HostDetector.detect(sig("DESKTOP-AB12CD")))
        assertEquals(HostOs.WINDOWS, HostDetector.detect(sig("Surface Pro")))
        assertEquals(HostOs.WINDOWS, HostDetector.detect(sig("Sam's PC", uuids = apple)))
    }

    @Test fun rule5AppleUuidClasses() {
        assertEquals(HostOs.MAC, HostDetector.detect(sig("X", device = 0x010C, uuids = apple)))
        assertEquals(HostOs.MAC, HostDetector.detect(sig("X", device = 0x0104, uuids = apple)))
        assertEquals(HostOs.IPAD, HostDetector.detect(sig("X", major = 0x0200, uuids = apple)))
        assertEquals(HostOs.IPAD, HostDetector.detect(sig("X", device = 0x0110, uuids = apple)))
        assertEquals(HostOs.MAC, HostDetector.detect(sig("X", uuids = apple)))
    }

    @Test fun rule6DefaultWindows() = assertEquals(HostOs.WINDOWS, HostDetector.detect(sig("Unknown")))
}
