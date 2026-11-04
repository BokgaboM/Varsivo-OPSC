package com.example.varsivo.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Input validation is what stops bad data reaching the API, so it is tested thoroughly. */
class ValidatorsTest {

    // ----- name -----
    @Test fun name_acceptsNormalNames() {
        assertTrue(Validators.isValidName("Thabo Mokoena"))
        assertTrue(Validators.isValidName("  Li  "))
    }

    @Test fun name_rejectsBlankShortOrNumericOnly() {
        assertFalse(Validators.isValidName(""))
        assertFalse(Validators.isValidName("   "))
        assertFalse(Validators.isValidName("A"))
        assertFalse(Validators.isValidName("12"))
    }

    // ----- student id -----
    @Test fun studentId_acceptsLettersDigitsDotUnderscoreDash() {
        assertTrue(Validators.isValidStudentId("stu1234"))
        assertTrue(Validators.isValidStudentId("  Thabo_01  "))
        assertTrue(Validators.isValidStudentId("a.b-c1"))
    }

    @Test fun studentId_rejectsTooShortTooLongOrSpaces() {
        assertFalse(Validators.isValidStudentId("abc"))
        assertFalse(Validators.isValidStudentId("a".repeat(21)))
        assertFalse(Validators.isValidStudentId("stu 1234"))
        assertFalse(Validators.isValidStudentId("stu@1234"))
        assertFalse(Validators.isValidStudentId(""))
    }

    // ----- password -----
    @Test fun password_needsEightCharsLetterAndDigit() {
        assertTrue(Validators.isStrongPassword("Password1"))
        assertTrue(Validators.isStrongPassword("12345678a"))
    }

    @Test fun password_rejectsWeakOnes() {
        assertFalse(Validators.isStrongPassword("Pass1"))          // too short
        assertFalse(Validators.isStrongPassword("passwordonly"))   // no digit
        assertFalse(Validators.isStrongPassword("1234567890"))     // no letter
        assertFalse(Validators.isStrongPassword(""))
    }

    // ----- email -----
    @Test fun email_acceptsValidAddresses() {
        assertTrue(Validators.isValidEmail("student@gmail.com"))
        assertTrue(Validators.isValidEmail(" first.last+tag@school.co.za "))
    }

    @Test fun email_rejectsInvalidAddresses() {
        assertFalse(Validators.isValidEmail("student"))
        assertFalse(Validators.isValidEmail("student@"))
        assertFalse(Validators.isValidEmail("@gmail.com"))
        assertFalse(Validators.isValidEmail("student@gmail"))
        assertFalse(Validators.isValidEmail(""))
    }

    // ----- marks -----
    @Test fun mark_mustBeBetween0And100() {
        assertTrue(Validators.isValidMark(0))
        assertTrue(Validators.isValidMark(100))
        assertFalse(Validators.isValidMark(-1))
        assertFalse(Validators.isValidMark(101))
    }

    // ----- UniAssist student email -----
    @Test fun studentEmail_acceptsSouthAfricanUniversityAddresses() {
        assertTrue(Validators.isStudentEmail("thabo@student.uj.ac.za"))
        assertTrue(Validators.isStudentEmail("  Lerato.D@students.wits.ac.za "))
    }

    @Test fun studentEmail_rejectsOtherAddresses() {
        assertFalse(Validators.isStudentEmail("thabo@gmail.com"))
        assertFalse(Validators.isStudentEmail("thabo@uj.ac.za.fake.com"))
        assertFalse(Validators.isStudentEmail("ac.za"))
        assertFalse(Validators.isStudentEmail(""))
    }

    // ---------- .edu.za and allowed college domains ----------

    @Test fun studentEmail_acceptsEduZaColleges() {
        assertTrue(Validators.isStudentEmail("ST10000000@vcconnect.edu.za"))   // IIE / Rosebank
        assertTrue(Validators.isStudentEmail("ST10000000@rcconnect.edu.za"))
        assertTrue(Validators.isStudentEmail("learner@students.sbtvet.edu.za")) // TVET college
    }

    @Test fun studentEmail_isCaseInsensitiveAndTrimmed() {
        assertTrue(Validators.isStudentEmail("  ST10000000@VCConnect.EDU.ZA  "))
        assertTrue(Validators.isStudentEmail("Thabo@Student.UJ.AC.ZA"))
    }

    @Test fun studentEmail_acceptsAllowListedCollegesAndTheirSubdomains() {
        assertTrue(Validators.isStudentEmail("thabo@eduvos.com"))
        assertTrue(Validators.isStudentEmail("thabo@students.eduvos.com"))
        assertTrue(Validators.isStudentEmail("thabo@mancosa.co.za"))
    }

    @Test fun studentEmail_rejectsPersonalAndCompanyMail() {
        assertFalse(Validators.isStudentEmail("thabo@outlook.com"))
        assertFalse(Validators.isStudentEmail("thabo@yahoo.co.za"))
        assertFalse(Validators.isStudentEmail("thabo@company.co.za")) // random .co.za is not a college
    }

    @Test fun studentEmail_rejectsLookAlikeDomains() {
        assertFalse(Validators.isStudentEmail("thabo@vcconnect.edu.za.fake.com"))
        assertFalse(Validators.isStudentEmail("thabo@eduvos.com.evil.net"))
        assertFalse(Validators.isStudentEmail("thabo@fakeeduvos.com"))    // no dot before the allowed domain
        assertFalse(Validators.isStudentEmail("thabo@edu.za"))           // bare suffix, no institution
        assertFalse(Validators.isStudentEmail("thabo@ac.za"))
        assertFalse(Validators.isStudentEmail("not-an-email.edu.za"))    // no @ at all
    }
}
