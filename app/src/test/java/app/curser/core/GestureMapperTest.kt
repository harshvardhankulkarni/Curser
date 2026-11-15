package app.curser.core

import app.curser.core.gesture.Dir
import app.curser.core.gesture.GestureEvent
import app.curser.core.hid.Hid
import app.curser.core.mapping.GestureMapper
import app.curser.core.mapping.GestureTable
import app.curser.core.mapping.HidAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMapperTest {

    private fun wheelFor(os: HostOs, natural: Boolean, dx: Float = 0f, dy: Float = 18f): HidAction? =
        GestureMapper().map(GestureEvent.Scroll(dx, dy), os, Settings(naturalScroll = natural)).actions.firstOrNull()

    @Test
    fun scrollSignPerOs() {
        assertEquals(HidAction.Wheel(1, 0), wheelFor(HostOs.WINDOWS, true))
        assertEquals(HidAction.Wheel(-1, 0), wheelFor(HostOs.MAC, true))
        assertEquals(HidAction.Wheel(-1, 0), wheelFor(HostOs.WINDOWS, false))
        assertEquals(HidAction.Wheel(1, 0), wheelFor(HostOs.MAC, false))
        assertEquals(HidAction.Wheel(0, -1), wheelFor(HostOs.WINDOWS, true, dx = 18f, dy = 0f))
        assertEquals(HidAction.Wheel(0, 1), wheelFor(HostOs.IPAD, true, dx = 18f, dy = 0f))
    }

    @Test
    fun threeFingerSwipeUpPerOs() {
        val e = GestureEvent.Swipe(3, Dir.UP)
        val s = Settings()
        val m = GestureMapper()
        assertEquals(
            listOf<HidAction>(HidAction.Key(Hid.MOD_LCTRL, Hid.KEY_UP), HidAction.Key(0, 0)),
            m.map(e, HostOs.MAC, s).actions
        )
        assertEquals("Mission Control", m.map(e, HostOs.MAC, s).feedback?.label)
        assertEquals(
            listOf<HidAction>(HidAction.Key(Hid.MOD_LGUI, Hid.KEY_TAB), HidAction.Key(0, 0)),
            m.map(e, HostOs.WINDOWS, s).actions
        )
        assertEquals(
            listOf<HidAction>(HidAction.Key(Hid.MOD_LGUI, Hid.KEY_H), HidAction.Key(0, 0)),
            m.map(e, HostOs.IPAD, s).actions
        )
    }

    @Test
    fun pointerRemainderAccumulates() {
        val m = GestureMapper()
        val s = Settings()
        val first = m.map(GestureEvent.Move(0.1f, 0f), HostOs.MAC, s).actions
        assertTrue(first.isEmpty())
        var sum = 0
        repeat(100) {
            for (a in m.map(GestureEvent.Move(0.1f, 0f), HostOs.MAC, s).actions) {
                sum += (a as HidAction.MouseMove).dx
            }
        }
        assertTrue("sum=$sum", sum in 10..13)
    }

    @Test
    fun guideReadsSameTable() {
        val row = GestureTable.guide(HostOs.MAC).first { it.gesture == "Swipe up with three fingers" }
        assertEquals("Mission Control", row.action)
    }

    @Test
    fun windowsSwitcherSequence() {
        val m = GestureMapper()
        val s = Settings()
        val a = Hid.MOD_LALT
        assertEquals(
            listOf<HidAction>(HidAction.Key(a, 0), HidAction.Key(a, Hid.KEY_TAB), HidAction.Key(a, 0)),
            m.map(GestureEvent.Swipe(3, Dir.RIGHT), HostOs.WINDOWS, s).actions
        )
        assertEquals(
            listOf<HidAction>(HidAction.Key(a or Hid.MOD_LSHIFT, Hid.KEY_TAB), HidAction.Key(a, 0)),
            m.map(GestureEvent.SwipeRepeat(3, Dir.LEFT), HostOs.WINDOWS, s).actions
        )
        assertTrue(m.map(GestureEvent.SwipeRepeat(3, Dir.UP), HostOs.WINDOWS, s).actions.isEmpty())
        assertEquals(
            listOf<HidAction>(HidAction.Key(0, 0)),
            m.map(GestureEvent.SwipeEnd(3), HostOs.WINDOWS, s).actions
        )
        assertTrue(m.map(GestureEvent.SwipeEnd(3), HostOs.WINDOWS, s).actions.isEmpty())
    }

    @Test
    fun repeatWithoutOpenSwitcherIsIgnored() {
        val m = GestureMapper()
        m.map(GestureEvent.Swipe(3, Dir.UP), HostOs.WINDOWS, Settings())
        assertTrue(m.map(GestureEvent.SwipeRepeat(3, Dir.RIGHT), HostOs.WINDOWS, Settings()).actions.isEmpty())
        assertTrue(m.map(GestureEvent.SwipeEnd(3), HostOs.WINDOWS, Settings()).actions.isEmpty())
    }

    @Test
    fun ipadFourFingerSwipeUpIsGlobeUp() {
        assertEquals(
            listOf<HidAction>(
                HidAction.Consumer(Hid.CONSUMER_GLOBE),
                HidAction.Key(0, Hid.KEY_UP),
                HidAction.Key(0, 0),
                HidAction.Consumer(0)
            ),
            GestureMapper().map(GestureEvent.Swipe(4, Dir.UP), HostOs.IPAD, Settings()).actions
        )
    }

    @Test
    fun multiPinchPerOs() {
        val pinchIn = GestureEvent.MultiPinch(false)
        assertEquals(
            listOf<HidAction>(HidAction.Consumer(0x02A0), HidAction.Consumer(0)),
            GestureMapper().map(pinchIn, HostOs.MAC, Settings()).actions
        )
        assertTrue(GestureMapper().map(pinchIn, HostOs.WINDOWS, Settings()).actions.isEmpty())
        assertNull(GestureMapper().map(pinchIn, HostOs.WINDOWS, Settings()).feedback)
    }

    @Test
    fun windowsPinchUsesQueuedWheel() {
        assertEquals(
            listOf<HidAction>(HidAction.Key(Hid.MOD_LCTRL, 0), HidAction.Wheel(1, 0, queued = true), HidAction.Key(0, 0)),
            GestureMapper().map(GestureEvent.Pinch(true), HostOs.WINDOWS, Settings()).actions
        )
    }

    @Test
    fun guideOmitsNoneRows() {
        val win = GestureTable.guide(HostOs.WINDOWS).map { it.gesture }
        assertTrue(win.none { "four or five fingers" in it })
        assertTrue("Three-finger drag" in win)
        assertTrue(GestureTable.guide(HostOs.MAC).any { it.gesture == "Spread with four or five fingers" })
        assertTrue(GestureTable.guide(HostOs.IPAD).none { it.gesture == "Spread with four or five fingers" })
    }
}
