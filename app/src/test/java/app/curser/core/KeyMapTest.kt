package app.curser.core

import app.curser.core.hid.Hid
import app.curser.core.keyboard.KeyMap
import app.curser.core.keyboard.KeyStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyMapTest {

    @Test
    fun letters() {
        assertEquals(KeyStroke(0, 0x04), KeyMap.stroke('a'))
        assertEquals(KeyStroke(0, 0x1D), KeyMap.stroke('z'))
        assertEquals(KeyStroke(Hid.MOD_LSHIFT, 0x04), KeyMap.stroke('A'))
    }

    @Test
    fun digitsAndSymbols() {
        assertEquals(KeyStroke(0, 0x1E), KeyMap.stroke('1'))
        assertEquals(KeyStroke(0, 0x27), KeyMap.stroke('0'))
        assertEquals(KeyStroke(Hid.MOD_LSHIFT, 0x1F), KeyMap.stroke('@'))
        assertEquals(KeyStroke(Hid.MOD_LSHIFT, 0x27), KeyMap.stroke(')'))
        assertEquals(KeyStroke(0, 0x38), KeyMap.stroke('/'))
        assertEquals(KeyStroke(Hid.MOD_LSHIFT, 0x38), KeyMap.stroke('?'))
        assertEquals(KeyStroke(Hid.MOD_LSHIFT, 0x34), KeyMap.stroke('"'))
        assertEquals(KeyStroke(0, 0x35), KeyMap.stroke('`'))
    }

    @Test
    fun whitespace() {
        assertEquals(KeyStroke(0, KeyMap.SPACE), KeyMap.stroke(' '))
        assertEquals(KeyStroke(0, KeyMap.ENTER), KeyMap.stroke('\n'))
        assertEquals(KeyStroke(0, KeyMap.TAB), KeyMap.stroke('\t'))
    }

    @Test
    fun untypableIsNull() {
        assertNull(KeyMap.stroke('é'))
        assertNull(KeyMap.stroke('€'))
        assertNull(KeyMap.stroke('\r'))
    }

    @Test
    fun copyPerOs() {
        val c = 0x06
        assertEquals(Hid.MOD_LCTRL, KeyMap.shortcuts(HostOs.WINDOWS).first { it.label == "Copy" }.mods)
        assertEquals(c, KeyMap.shortcuts(HostOs.WINDOWS).first { it.label == "Copy" }.usage)
        assertEquals(Hid.MOD_LGUI, KeyMap.shortcuts(HostOs.MAC).first { it.label == "Copy" }.mods)
    }

    @Test
    fun ipadOmitsSomeShortcuts() {
        val labels = KeyMap.shortcuts(HostOs.IPAD).map { it.label }
        assertFalse("Save" in labels)
        assertEquals(11, labels.size)
        assertEquals(14, KeyMap.shortcuts(HostOs.MAC).size)
        assertEquals(14, KeyMap.shortcuts(HostOs.WINDOWS).size)
        assertTrue("Task Manager" in KeyMap.shortcuts(HostOs.WINDOWS).map { it.label })
    }
}
