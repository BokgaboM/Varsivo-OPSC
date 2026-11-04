package com.example.varsivo.util

/**
 * Recommends careers from a learner's APS (Explore screen).
 *
 * Each option has an APPROXIMATE minimum APS that South African universities commonly ask for.
 * Every university and faculty sets its own minimums (and subject rules such as Mathematics), so
 * the screen labels these as a guide and the learner must still check the institution.
 *
 * [CareerOption.index] matches the order of the `career_names` string-array, so the names are
 * translated (English, isiZulu, Afrikaans) while this class stays plain Kotlin for unit tests.
 */
data class CareerOption(val index: Int, val minAps: Int)

/** What to show for one APS score. */
data class CareerRecommendations(
    /** Careers whose usual minimum APS is met, hardest first. */
    val qualifies: List<CareerOption>,
    /** Careers just out of reach (within [CareerAdvisor.ALMOST_RANGE] points), closest first. */
    val almost: List<CareerOption>
)

object CareerAdvisor {

    /** How many APS points short still counts as "almost". */
    const val ALMOST_RANGE = 3

    /** Same order as the `career_names` string-array. */
    val options = listOf(
        CareerOption(0, 40),  // Medicine
        CareerOption(1, 36),  // Actuarial Science
        CareerOption(2, 34),  // Engineering
        CareerOption(3, 32),  // Law
        CareerOption(4, 30),  // Accounting
        CareerOption(5, 30),  // Computer Science
        CareerOption(6, 28),  // Nursing
        CareerOption(7, 24),  // Information Technology (diploma)
        CareerOption(8, 24),  // Teaching (BEd)
        CareerOption(9, 22),  // Business Management
        CareerOption(10, 15)  // Higher Certificate
    )

    fun recommend(aps: Int): CareerRecommendations {
        val qualifies = options.filter { aps >= it.minAps }.sortedByDescending { it.minAps }
        val almost = options
            .filter { it.minAps > aps && it.minAps - aps <= ALMOST_RANGE }
            .sortedBy { it.minAps }
        return CareerRecommendations(qualifies, almost)
    }

    /** Points still needed for [option]; 0 when already met. */
    fun pointsShort(aps: Int, option: CareerOption): Int = maxOf(0, option.minAps - aps)
}
