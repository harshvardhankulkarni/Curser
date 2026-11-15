package app.curser.core

import app.curser.core.gesture.Dir
import app.curser.core.gesture.GestureEngine
import app.curser.core.gesture.GestureEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureEngineTest {

    private fun p(id: Int, x: Float, y: Float) = TouchPoint(id, x, y)

    private fun GestureEngine.feed(t: Long, vararg pts: TouchPoint): List<GestureEvent> =
        onFrame(TouchFrame(t, pts.toList()))

    private fun GestureEngine.feedAll(vararg frames: Pair<Long, List<TouchPoint>>): List<GestureEvent> =
        frames.flatMap { onFrame(TouchFrame(it.first, it.second)) }

    @Test
    fun oneFingerMoveSkipsSlop() {
        val e = GestureEngine()
        val events = e.feedAll(
            0L to listOf(p(0, 100f, 100f)),
            8L to listOf(p(0, 102f, 100f)),
            16L to listOf(p(0, 104f, 100f)),
            24L to listOf(p(0, 110f, 100f)),
            32L to listOf(p(0, 116f, 100f)),
            40L to emptyList()
        )
        assertEquals(listOf(GestureEvent.Move(6f, 0f), GestureEvent.Move(6f, 0f)), events)
    }

    @Test
    fun quickTapWithoutDragIsClick() {
        val e = GestureEngine(tapAndDrag = false)
        val events = e.feedAll(0L to listOf(p(0, 50f, 50f)), 100L to emptyList())
        assertEquals(listOf(GestureEvent.LeftDown, GestureEvent.LeftUp), events)
        assertNull(e.deadlineMs)
    }

    @Test
    fun tapWithDragWaitsForTimeout() {
        val e = GestureEngine(tapAndDrag = true)
        val events = e.feedAll(0L to listOf(p(0, 50f, 50f)), 100L to emptyList())
        assertEquals(listOf<GestureEvent>(GestureEvent.LeftDown), events)
        assertEquals(280L, e.deadlineMs)
        assertEquals(listOf<GestureEvent>(GestureEvent.LeftUp), e.onTimeout(280L))
        assertNull(e.deadlineMs)
    }

    @Test
    fun tapThenDragHoldsButtonUntilLift() {
        val e = GestureEngine()
        e.feedAll(0L to listOf(p(0, 50f, 50f)), 100L to emptyList())
        val land = e.feed(150L, p(0, 50f, 50f))
        assertTrue(land.isEmpty())
        assertNull(e.deadlineMs)
        val events = e.feedAll(
            160L to listOf(p(0, 60f, 50f)),
            170L to listOf(p(0, 70f, 50f)),
            200L to emptyList()
        )
        assertEquals(listOf(GestureEvent.Move(10f, 0f), GestureEvent.LeftUp), events)
    }

    @Test
    fun doubleTapGivesTwoFullClicks() {
        val e = GestureEngine()
        val events = e.feedAll(
            0L to listOf(p(0, 50f, 50f)),
            100L to emptyList(),
            150L to listOf(p(0, 50f, 50f)),
            230L to emptyList()
        )
        assertEquals(
            listOf(GestureEvent.LeftDown, GestureEvent.LeftUp, GestureEvent.LeftDown, GestureEvent.LeftUp),
            events
        )
        assertNull(e.deadlineMs)
    }

    @Test
    fun twoFingerDragScrolls() {
        val e = GestureEngine()
        val events = e.feedAll(
            0L to listOf(p(0, 100f, 100f), p(1, 200f, 100f)),
            10L to listOf(p(0, 100f, 110f), p(1, 200f, 110f)),
            20L to listOf(p(0, 100f, 120f), p(1, 200f, 120f)),
            30L to emptyList()
        )
        assertEquals(listOf(GestureEvent.Scroll(0f, 10f), GestureEvent.Scroll(0f, 10f)), events)
    }

    @Test
    fun twoFingerTap() {
        val e = GestureEngine()
        val events = e.feedAll(
            0L to listOf(p(0, 100f, 100f), p(1, 200f, 100f)),
            100L to emptyList()
        )
        assertEquals(listOf<GestureEvent>(GestureEvent.Tap(2)), events)
    }

    @Test
    fun pinchOutZoomsIn() {
        val e = GestureEngine()
        val events = e.feedAll(
            0L to listOf(p(0, 100f, 100f), p(1, 200f, 100f)),
            10L to listOf(p(0, 60f, 100f), p(1, 240f, 100f)),
            20L to emptyList()
        )
        assertTrue(events.isNotEmpty())
        assertTrue(events.all { it == GestureEvent.Pinch(zoomIn = true) })
    }

    @Test
    fun threeFingerSwipeUpFiresOnce() {
        val e = GestureEngine()
        fun three(y: Float) = listOf(p(0, 100f, y), p(1, 150f, y), p(2, 200f, y))
        val events = e.feedAll(
            0L to three(300f),
            10L to three(280f),
            20L to three(250f),
            30L to three(240f),
            40L to three(200f),
            50L to emptyList()
        )
        assertEquals(listOf(GestureEvent.Swipe(3, Dir.UP), GestureEvent.SwipeEnd(3)), events)
    }

    @Test
    fun remainingFingerAfterScrollDoesNotMove() {
        val e = GestureEngine()
        val events = e.feedAll(
            0L to listOf(p(0, 100f, 100f), p(1, 200f, 100f)),
            10L to listOf(p(0, 100f, 120f), p(1, 200f, 120f)),
            20L to listOf(p(0, 100f, 130f)),
            30L to listOf(p(0, 100f, 160f)),
            40L to listOf(p(0, 100f, 200f)),
            50L to emptyList()
        )
        assertTrue(events.none { it is GestureEvent.Move })
        assertTrue(events.any { it is GestureEvent.Scroll })
    }

    private fun three(x: Float, y: Float = 300f) = listOf(p(0, x, y), p(1, x + 50f, y), p(2, x + 100f, y))

    @Test
    fun threeFingerSwipeRepeatsAndEnds() {
        val e = GestureEngine()
        val events = e.feedAll(
            0L to three(0f),
            10L to three(60f),
            20L to three(110f),
            30L to three(160f),
            40L to emptyList()
        )
        assertEquals(
            listOf(
                GestureEvent.Swipe(3, Dir.RIGHT),
                GestureEvent.SwipeRepeat(3, Dir.RIGHT),
                GestureEvent.SwipeRepeat(3, Dir.RIGHT),
                GestureEvent.SwipeEnd(3)
            ),
            events
        )
    }

    @Test
    fun swipeReversalRepeatsOtherWay() {
        val e = GestureEngine()
        val events = e.feedAll(0L to three(0f), 10L to three(60f), 20L to three(10f))
        assertEquals(listOf(GestureEvent.Swipe(3, Dir.RIGHT), GestureEvent.SwipeRepeat(3, Dir.LEFT)), events)
    }

    private fun five(y: Float) = (0..4).map { p(it, 50f * it, y) }

    @Test
    fun fiveFingerSwipeUpIsFourFingerSwipe() {
        val e = GestureEngine()
        val events = e.feedAll(0L to five(300f), 10L to five(240f), 20L to emptyList())
        assertEquals(listOf(GestureEvent.Swipe(4, Dir.UP), GestureEvent.SwipeEnd(4)), events)
    }

    @Test
    fun sixFingersAreIgnored() {
        val e = GestureEngine()
        val six = (0..5).map { p(it, 50f * it, 300f) }
        assertTrue(e.feedAll(0L to six, 10L to six.map { p(it.id, it.x, 200f) }, 20L to emptyList()).isEmpty())
    }

    @Test
    fun fourFingersTowardCentroidIsOneMultiPinch() {
        val e = GestureEngine()
        fun four(r: Float) = listOf(p(0, 200f - r, 200f), p(1, 200f + r, 200f), p(2, 200f, 200f - r), p(3, 200f, 200f + r))
        val events = e.feedAll(0L to four(80f), 10L to four(60f), 20L to four(40f), 30L to emptyList())
        assertEquals(listOf<GestureEvent>(GestureEvent.MultiPinch(spread = false)), events)
    }

    @Test
    fun threeFingerDragHoldsButton() {
        val e = GestureEngine(threeFingerDrag = true)
        val events = e.feedAll(
            0L to three(0f),
            10L to three(5f),
            20L to three(15f),
            30L to three(40f),
            40L to three(100f),
            50L to emptyList()
        )
        assertEquals(
            listOf(
                GestureEvent.LeftDown,
                GestureEvent.Move(10f, 0f),
                GestureEvent.Move(25f, 0f),
                GestureEvent.Move(60f, 0f),
                GestureEvent.LeftUp
            ),
            events
        )
    }

    @Test
    fun threeFingerDragReleasesWhenFingerLifts() {
        val e = GestureEngine(threeFingerDrag = true)
        val events = e.feedAll(0L to three(0f), 10L to three(10f), 20L to three(20f), 30L to three(30f).take(2))
        assertEquals(listOf(GestureEvent.LeftDown, GestureEvent.Move(10f, 0f), GestureEvent.LeftUp), events)
    }
}
