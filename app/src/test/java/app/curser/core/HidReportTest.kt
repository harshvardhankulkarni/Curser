package app.curser.core

import app.curser.core.hid.Hid
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HidReportTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun mouseReportIsLittleEndian() {
        val r = Hid.mouseReport(buttons = 0, x = -2, y = 300, wheel = 0, pan = 0)
        assertEquals(7, r.size)
        assertArrayEquals(bytes(0x00, 0xFE, 0xFF, 0x2C, 0x01, 0x00, 0x00), r)
    }

    @Test
    fun mouseReportClampsAndKeepsButtons() {
        val r = Hid.mouseReport(buttons = 0x0B, x = 99999, y = -99999, wheel = 500, pan = -500)
        assertArrayEquals(bytes(0x03, 0xFF, 0x7F, 0x01, 0x80, 0x7F, 0x81), r)
    }

    @Test
    fun keyboardReportLayout() {
        val r = Hid.keyboardReport(Hid.MOD_LGUI or Hid.MOD_LSHIFT, Hid.KEY_TAB)
        assertEquals(8, r.size)
        assertArrayEquals(bytes(0x0A, 0x00, 0x2B, 0, 0, 0, 0, 0), r)
    }

    @Test
    fun descriptorMatchesContract() {
        val d = Hid.DESCRIPTOR
        assertEquals(171, d.size)
        assertArrayEquals(bytes(0x05, 0x01, 0x09, 0x02, 0xA1, 0x01, 0x85, 0x01), d.copyOfRange(0, 8))
        assertEquals(0xC0.toByte(), d.last())
    }

    @Test
    fun consumerReportIsLittleEndian() {
        assertArrayEquals(bytes(0x9D, 0x02), Hid.consumerReport(Hid.CONSUMER_GLOBE))
        assertArrayEquals(bytes(0x00, 0x00), Hid.consumerReport(0))
    }
}
