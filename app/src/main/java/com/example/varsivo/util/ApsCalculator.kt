package com.example.varsivo.util

/**
 * Admission Point Score (APS) maths for the Explore screen.
 *
 * Standard NSC method used by most South African universities:
 *  - six subjects are counted: Home Language, First Additional Language, Mathematics or
 *    Mathematical Literacy, and three electives. Life Orientation is NOT counted;
 *  - each percentage is converted to the 7-point scale (80–100% = 7 … 0–29% = 1);
 *  - the six points are added together, so the maximum APS is 42.
 * Some universities adjust this (for example extra points for Maths), which the app does not model.
 *
 * Reference: ZA Bursaries (2026) *Calculate your APS score for university entrance*.
 * https://www.zabursaries.co.za/aps-score-for-university-entrance/ (Accessed: 4 November 2026).
 */
object ApsCalculator {

    const val MAX_SUBJECTS = 6

    fun markToPoint(mark: Int): Int = when {
        mark >= 80 -> 7
        mark >= 70 -> 6
        mark >= 60 -> 5
        mark >= 50 -> 4
        mark >= 40 -> 3
        mark >= 30 -> 2
        else -> 1
    }

    /**
     * Adds the points of exactly six subject marks (Life Orientation excluded).
     * Returns null unless exactly [MAX_SUBJECTS] marks are given, because an APS from fewer
     * subjects would not be comparable with a university's minimum.
     * (The old version averaged fewer subjects and scaled them up, so three marks of 80%
     * already gave the maximum of 42 — that was the bug.)
     */
    fun calculate(marks: List<Int>): Int? {
        if (marks.size != MAX_SUBJECTS) return null
        return marks.sumOf { markToPoint(it) }
    }

    fun isEligible(apsScore: Int, minimumAps: Int): Boolean = apsScore >= minimumAps
}

/** An institution with the minimum APS it asks for. */
data class MatchedInstitution(val name: String, val minAps: Int)

/** Prototype data for the Explore screen (a real build would load this from the API). */
object ExploreData {
    val institutions = listOf(
        MatchedInstitution("University of Pretoria", 30),
        MatchedInstitution("Wits University", 32),
        MatchedInstitution("University of Johannesburg", 26),
        MatchedInstitution("Stellenbosch University", 28),
        MatchedInstitution("UNISA", 22)
    )

    val careers = listOf(
        "Software Developer",
        "Computer Science",
        "Economics",
        "Mechanical Engineering",
        "Nursing"
    )
}
