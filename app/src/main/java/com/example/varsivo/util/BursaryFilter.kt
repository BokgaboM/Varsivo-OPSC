package com.example.varsivo.util

import com.example.varsivo.api.Bursary

/**
 * Decides which bursaries the Bursaries screen lists. Pure Kotlin so it can be unit tested.
 */
object BursaryFilter {

    const val ALL_FIELDS = "All fields"
    const val ALL_PROVINCES = "All provinces"
    const val NATIONAL = "National"

    /** How many bursaries the screen shows before "View more" is tapped. */
    const val FIRST_PAGE = 3

    val fields = listOf(ALL_FIELDS, "Engineering", "Commerce", "Science", "Education", "Health", "Law")
    val provinces = listOf(
        ALL_PROVINCES, "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo",
        "Mpumalanga", "North West", "Northern Cape", "Western Cape"
    )

    /**
     * 1. Keep bursaries whose name or provider contains [query] (case-insensitive).
     * 2. Keep bursaries for the chosen [field] (bursaries for "All fields" always match).
     * 3. Keep bursaries for the chosen [province] (national bursaries always match).
     * 4. Without "View more" and with no search or filter, only the first [FIRST_PAGE] are shown.
     */
    fun visibleBursaries(
        bursaries: List<Bursary>,
        showAll: Boolean,
        query: String,
        field: String = ALL_FIELDS,
        province: String = ALL_PROVINCES
    ): List<Bursary> {
        val q = query.trim()
        val matches = bursaries.filter { b ->
            (q.isEmpty() || b.name.contains(q, ignoreCase = true) || b.provider.contains(q, ignoreCase = true)) &&
                (field == ALL_FIELDS || b.field.equals(field, ignoreCase = true) ||
                    b.field.equals(ALL_FIELDS, ignoreCase = true)) &&
                (province == ALL_PROVINCES || b.province.equals(province, ignoreCase = true) ||
                    b.province.equals(NATIONAL, ignoreCase = true))
        }
        val filtering = q.isNotEmpty() || field != ALL_FIELDS || province != ALL_PROVINCES
        return if (showAll || filtering) matches else matches.take(FIRST_PAGE)
    }
}
