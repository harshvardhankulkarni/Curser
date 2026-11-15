package app.curser.core.mapping

import app.curser.core.GestureGuideRow
import app.curser.core.HostOs
import app.curser.core.gesture.Dir
import app.curser.core.gesture.GestureEvent
import app.curser.core.hid.Hid

/** Single source for keyed gestures: the mapper executes it, gestureGuide() prints it. */
object GestureTable {
    sealed interface Cell {
        val name: String
    }

    data class Combo(override val name: String, val mods: Int, val key: Int) : Cell
    data class Media(override val name: String, val usage: Int) : Cell
    data class Globe(override val name: String, val key: Int) : Cell
    data class Switcher(override val name: String, val mod: Int) : Cell
    data object None : Cell {
        override val name = ""
    }

    /** trigger == null means a guide-only row (handled by dedicated mapper code). */
    class Row(val gesture: String, val trigger: GestureEvent?, val cells: Map<HostOs, Cell>)

    private const val C = Hid.MOD_LCTRL
    private const val A = Hid.MOD_LALT
    private const val G = Hid.MOD_LGUI

    private fun combo(name: String, mods: Int, key: Int) = Combo(name, mods, key)
    private fun switcher(mod: Int) = Switcher("Switch Apps", mod)

    private fun row(gesture: String, trigger: GestureEvent?, mac: Cell, win: Cell, ipad: Cell) =
        Row(gesture, trigger, mapOf(HostOs.MAC to mac, HostOs.WINDOWS to win, HostOs.IPAD to ipad))

    private fun info(gesture: String, name: String) = combo(name, 0, 0).let { row(gesture, null, it, it, it) }

    private fun swipe(n: Int, dir: Dir, mac: Cell, win: Cell, ipad: Cell): Row {
        val count = if (n == 3) "three" else "four"
        return row("Swipe ${dir.name.lowercase()} with $count fingers", GestureEvent.Swipe(n, dir), mac, win, ipad)
    }

    private fun tap(n: Int, mac: Cell, win: Cell, ipad: Cell): Row {
        val count = if (n == 3) "three" else "four"
        return row("Tap with $count fingers", GestureEvent.Tap(n), mac, win, ipad)
    }

    val rows: List<Row> = listOf(
        swipe(3, Dir.UP, combo("Mission Control", C, Hid.KEY_UP), combo("Task View", G, Hid.KEY_TAB), combo("Home", G, Hid.KEY_H)),
        swipe(3, Dir.DOWN, combo("App Exposé", C, Hid.KEY_DOWN), combo("Show Desktop", G, Hid.KEY_D), combo("Search", G, Hid.KEY_SPACE)),
        swipe(3, Dir.LEFT, combo("Next Space", C, Hid.KEY_RIGHT), switcher(A), switcher(G)),
        swipe(3, Dir.RIGHT, combo("Previous Space", C, Hid.KEY_LEFT), switcher(A), switcher(G)),
        swipe(4, Dir.UP, combo("Mission Control", C, Hid.KEY_UP), combo("Task View", G, Hid.KEY_TAB), Globe("App Switcher", Hid.KEY_UP)),
        swipe(4, Dir.DOWN, combo("App Exposé", C, Hid.KEY_DOWN), combo("Show Desktop", G, Hid.KEY_D), Globe("Control Centre", Hid.KEY_C)),
        swipe(4, Dir.LEFT, switcher(G), combo("Next Desktop", G or C, Hid.KEY_RIGHT), Globe("Next App", Hid.KEY_RIGHT)),
        swipe(4, Dir.RIGHT, switcher(G), combo("Previous Desktop", G or C, Hid.KEY_LEFT), Globe("Previous App", Hid.KEY_LEFT)),
        tap(3, combo("Look Up", C or G, Hid.KEY_D), combo("Search", G, Hid.KEY_S), Globe("Dock", Hid.KEY_A)),
        tap(4, combo("Spotlight", G, Hid.KEY_SPACE), combo("Notifications", G, Hid.KEY_N), Globe("Notification Centre", Hid.KEY_N)),
        row(
            "Pinch in with four or five fingers", GestureEvent.MultiPinch(false),
            Media("Launchpad", Hid.CONSUMER_LAUNCHPAD), None, combo("Home", G, Hid.KEY_H)
        ),
        row(
            "Spread with four or five fingers", GestureEvent.MultiPinch(true),
            combo("Show Desktop", 0, Hid.KEY_F11), None, None
        ),
        info("Pinch out / in", "Zoom In / Out"),
        info("Drag with two fingers", "Scroll"),
        info("Tap with two fingers", "Right Click"),
        info("Tap, then drag", "Drag"),
        info("Three-finger drag", "Drag")
    )

    fun lookup(event: GestureEvent, os: HostOs): Cell? =
        rows.firstOrNull { it.trigger == event }?.cells?.get(os)

    fun guide(os: HostOs): List<GestureGuideRow> =
        rows.mapNotNull { r -> r.cells.getValue(os).takeIf { it !is None }?.let { GestureGuideRow(r.gesture, it.name) } }
}
