package com.bacos.app.domain

import com.bacos.app.data.db.CardStateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SRSEngineTest {

    private val now = 1_700_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    private fun state(
        ease: Double = 2.5,
        interval: Int = 0,
        reps: Int = 0,
        lapses: Int = 0,
    ) = CardStateEntity(cardId = 1, ease = ease, intervalDays = interval, repetitions = reps, lapses = lapses)

    @Test
    fun `first good review schedules one day ahead`() {
        val r = SRSEngine.review(state(), SRSEngine.GRADE_GOOD, now)
        assertEquals(1, r.intervalDays)
        assertEquals(now + day, r.dueAt)
        assertEquals(1, r.repetitions)
    }

    @Test
    fun `second good review schedules six days`() {
        val r = SRSEngine.review(state(interval = 1, reps = 1), SRSEngine.GRADE_GOOD, now)
        assertEquals(6, r.intervalDays)
    }

    @Test
    fun `intervals grow multiplicatively after third success`() {
        val r2 = SRSEngine.review(state(interval = 1, reps = 1), SRSEngine.GRADE_GOOD, now)
        val r3 = SRSEngine.review(state(ease = r2.ease, interval = r2.intervalDays, reps = 2), SRSEngine.GRADE_GOOD, now)
        assertTrue("expected growth beyond 6 days, got ${r3.intervalDays}", r3.intervalDays > 6)
    }

    @Test
    fun `lapse resets repetitions and interval`() {
        val r = SRSEngine.review(state(interval = 20, reps = 4), SRSEngine.GRADE_AGAIN, now)
        assertEquals(0, r.repetitions)
        assertEquals(1, r.intervalDays)
        assertEquals(1, r.lapses)
    }

    @Test
    fun `lapse lowers ease but never below floor`() {
        var s = state(ease = 1.4)
        repeat(10) {
            val r = SRSEngine.review(s, SRSEngine.GRADE_AGAIN, now)
            assertTrue(r.ease >= 1.3)
            s = s.copy(ease = r.ease)
        }
    }

    @Test
    fun `hard review grows slower than good`() {
        val hard = SRSEngine.review(state(interval = 6, reps = 2), SRSEngine.GRADE_HARD, now)
        val good = SRSEngine.review(state(interval = 6, reps = 2), SRSEngine.GRADE_GOOD, now)
        assertTrue(hard.intervalDays <= good.intervalDays)
    }
}
