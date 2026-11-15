package app.curser.ui.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class StickyModsTest {

    @Test
    fun offArmedLockedOff() {
        val s = StickyMods()
        assertEquals(Sticky.OFF, s.state(StickyMods.CTRL))
        s.tap(StickyMods.CTRL)
        assertEquals(Sticky.ARMED, s.state(StickyMods.CTRL))
        s.tap(StickyMods.CTRL)
        assertEquals(Sticky.LOCKED, s.state(StickyMods.CTRL))
        s.tap(StickyMods.CTRL)
        assertEquals(Sticky.OFF, s.state(StickyMods.CTRL))
        assertEquals(0, s.mods)
    }

    @Test
    fun armedClearsAfterKeyLockedStays() {
        val s = StickyMods()
        s.tap(StickyMods.CTRL)
        s.tap(StickyMods.SHIFT)
        s.tap(StickyMods.SHIFT)
        assertEquals(StickyMods.CTRL or StickyMods.SHIFT, s.mods)
        s.keyReleased()
        assertEquals(Sticky.OFF, s.state(StickyMods.CTRL))
        assertEquals(Sticky.LOCKED, s.state(StickyMods.SHIFT))
        assertEquals(StickyMods.SHIFT, s.mods)
    }
}
