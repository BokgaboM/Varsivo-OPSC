package com.example.varsivo.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Offline-first application tracker.
 *
 * Every change (new application, status change, delete) is saved to the local SQLite table first
 * and flagged as not synced. It is then sent to Cloud Firestore. Without internet the change stays
 * flagged ("Offline - will sync") and [syncPending] sends it the next time the tracker opens.
 */
class ApplicationRepository(
    private val db: ApplicationsDb,
    private val remote: ApplicationRemote,
    private val userStore: AccountStore,
) {

    suspend fun getAll(): List<TrackedApplication> = withContext(Dispatchers.IO) { db.getAll() }

    /** Saves a new application locally, then tries to sync it. Returns true when it synced. */
    suspend fun submit(
        type: ApplicationType,
        institution: String,
        programme: String,
        applicantName: String,
        email: String
    ): Boolean = withContext(Dispatchers.IO) {
        val app = TrackedApplication(
            type = type,
            institution = institution,
            programme = programme,
            status = ApplicationStatus.SUBMITTED,
            synced = false,
            applicantName = applicantName,
            email = email,
            createdAt = System.currentTimeMillis()
        )
        val id = db.insert(app)
        push(app.copy(id = id))
    }

    /** Sends every change that has not reached Firestore yet. */
    suspend fun syncPending() = withContext(Dispatchers.IO) {
        val pending = db.getUnsynced()
        if (pending.isEmpty()) return@withContext
        Log.i(TAG, "Syncing ${pending.size} pending change(s)")
        pending.forEach { push(it) }
    }

    suspend fun cycleStatus(app: TrackedApplication) = withContext(Dispatchers.IO) {
        db.updateStatus(app.id, app.status.next())
        db.getById(app.id)?.let { push(it) }
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        db.markDeleted(id)
        db.getById(id)?.let { push(it) }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) { db.clear() }

    /** Uploads one change (or delete). Never throws; returns true when Firestore confirmed it. */
    private suspend fun push(app: TrackedApplication): Boolean {
        val uid = userStore.remoteId
        if (uid == null) {
            Log.w(TAG, "Not signed in online, application ${app.id} stays local for now")
            return false
        }
        val ok = try {
            withTimeoutOrNull(SYNC_TIMEOUT_MS) {
                if (app.deleted) remote.delete(uid, app.remoteId) else remote.upsert(uid, app)
                true
            } ?: false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync application ${app.id}: ${e.message}")
            false
        }
        if (ok) {
            if (app.deleted) db.delete(app.id) else db.markSynced(app.id)
            Log.i(TAG, "Application ${app.id} synced to Firestore (deleted=${app.deleted})")
        } else {
            Log.i(TAG, "Application ${app.id} saved offline, will sync later")
        }
        return ok
    }

    private companion object {
        const val TAG = "ApplicationRepository"
        const val SYNC_TIMEOUT_MS = 6_000L
    }
}
