package app.curser.core.keyboard

import app.curser.core.HostOs
import app.curser.core.hid.Hid

data class KeyStroke(val mods: Int, val usage: Int)
data class Shortcut(val label: String, val mods: Int, val usage: Int)

object KeyMap {
    // HID keyboard usages
    const val ENTER = 0x28; const val ESC = 0x29; const val BACKSPACE = 0x2A; const val TAB = 0x2B
    const val SPACE = 0x2C; const val CAPS_LOCK = 0x39
    const val F1 = 0x3A                                   // F1..F12 = 0x3A..0x45
    const val PRINT_SCREEN = 0x46; const val INSERT = 0x49; const val HOME = 0x4A; const val PAGE_UP = 0x4B
    const val DELETE = 0x4C; const val END = 0x4D; const val PAGE_DOWN = 0x4E
    const val RIGHT = 0x4F; const val LEFT = 0x50; const val DOWN = 0x51; const val UP = 0x52

    private const val SHIFT = Hid.MOD_LSHIFT
    private const val PUNCT = "-=[]\\;'`,./"
    private val PUNCT_USAGES = intArrayOf(0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38)
    private const val PUNCT_SHIFTED = "_+{}|:\"~<>?"
    private const val DIGITS_SHIFTED = "!@#$%^&*()"

    /** US layout. null = this character cannot be typed. */
    fun stroke(c: Char): KeyStroke? = when (c) {
        in 'a'..'z' -> KeyStroke(0, 0x04 + (c - 'a'))
        in 'A'..'Z' -> KeyStroke(SHIFT, 0x04 + (c - 'A'))
        in '1'..'9' -> KeyStroke(0, 0x1E + (c - '1'))
        '0' -> KeyStroke(0, 0x27)
        '\n' -> KeyStroke(0, ENTER)
        '\t' -> KeyStroke(0, TAB)
        ' ' -> KeyStroke(0, SPACE)
        else -> PUNCT.indexOf(c).takeIf { it >= 0 }?.let { KeyStroke(0, PUNCT_USAGES[it]) }
            ?: DIGITS_SHIFTED.indexOf(c).takeIf { it >= 0 }?.let { KeyStroke(SHIFT, 0x1E + it) }
            ?: PUNCT_SHIFTED.indexOf(c).takeIf { it >= 0 }?.let { KeyStroke(SHIFT, PUNCT_USAGES[it]) }
    }

    fun shortcuts(os: HostOs): List<Shortcut> {
        val win = os == HostOs.WINDOWS
        val p = if (win) Hid.MOD_LCTRL else Hid.MOD_LGUI
        val key = { c: Char -> stroke(c)!!.usage }
        return buildList {
            add(Shortcut("Copy", p, key('c')))
            add(Shortcut("Paste", p, key('v')))
            add(Shortcut("Cut", p, key('x')))
            add(Shortcut("Undo", p, key('z')))
            add(if (win) Shortcut("Redo", Hid.MOD_LCTRL, key('y')) else Shortcut("Redo", SHIFT or Hid.MOD_LGUI, key('z')))
            add(Shortcut("Select All", p, key('a')))
            if (os != HostOs.IPAD) add(Shortcut("Save", p, key('s')))
            add(Shortcut("Find", p, key('f')))
            add(Shortcut("New Tab", p, key('t')))
            add(Shortcut("Close Tab", p, key('w')))
            add(
                when (os) {
                    HostOs.WINDOWS -> Shortcut("Screenshot", SHIFT or Hid.MOD_LGUI, key('s'))
                    HostOs.MAC -> Shortcut("Screenshot", SHIFT or Hid.MOD_LGUI, key('5'))
                    HostOs.IPAD -> Shortcut("Screenshot", SHIFT or Hid.MOD_LGUI, key('3'))
                },
            )
            add(if (win) Shortcut("Lock", Hid.MOD_LGUI, key('l')) else Shortcut("Lock", Hid.MOD_LCTRL or Hid.MOD_LGUI, key('q')))
            when (os) {
                HostOs.WINDOWS -> {
                    add(Shortcut("Close Window", Hid.MOD_LALT, F1 + 3))
                    add(Shortcut("Task Manager", Hid.MOD_LCTRL or SHIFT, ESC))
                }
                HostOs.MAC -> {
                    add(Shortcut("Quit", Hid.MOD_LGUI, key('q')))
                    add(Shortcut("Force Quit", Hid.MOD_LALT or Hid.MOD_LGUI, ESC))
                }
                HostOs.IPAD -> Unit
            }
        }
    }
}
