package app.curser.core.gesture

import app.curser.core.TouchFrame
import kotlin.math.abs
import kotlin.math.hypot

enum class Dir { UP, DOWN, LEFT, RIGHT }

sealed interface GestureEvent {
    data class Move(val dx: Float, val dy: Float) : GestureEvent
    data class Scroll(val dx: Float, val dy: Float) : GestureEvent
    data class Pinch(val zoomIn: Boolean) : GestureEvent
    data class Tap(val fingers: Int) : GestureEvent
    data class Swipe(val fingers: Int, val dir: Dir) : GestureEvent
    data object LeftDown : GestureEvent
    data object LeftUp : GestureEvent
    data class SwipeRepeat(val fingers: Int, val dir: Dir) : GestureEvent
    data class SwipeEnd(val fingers: Int) : GestureEvent
    data class MultiPinch(val spread: Boolean) : GestureEvent
}

class GestureEngine(
    var tapToClick: Boolean = true,
    var tapAndDrag: Boolean = true,
    var threeFingerDrag: Boolean = false
) {

    companion object {
        const val TAP_MAX_MS = 250L
        const val TAP_SLOP_DP = 10f
        const val MOVE_SLOP_DP = 3f
        const val DRAG_WINDOW_MS = 180L
        const val SCROLL_SLOP_DP = 8f
        const val PINCH_SLOP_DP = 24f
        const val PINCH_STEP_DP = 40f
        const val SWIPE_DP = 56f
        const val SWIPE_STEP_DP = 44f
        const val MULTI_PINCH_DP = 18f
    }

    private enum class Mode { NONE, PINCH, SCROLL }

    var deadlineMs: Long? = null
        private set

    private var active = false
    private var ignored = false
    private var held = false          // engine believes the left button is down
    private var dragSeq = false       // sequence began while the button was held by a tap
    private var startMs = 0L
    private var maxFingers = 0
    private var prevN = 0
    private var prevX = 0f
    private var prevY = 0f
    private var baseX = 0f
    private var baseY = 0f
    private var baseD = 0f
    private var maxTravel = 0f
    private var moved = false
    private var swiped = false
    private var swipeFingers = 0
    private var swipeHoriz = false
    private var anchor = 0f           // centroid position along the swipe axis
    private var multiPinched = false
    private var baseSpread = 0f
    private var tfdDown = false       // three-finger drag holds the button
    private var tfdUsed = false
    private var mode = Mode.NONE
    private var pinchRef = 0f
    private val origins = HashMap<Int, Pair<Float, Float>>()

    fun onFrame(frame: TouchFrame): List<GestureEvent> {
        val out = ArrayList<GestureEvent>(2)
        val pts = frame.points
        val n = pts.size
        if (!active) {
            if (n == 0) return out
            val dl = deadlineMs
            if (dl != null && frame.timeMs >= dl) out += onTimeout(frame.timeMs)
            begin(frame.timeMs)
        }
        if (n == 0) {
            if (!ignored) finish(frame.timeMs, out)
            active = false
            return out
        }
        if (ignored) return out
        if (n >= 6) {
            ignored = true
            release(out)
            endMulti(out)
            return out
        }
        maxFingers = maxOf(maxFingers, n)

        var cx = 0f
        var cy = 0f
        for (p in pts) {
            cx += p.x
            cy += p.y
            val o = origins.getOrPut(p.id) { p.x to p.y }
            maxTravel = maxOf(maxTravel, hypot(p.x - o.first, p.y - o.second))
        }
        cx /= n
        cy /= n
        val d = if (n == 2) hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y) else 0f
        var spread = 0f
        if (n >= 3) {
            for (p in pts) spread += hypot(p.x - cx, p.y - cy)
            spread /= n
        }

        if (n != prevN) {
            if (maxFingers >= 2 && dragSeq) release(out)
            if (tfdDown) {
                tfdDown = false
                out += GestureEvent.LeftUp
            }
            baseX = cx
            baseY = cy
            baseD = d
            baseSpread = spread
            if (swiped) anchor = if (swipeHoriz) cx else cy
        } else if (n == 1 && maxFingers == 1) {
            if (!moved) {
                if (hypot(cx - baseX, cy - baseY) >= MOVE_SLOP_DP) moved = true
            } else {
                out += GestureEvent.Move(cx - prevX, cy - prevY)
            }
        } else if (n == 2 && maxFingers == 2) {
            twoFingers(cx, cy, d, out)
        } else if (n >= 3) {
            multiFingers(n, cx, cy, spread, out)
        }
        prevN = n
        prevX = cx
        prevY = cy
        return out
    }

    fun onTimeout(nowMs: Long): List<GestureEvent> {
        val dl = deadlineMs ?: return emptyList()
        if (nowMs < dl) return emptyList()
        deadlineMs = null
        if (!held) return emptyList()
        held = false
        return listOf(GestureEvent.LeftUp)
    }

    fun reset(): List<GestureEvent> {
        val wasHeld = held || tfdDown
        tfdDown = false
        active = false
        ignored = false
        held = false
        dragSeq = false
        deadlineMs = null
        origins.clear()
        return if (wasHeld) listOf(GestureEvent.LeftUp) else emptyList()
    }

    private fun begin(t: Long) {
        active = true
        ignored = false
        dragSeq = held
        deadlineMs = null
        startMs = t
        maxFingers = 0
        prevN = 0
        maxTravel = 0f
        moved = false
        swiped = false
        multiPinched = false
        tfdDown = false
        tfdUsed = false
        mode = Mode.NONE
        origins.clear()
    }

    private fun release(out: MutableList<GestureEvent>) {
        if (held) {
            held = false
            out += GestureEvent.LeftUp
        }
    }

    private fun multiFingers(n: Int, cx: Float, cy: Float, spread: Float, out: MutableList<GestureEvent>) {
        if (swiped) {
            val pos = if (swipeHoriz) cx else cy
            while (abs(pos - anchor) >= SWIPE_STEP_DP) {
                val fwd = pos > anchor
                anchor += if (fwd) SWIPE_STEP_DP else -SWIPE_STEP_DP
                val dir = if (swipeHoriz) (if (fwd) Dir.RIGHT else Dir.LEFT) else (if (fwd) Dir.DOWN else Dir.UP)
                out += GestureEvent.SwipeRepeat(swipeFingers, dir)
            }
            return
        }
        if (multiPinched) return
        if (n >= 4) {
            val ds = spread - baseSpread
            if (abs(ds) >= MULTI_PINCH_DP && abs(ds) > hypot(cx - baseX, cy - baseY)) {
                multiPinched = true
                out += GestureEvent.MultiPinch(ds > 0f)
                return
            }
        }
        val dx = cx - baseX
        val dy = cy - baseY
        if (threeFingerDrag && maxFingers == 3) {
            if (!tfdDown) {
                if (hypot(dx, dy) >= MOVE_SLOP_DP) {
                    tfdDown = true
                    tfdUsed = true
                    out += GestureEvent.LeftDown
                }
            } else {
                out += GestureEvent.Move(cx - prevX, cy - prevY)
            }
            return
        }
        if (hypot(dx, dy) >= SWIPE_DP) {
            val dir = direction(dx, dy)
            swiped = true
            swipeFingers = minOf(maxFingers, 4)
            swipeHoriz = dir == Dir.LEFT || dir == Dir.RIGHT
            anchor = if (swipeHoriz) cx else cy
            out += GestureEvent.Swipe(swipeFingers, dir)
        }
    }

    // Closes anything a multi-finger sequence holds open: the drag button, the swipe.
    private fun endMulti(out: MutableList<GestureEvent>) {
        if (tfdDown) {
            tfdDown = false
            out += GestureEvent.LeftUp
        }
        if (swiped) {
            swiped = false
            out += GestureEvent.SwipeEnd(swipeFingers)
        }
    }

    private fun twoFingers(cx: Float, cy: Float, d: Float, out: MutableList<GestureEvent>) {
        when (mode) {
            Mode.NONE -> {
                val dd = d - baseD
                val travel = hypot(cx - baseX, cy - baseY)
                if (abs(dd) >= PINCH_SLOP_DP && abs(dd) > travel) {
                    mode = Mode.PINCH
                    pinchRef = baseD + (if (dd > 0f) PINCH_SLOP_DP else -PINCH_SLOP_DP)
                    out += GestureEvent.Pinch(dd > 0f)
                    pinchSteps(d, out)
                } else if (travel >= SCROLL_SLOP_DP) {
                    mode = Mode.SCROLL
                    out += GestureEvent.Scroll(cx - prevX, cy - prevY)
                }
            }
            Mode.PINCH -> pinchSteps(d, out)
            Mode.SCROLL -> out += GestureEvent.Scroll(cx - prevX, cy - prevY)
        }
    }

    private fun pinchSteps(d: Float, out: MutableList<GestureEvent>) {
        while (d - pinchRef >= PINCH_STEP_DP) {
            pinchRef += PINCH_STEP_DP
            out += GestureEvent.Pinch(true)
        }
        while (pinchRef - d >= PINCH_STEP_DP) {
            pinchRef -= PINCH_STEP_DP
            out += GestureEvent.Pinch(false)
        }
    }

    private fun direction(dx: Float, dy: Float): Dir =
        if (abs(dx) > abs(dy)) (if (dx > 0f) Dir.RIGHT else Dir.LEFT)
        else (if (dy > 0f) Dir.DOWN else Dir.UP)

    private fun finish(t: Long, out: MutableList<GestureEvent>) {
        val dur = t - startMs
        val tapLike = dur <= TAP_MAX_MS && maxTravel <= TAP_SLOP_DP
        val wasSwiped = swiped
        endMulti(out)
        if (held) {
            if (maxFingers == 1 && tapLike) {
                out += GestureEvent.LeftUp
                out += GestureEvent.LeftDown
            }
            out += GestureEvent.LeftUp
            held = false
        } else if (maxFingers == 1 && tapLike && tapToClick) {
            out += GestureEvent.LeftDown
            if (tapAndDrag) {
                held = true
                deadlineMs = t + DRAG_WINDOW_MS
            } else {
                out += GestureEvent.LeftUp
            }
        } else if (maxFingers in 2..4 && dur <= TAP_MAX_MS + 50 && maxTravel <= TAP_SLOP_DP &&
            mode == Mode.NONE && !wasSwiped && !tfdUsed && tapToClick
        ) {
            out += GestureEvent.Tap(maxFingers)
        }
    }
}
