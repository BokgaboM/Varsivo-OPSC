package com.example.varsivo.util

/**
 * Pure input-validation rules, kept free of Android classes so they can be unit tested
 * on the JVM (see ValidatorsTest). The UI layer decides which message to show.
 */
object Validators {

    private val STUDENT_ID_REGEX = Regex("^[A-Za-z0-9._-]{4,20}$")
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val MIN_PASSWORD_LENGTH = 8

    /** A name needs at least two characters including one letter. */
    fun isValidName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.length >= 2 && trimmed.any { it.isLetter() }
    }

    /** Student ID: 4–20 letters, digits, dot, underscore or dash (no spaces). */
    fun isValidStudentId(studentId: String): Boolean =
        STUDENT_ID_REGEX.matches(studentId.trim())

    /** Password: at least 8 characters, with at least one letter and one digit. */
    fun isStrongPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    /**
     * South African academic domain endings: ".ac.za" (public universities, e.g. student.uj.ac.za)
     * and ".edu.za" (TVET colleges and private institutions, e.g. the IIE's vcconnect.edu.za).
     */
    private val ACADEMIC_SUFFIXES = listOf(".ac.za", ".edu.za")

    /**
     * Private SA colleges whose student emails do not end in .ac.za or .edu.za.
     * The address must be this domain or a sub-domain of it (e.g. students.eduvos.com).
     * Add to this list when a new college needs to be accepted.
     */
    private val ALLOWED_COLLEGE_DOMAINS = listOf(
        "eduvos.com",
        "mancosa.co.za",
        "regenesys.net",
        "damelin.co.za"
    )

    /**
     * UniAssist: a valid email at a South African university or college.
     * Accepted: any "name@<something>.ac.za" or "name@<something>.edu.za" address, or an address
     * at one of [ALLOWED_COLLEGE_DOMAINS] (or its sub-domains).
     * Rejected: personal mail (gmail, outlook...), ordinary ".co.za" company addresses, a bare
     * "ac.za"/"edu.za" domain, and look-alikes such as "name@uj.ac.za.fake.com" (only the real
     * END of the domain counts). Spaces around the address are ignored; case does not matter.
     */
    fun isStudentEmail(email: String): Boolean {
        if (!isValidEmail(email)) return false
        val domain = email.trim().lowercase().substringAfterLast('@')

        // ".ac.za" / ".edu.za" with at least one name in front (so "ac.za" alone is refused).
        val academic = ACADEMIC_SUFFIXES.any { suffix ->
            domain.endsWith(suffix) && domain.length > suffix.length
        }
        // Exact college domain, or a sub-domain ending in "." + that domain
        // ("fakeeduvos.com" does not match "eduvos.com").
        val allowedCollege = ALLOWED_COLLEGE_DOMAINS.any { allowed ->
            domain == allowed || domain.endsWith(".$allowed")
        }
        return academic || allowedCollege
    }

    /** NSC subject marks are percentages between 0 and 100. */
    fun isValidMark(mark: Int): Boolean = mark in 0..100
}
