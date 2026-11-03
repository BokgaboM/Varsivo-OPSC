package com.example.varsivo.util

import com.example.varsivo.api.Bursary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BursaryFilterTest {

    private val bursaries = listOf(
        Bursary(id = "1", name = "NSFAS", provider = "Government", field = "All fields", province = "National"),
        Bursary(id = "2", name = "Sasol Bursary", provider = "Sasol", field = "Engineering", province = "National"),
        Bursary(id = "3", name = "Deloitte Bursary", provider = "Deloitte", field = "Commerce", province = "National"),
        Bursary(id = "4", name = "Gauteng City Region Academy", provider = "Gauteng", field = "All fields", province = "Gauteng"),
        Bursary(id = "5", name = "Western Cape Bursary", provider = "WC Gov", field = "Science", province = "Western Cape")
    )

    private fun names(list: List<Bursary>) = list.map { it.name }

    @Test fun defaultView_showsOnlyTheFirstPage() {
        val result = BursaryFilter.visibleBursaries(bursaries, showAll = false, query = "")
        assertEquals(BursaryFilter.FIRST_PAGE, result.size)
    }

    @Test fun viewMore_showsEverything() {
        assertEquals(bursaries.size, BursaryFilter.visibleBursaries(bursaries, showAll = true, query = "").size)
    }

    @Test fun search_isCaseInsensitiveAndTrimmed() {
        val result = BursaryFilter.visibleBursaries(bursaries, showAll = false, query = "  sasol ")
        assertEquals(listOf("Sasol Bursary"), names(result))
    }

    @Test fun search_alsoMatchesTheProvider() {
        val result = BursaryFilter.visibleBursaries(bursaries, showAll = false, query = "deloitte")
        assertEquals(listOf("Deloitte Bursary"), names(result))
    }

    @Test fun fieldFilter_keepsThatFieldPlusAllFieldsBursaries() {
        val result = BursaryFilter.visibleBursaries(bursaries, false, "", field = "Engineering")
        assertEquals(listOf("NSFAS", "Sasol Bursary", "Gauteng City Region Academy"), names(result))
    }

    @Test fun provinceFilter_keepsThatProvincePlusNationalBursaries() {
        val result = BursaryFilter.visibleBursaries(bursaries, false, "", province = "Western Cape")
        assertEquals(listOf("NSFAS", "Sasol Bursary", "Deloitte Bursary", "Western Cape Bursary"), names(result))
    }

    @Test fun fieldAndProvinceTogether() {
        val result = BursaryFilter.visibleBursaries(bursaries, false, "", field = "Science", province = "Gauteng")
        assertEquals(listOf("NSFAS", "Gauteng City Region Academy"), names(result))
    }

    @Test fun noMatch_returnsAnEmptyList() {
        assertTrue(BursaryFilter.visibleBursaries(bursaries, showAll = true, query = "zzzz").isEmpty())
    }
}
