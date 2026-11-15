package app.curser.core.mapping

import app.curser.core.Feedback
import app.curser.core.HostOs
import app.curser.core.Settings
import app.curser.core.gesture.Dir
import app.curser.core.gesture.GestureEvent
import app.curser.core.hid.Hid
import kotlin.math.hypot

sealed interface HidAction {
    data class MouseMove(val dx: Int, val dy: Int) : HidAction
    data class Wheel(val v: Int, val h: Int, val queued: Boolean = false) : HidAction
    data class Buttons(val mask: Int) : HidAction
    data class Key(val mods: Int, val key: Int) : HidAction
    data class Consumer(val usage: Int) : HidAction
}

class GestureMapper {
    data class Mapped(val actions: List<HidAction>, val feedback: Feedback? = null)

    private companion object {
        const val SCROLL_UNIT_DP = 18f
        const val BTN_LEFT = 1
        const val BTN_RIGHT = 2
    }

    private var remX = 0f
    private var remY = 0f
    private var accV = 0f
    private var accH = 0f
    private var leftDown = false
    private var dragAnnounced = false
    private var switcherMod: Int? = null

    fun reset() {
        remX = 0f
        remY = 0f
        accV = 0f
        accH = 0f
        leftDown = false
        dragAnnounced = false
        switcherMod = null
    }

    fun map(event: GestureEvent, os: HostOs, s: Settings): Mapped = when (event) {
        is GestureEvent.Move -> move(event, s)
        is GestureEvent.Scroll -> scroll(event, os, s)
        is GestureEvent.Pinch -> pinch(event, os)
        is GestureEvent.Tap -> tap(event, os)
        is GestureEvent.Swipe -> swipe(event, os)
        is GestureEvent.SwipeRepeat -> switcherMod?.let { Mapped(tabStep(it, event.dir)) } ?: Mapped(emptyList())
        is GestureEvent.SwipeEnd -> switcherMod?.let {
            switcherMod = null
            Mapped(listOf(HidAction.Key(0, 0)))
        } ?: Mapped(emptyList())
        is GestureEvent.MultiPinch -> keyed(event, os)
        GestureEvent.LeftDown -> {
            leftDown = true
            dragAnnounced = false
            Mapped(listOf(HidAction.Buttons(BTN_LEFT)), Feedback("Click", Feedback.Kind.CLICK))
        }
        GestureEvent.LeftUp -> {
            leftDown = false
            Mapped(listOf(HidAction.Buttons(0)))
        }
    }

    private fun move(e: GestureEvent.Move, s: Settings): Mapped {
        val speed = hypot(e.dx, e.dy)
        val t = ((speed - 1.5f) / 20f).coerceIn(0f, 1f)
        val gain = 1f + 1.5f * t * t * (3f - 2f * t)
        val fx = e.dx * s.pointerSpeed * gain + remX
        val fy = e.dy * s.pointerSpeed * gain + remY
        val ix = fx.toInt()
        val iy = fy.toInt()
        remX = fx - ix
        remY = fy - iy
        var fb: Feedback? = null
        if (leftDown && !dragAnnounced) {
            dragAnnounced = true
            fb = Feedback("Drag", Feedback.Kind.DRAG)
        }
        val actions = if (ix == 0 && iy == 0) emptyList() else listOf(HidAction.MouseMove(ix, iy))
        return Mapped(actions, fb)
    }

    private fun scroll(e: GestureEvent.Scroll, os: HostOs, s: Settings): Mapped {
        val sign = (if (os == HostOs.WINDOWS) 1f else -1f) * (if (s.naturalScroll) 1f else -1f)
        accV += e.dy * sign
        accH += -e.dx * sign
        val unit = SCROLL_UNIT_DP / s.scrollSpeed
        val v = (accV / unit).toInt().coerceIn(-127, 127)
        val h = (accH / unit).toInt().coerceIn(-127, 127)
        accV -= v * unit
        accH -= h * unit
        return if (v == 0 && h == 0) Mapped(emptyList()) else Mapped(listOf(HidAction.Wheel(v, h)))
    }

    private fun pinch(e: GestureEvent.Pinch, os: HostOs): Mapped {
        val actions = if (os == HostOs.WINDOWS) {
            listOf(
                HidAction.Key(Hid.MOD_LCTRL, 0),
                HidAction.Wheel(if (e.zoomIn) 1 else -1, 0, queued = true),
                HidAction.Key(0, 0)
            )
        } else {
            combo(Hid.MOD_LGUI, if (e.zoomIn) Hid.KEY_EQUAL else Hid.KEY_MINUS)
        }
        return Mapped(actions, Feedback(if (e.zoomIn) "Zoom In" else "Zoom Out", Feedback.Kind.GESTURE))
    }

    private fun tap(e: GestureEvent.Tap, os: HostOs): Mapped =
        if (e.fingers == 2) {
            Mapped(
                listOf(HidAction.Buttons(BTN_RIGHT), HidAction.Buttons(0)),
                Feedback("Right Click", Feedback.Kind.CLICK)
            )
        } else keyed(e, os)

    private fun swipe(e: GestureEvent.Swipe, os: HostOs): Mapped {
        switcherMod = null
        val cell = GestureTable.lookup(e, os)
        if (cell is GestureTable.Switcher) {
            switcherMod = cell.mod
            val actions = listOf<HidAction>(HidAction.Key(cell.mod, 0)) + tabStep(cell.mod, e.dir)
            return Mapped(actions, Feedback(cell.name, Feedback.Kind.GESTURE))
        }
        return keyed(e, os)
    }

    private fun keyed(e: GestureEvent, os: HostOs): Mapped {
        val cell = GestureTable.lookup(e, os) ?: return Mapped(emptyList())
        val actions = when (cell) {
            is GestureTable.Combo -> combo(cell.mods, cell.key)
            is GestureTable.Media -> listOf(HidAction.Consumer(cell.usage), HidAction.Consumer(0))
            is GestureTable.Globe -> listOf(
                HidAction.Consumer(Hid.CONSUMER_GLOBE),
                HidAction.Key(0, cell.key),
                HidAction.Key(0, 0),
                HidAction.Consumer(0)
            )
            is GestureTable.Switcher -> return Mapped(emptyList())
            GestureTable.None -> return Mapped(emptyList())
        }
        return Mapped(actions, Feedback(cell.name, Feedback.Kind.GESTURE))
    }

    private fun combo(mods: Int, key: Int): List<HidAction> = listOf(HidAction.Key(mods, key), HidAction.Key(0, 0))

    private fun tabStep(mod: Int, dir: Dir): List<HidAction> = when (dir) {
        Dir.RIGHT -> listOf(HidAction.Key(mod, Hid.KEY_TAB), HidAction.Key(mod, 0))
        Dir.LEFT -> listOf(HidAction.Key(mod or Hid.MOD_LSHIFT, Hid.KEY_TAB), HidAction.Key(mod, 0))
        else -> emptyList()
    }
}
