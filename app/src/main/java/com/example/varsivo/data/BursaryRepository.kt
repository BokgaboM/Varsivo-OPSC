package com.example.varsivo.data

import android.util.Log
import com.example.varsivo.api.Bursary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Reads bursaries from the Cloud Firestore "bursaries" collection (NoSQL).
 * The first time the collection is empty, the starter list ([BursarySeed]) is written to it, so
 * the database is filled from the app itself. Firestore also caches results, so the list still
 * loads offline after it has been opened once.
 *
 * Reference: Firebase (2026) *Get data with Cloud Firestore*
 * https://firebase.google.com/docs/firestore/query-data/get-data
 */
class BursaryRepository {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun loadBursaries(): List<Bursary> {
        val collection = db.collection(COLLECTION)
        val snapshot = collection.get().await()
        if (!snapshot.isEmpty) {
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Bursary::class.java)?.copy(id = doc.id)?.sanitised()
            }
            Log.i(TAG, "Loaded ${list.size} bursaries from Firestore")
            return list.sortedBy { it.name }
        }

        // Empty collection: seed it once (a batch writes all documents together).
        Log.i(TAG, "Bursaries collection is empty, seeding ${BursarySeed.bursaries.size} documents")
        val batch = db.batch()
        BursarySeed.bursaries.forEach { b -> batch.set(collection.document(b.id), b) }
        batch.commit().await()
        return BursarySeed.bursaries.sortedBy { it.name }
    }

    private companion object {
        const val TAG = "BursaryRepository"
        const val COLLECTION = "bursaries"
    }
}
