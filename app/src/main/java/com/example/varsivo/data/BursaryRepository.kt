package com.example.varsivo.data

import android.util.Log
import com.example.varsivo.api.Bursary
import com.example.varsivo.api.RetrofitClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Where the bursary list came from, so the screen can say so. */
enum class BursarySource { LIVE_API, DATABASE }

data class BursaryResult(val bursaries: List<Bursary>, val source: BursarySource)

/**
 * Loads the bursary list.
 *
 * 1. **Live REST API first:** GET /bursaries on our hosted MockAPI project, through Retrofit
 *    ([RetrofitClient.bursaryApi]), so the list is always the latest version.
 * 2. **Cloud Firestore as the backup:** if the API is unreachable, fails or returns nothing, the
 *    "bursaries" collection (NoSQL) is used instead. Firestore also caches results, so this still
 *    works offline after the screen has been opened once.
 *
 * The two sources are passed in as functions so the fallback logic can be unit tested on the JVM.
 *
 * References: Square (2024) *Retrofit* https://square.github.io/retrofit/ ;
 * Firebase (2026) *Get data with Cloud Firestore* https://firebase.google.com/docs/firestore/query-data/get-data
 */
class BursaryRepository(
    private val liveApi: suspend () -> List<Bursary> = { RetrofitClient.bursaryApi.getBursaries() },
    private val database: suspend () -> List<Bursary> = { FirestoreBursaries.load() }
) {

    suspend fun loadBursaries(): BursaryResult {
        try {
            val live = liveApi().map { it.sanitised() }.filter { it.name.isNotBlank() }
            if (live.isNotEmpty()) {
                Log.i(TAG, "Loaded ${live.size} bursaries from the live API")
                return BursaryResult(live.sortedBy { it.name.lowercase() }, BursarySource.LIVE_API)
            }
            Log.w(TAG, "Live API returned no bursaries, using the database")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Live API unavailable (${e.message}), using the database")
        }
        return BursaryResult(database(), BursarySource.DATABASE)
    }

    private companion object {
        const val TAG = "BursaryRepository"
    }
}

/** The Firestore "bursaries" collection, seeded from the app the first time it is empty. */
object FirestoreBursaries {

    private const val TAG = "FirestoreBursaries"
    private const val COLLECTION = "bursaries"

    suspend fun load(): List<Bursary> {
        val db = FirebaseFirestore.getInstance()
        val collection = db.collection(COLLECTION)
        val snapshot = collection.get().await()
        if (!snapshot.isEmpty) {
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Bursary::class.java)?.copy(id = doc.id)?.sanitised()
            }
            Log.i(TAG, "Loaded ${list.size} bursaries from Firestore")
            return list.sortedBy { it.name.lowercase() }
        }

        // Empty collection: seed it once (a batch writes all documents together).
        Log.i(TAG, "Bursaries collection is empty, seeding ${BursarySeed.bursaries.size} documents")
        val batch = db.batch()
        BursarySeed.bursaries.forEach { b -> batch.set(collection.document(b.id), b) }
        batch.commit().await()
        return BursarySeed.bursaries.sortedBy { it.name.lowercase() }
    }
}
