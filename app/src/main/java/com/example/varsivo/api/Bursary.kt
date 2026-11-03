package com.example.varsivo.api

/**
 * A bursary, as stored in the Cloud Firestore "bursaries" collection.
 * Firestore (and Gson) bypass Kotlin's null-safety, so a missing field can end up null even though
 * it is declared non-null; [sanitised] turns those into empty strings before the UI touches them.
 */
data class Bursary(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val provider: String = "",
    val eligibility: String = "",
    val closingDate: String = "",
    /** Study field, e.g. "Engineering", or "All fields". */
    val field: String = "",
    /** Province, or "National" when it is open to the whole country. */
    val province: String = ""
) {
    fun sanitised(): Bursary = Bursary(
        id = orEmpty(id),
        name = orEmpty(name),
        description = orEmpty(description),
        provider = orEmpty(provider),
        eligibility = orEmpty(eligibility),
        closingDate = orEmpty(closingDate),
        field = orEmpty(field),
        province = orEmpty(province)
    )

    private companion object {
        // Takes String? so the null check is real (a plain `x ?: ""` on a String can be optimised away).
        fun orEmpty(value: String?): String = value ?: ""
    }
}
