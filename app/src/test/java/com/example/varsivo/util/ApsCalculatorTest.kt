package com.example.varsivo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApsCalculatorTest {

    @Test fun markToPoint_usesTheSevenPointScale() {
        assertEquals(7, ApsCalculator.markToPoint(100))
        assertEquals(7, ApsCalculator.markToPoint(80))
        assertEquals(6, ApsCalculator.markToPoint(79))
        assertEquals(6, ApsCalculator.markToPoint(70))
        assertEquals(5, ApsCalculator.markToPoint(60))
        assertEquals(4, ApsCalculator.markToPoint(50))
        assertEquals(3, ApsCalculator.markToPoint(40))
        assertEquals(2, ApsCalculator.markToPoint(30))
        assertEquals(1, ApsCalculator.markToPoint(29))
        assertEquals(1, ApsCalculator.markToPoint(0))
    }

    @Test fun calculate_sixSubjectsAddsThePoints() {
        // 80,70,60,50,40,30 -> 7+6+5+4+3+2 = 27
        assertEquals(27, ApsCalculator.calculate(listOf(80, 70, 60, 50, 40, 30)))
    }

    @Test fun calculate_needsExactlySixSubjects() {
        // The old version scaled 3 subjects of 80%+ up to the maximum 42: that was the bug.
        assertNull(ApsCalculator.calculate(listOf(90, 85, 80)))
        assertNull(ApsCalculator.calculate(listOf(80, 70, 60, 50, 40, 30, 90))) // 7 marks: LO must be left out
    }

    @Test fun calculate_matchesTheWorkedExampleFromTheSource() {
        // Points 5, 5, 4, 7, 6, 5 -> APS 32 (ZA Bursaries worked example)
        assertEquals(32, ApsCalculator.calculate(listOf(65, 60, 55, 85, 75, 62)))
    }

    @Test fun calculate_allDistinctionsGiveTheMaximum42() {
        assertEquals(42, ApsCalculator.calculate(List(6) { 80 }))
    }

    @Test fun calculate_lowestMarksGiveTheMinimum6() {
        assertEquals(6, ApsCalculator.calculate(List(6) { 0 }))
    }

    @Test fun calculate_returnsNullWhenNoMarksEntered() {
        assertNull(ApsCalculator.calculate(emptyList()))
    }

    @Test fun isEligible_comparesAgainstTheMinimum() {
        assertTrue(ApsCalculator.isEligible(30, 30))
        assertTrue(ApsCalculator.isEligible(35, 30))
        assertFalse(ApsCalculator.isEligible(29, 30))
    }

    @Test fun exploreData_isNotEmpty() {
        assertTrue(ExploreData.institutions.isNotEmpty())
        assertTrue(ExploreData.careers.isNotEmpty())
    }
}
