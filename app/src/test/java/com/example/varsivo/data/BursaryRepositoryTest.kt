package com.example.varsivo.data

import com.example.varsivo.api.Bursary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class BursaryRepositoryTest {

    private val fromApi = listOf(Bursary(id = "1", name = "Zulu Bursary"), Bursary(id = "2", name = "Absa Bursary"))
    private val fromDatabase = listOf(Bursary(id = "nsfas", name = "NSFAS"))

    @Test fun usesTheLiveApiWhenItAnswers_sortedByName() = runTest {
        val repo = BursaryRepository(liveApi = { fromApi }, database = { fromDatabase })

        val result = repo.loadBursaries()

        assertEquals(BursarySource.LIVE_API, result.source)
        assertEquals(listOf("Absa Bursary", "Zulu Bursary"), result.bursaries.map { it.name })
    }

    @Test fun fallsBackToTheDatabaseWhenTheApiFails() = runTest {
        val repo = BursaryRepository(liveApi = { throw IOException("offline") }, database = { fromDatabase })

        val result = repo.loadBursaries()

        assertEquals(BursarySource.DATABASE, result.source)
        assertEquals(fromDatabase, result.bursaries)
    }

    @Test fun fallsBackToTheDatabaseWhenTheApiReturnsNothing() = runTest {
        val repo = BursaryRepository(liveApi = { emptyList() }, database = { fromDatabase })

        assertEquals(BursarySource.DATABASE, repo.loadBursaries().source)
    }

    @Test fun ignoresApiRowsWithoutAName() = runTest {
        val repo = BursaryRepository(
            liveApi = { listOf(Bursary(id = "9", name = ""), Bursary(id = "1", name = "Sasol")) },
            database = { fromDatabase }
        )

        assertEquals(listOf("Sasol"), repo.loadBursaries().bursaries.map { it.name })
    }
}
