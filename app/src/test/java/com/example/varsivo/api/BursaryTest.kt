package com.example.varsivo.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks that the JSON the hosted API returns is turned into safe Bursary objects. */
class BursaryTest {

    @Test fun gsonParsing_readsTheApiFields() {
        val json = """{"id":"7","name":"Sasol Bursary","provider":"Sasol","closingDate":"31 May"}"""
        val bursary = Gson().fromJson(json, Bursary::class.java)
        assertEquals("Sasol Bursary", bursary.name)
        assertEquals("Sasol", bursary.provider)
        assertEquals("31 May", bursary.closingDate)
    }

    @Test fun sanitised_replacesExplicitJsonNullsWithEmptyStrings() {
        val json = """{"id":"7","name":"Test","description":null,"provider":null,"eligibility":null,"closingDate":null}"""
        val bursary = Gson().fromJson(json, Bursary::class.java).sanitised()
        assertEquals("Test", bursary.name)
        assertEquals("", bursary.description)
        assertEquals("", bursary.provider)
        assertEquals("", bursary.eligibility)
        assertEquals("", bursary.closingDate)
    }

    @Test fun gsonParsing_acceptsMockApisLowercaseClosingdate() {
        // Our MockAPI resource stores the field as "closingdate".
        val json = """{"id":"1","name":"NSFAS","closingdate":"Check official NSFAS website"}"""
        val bursary = Gson().fromJson(json, Bursary::class.java)
        assertEquals("Check official NSFAS website", bursary.closingDate)
    }
}
