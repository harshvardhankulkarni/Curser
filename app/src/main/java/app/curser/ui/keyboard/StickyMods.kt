package app.curser.ui.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.curser.core.hid.Hid

enum class Sticky { OFF, ARMED, LOCKED }

/** Ctrl, Alt, Win/Cmd, Shift. One tap arms (clears after the next key), second tap locks, third clears. */
class StickyMods {
    private var states by mutableStateOf(emptyMap<Int, Sticky>())

    val mods: Int get() = states.keys.fold(0) { a, m -> a or m }

    fun state(mod: Int): Sticky = states[mod] ?: Sticky.OFF

    fun tap(mod: Int) {
        states = when (state(mod)) {
            Sticky.OFF -> states + (mod to Sticky.ARMED)
            Sticky.ARMED -> states + (mod to Sticky.LOCKED)
            Sticky.LOCKED -> states - mod
        }
    }

    /** Call after a non-modifier key goes up. */
    fun keyReleased() {
        states = states.filterValues { it == Sticky.LOCKED }
    }

    companion object {
        const val CTRL = Hid.MOD_LCTRL
        const val SHIFT = Hid.MOD_LSHIFT
        const val ALT = Hid.MOD_LALT
        const val GUI = Hid.MOD_LGUI
    }
}
