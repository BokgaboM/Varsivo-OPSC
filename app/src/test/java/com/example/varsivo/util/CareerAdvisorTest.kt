package com.example.varsivo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerAdvisorTest {

    @Test fun topScore_qualifiesForEveryCareer() {
        val result = CareerAdvisor.recommend(42)
        assertEquals(CareerAdvisor.options.size, result.qualifies.size)
        assertTrue(result.almost.isEmpty())
    }

    @Test fun qualifyingCareers_areListedHardestFirst() {
        val result = CareerAdvisor.recommend(30)
        assertEquals(listOf(30, 30, 28, 24, 24, 22, 15), result.qualifies.map { it.minAps })
    }

    @Test fun almost_listsCareersWithinThreePoints_closestFirst() {
        // APS 30: Law (32) is 2 short and Engineering (34) is 4 short, so only Law is "almost".
        val result = CareerAdvisor.recommend(30)
        assertEquals(listOf(3), result.almost.map { it.index })
    }

    @Test fun lowScore_qualifiesForNothing_butSeesTheClosestOption() {
        val result = CareerAdvisor.recommend(12)
        assertTrue(result.qualifies.isEmpty())
        assertEquals(listOf(10), result.almost.map { it.index }) // Higher Certificate, 3 points away
    }

    @Test fun pointsShort_isNeverNegative() {
        val law = CareerAdvisor.options[3]
        assertEquals(2, CareerAdvisor.pointsShort(30, law))
        assertEquals(0, CareerAdvisor.pointsShort(40, law))
    }

    @Test fun everyOptionHasAUniqueIndexInOrder() {
        assertEquals(CareerAdvisor.options.indices.toList(), CareerAdvisor.options.map { it.index })
    }
}
