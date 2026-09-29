package com.bacos.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessEngineTest {

    @Test
    fun `empty subjects produce zero`() {
        assertEquals(0, ReadinessEngine.overall(emptyList(), streak = 10))
    }

    @Test
    fun `full mastery full coverage full retention equals near hundred`() {
        val input = ReadinessEngine.SubjectInput(masteryAvg = 100.0, coverage = 1.0, retention = 1.0)
        val score = ReadinessEngine.subjectScore(input)
        assertEquals(100.0, score, 0.01)
    }

    @Test
    fun `zero everything equals near zero`() {
        val input = ReadinessEngine.SubjectInput(masteryAvg = 0.0, coverage = 0.0, retention = 0.0)
        assertEquals(0.0, ReadinessEngine.subjectScore(input), 0.01)
    }

    @Test
    fun `streak contributes at most ten points`() {
        val inputs = listOf(90.0, 90.0)
        val noStreak = ReadinessEngine.overall(inputs, streak = 0)
        val midStreak = ReadinessEngine.overall(inputs, streak = 7)
        val fullStreak = ReadinessEngine.overall(inputs, streak = 30)
        assertEquals(81, noStreak)
        assertEquals(86, midStreak)
        assertEquals(91, fullStreak)
        assertTrue(fullStreak - noStreak <= 10)
    }

    @Test
    fun `score is bounded to 0-100`() {
        val huge = ReadinessEngine.overall(listOf(120.0, 130.0), streak = 100)
        assertTrue(huge in 0..100)
    }

    @Test
    fun `labels are motivational`() {
        assertEquals("بداية الرحلة", ReadinessEngine.label(10))
        assertEquals("يحتاج عمل جاد", ReadinessEngine.label(50))
        assertEquals("على الطريق الصحيح", ReadinessEngine.label(70))
        assertEquals("جاهز", ReadinessEngine.label(90))
    }
}
