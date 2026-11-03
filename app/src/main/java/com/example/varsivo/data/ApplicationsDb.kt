package com.example.varsivo.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/** What kind of application a row is. */
enum class ApplicationType { UNIVERSITY, BURSARY }

/** Where an application is in the student's own tracking. */
enum class ApplicationStatus {
    PENDING, SUBMITTED, OFFER;

    /** PENDING -> SUBMITTED -> OFFER -> PENDING ... used by the tracker's status chip. */
    fun next(): ApplicationStatus = entries[(ordinal + 1) % entries.size]
}

data class TrackedApplication(
    val id: Long = 0,
    val type: ApplicationType,
    val institution: String,
    val programme: String,
    val status: ApplicationStatus,
    val synced: Boolean,
    val applicantName: String,
    val email: String,
    val createdAt: Long,
    /** Deleted on the phone but the delete has not reached Firestore yet. Hidden from the list. */
    val deleted: Boolean = false
) {
    /** Firestore document id. createdAt is unique per phone, so the id never changes. */
    val remoteId: String get() = "app-$createdAt"
}

/**
 * Local SQLite database for the student's applications (offline-first).
 * Rows are written here first; [ApplicationRepository] then syncs them to Cloud Firestore.
 * A row with synced = 0 has a change (new, status change or delete) that Firestore has not seen yet.
 *
 * Reference: Android Developers (2024) *Save data using SQLite* https://developer.android.com/training/data-storage/sqlite
 */
class ApplicationsDb(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        Log.i(TAG, "Creating $TABLE table")
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TYPE TEXT NOT NULL,
                $COL_INSTITUTION TEXT NOT NULL,
                $COL_PROGRAMME TEXT NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_SYNCED INTEGER NOT NULL DEFAULT 0,
                $COL_NAME TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL,
                $COL_CREATED INTEGER NOT NULL,
                $COL_DELETED INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Prototype: simply rebuild. A production app would migrate the data.
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insert(app: TrackedApplication): Long {
        val id = writableDatabase.insert(TABLE, null, app.toValues())
        Log.d(TAG, "Inserted application id=$id (${app.institution} / ${app.programme})")
        return id
    }

    fun getAll(): List<TrackedApplication> {
        val result = mutableListOf<TrackedApplication>()
        readableDatabase.query(TABLE, null, "$COL_DELETED = 0", null, null, null, "$COL_CREATED DESC").use { c ->
            while (c.moveToNext()) result.add(c.toApplication())
        }
        return result
    }

    fun getUnsynced(): List<TrackedApplication> {
        val result = mutableListOf<TrackedApplication>()
        readableDatabase.query(TABLE, null, "$COL_SYNCED = 0", null, null, null, "$COL_CREATED ASC").use { c ->
            while (c.moveToNext()) result.add(c.toApplication())
        }
        return result
    }

    fun updateStatus(id: Long, status: ApplicationStatus) {
        val values = ContentValues().apply {
            put(COL_STATUS, status.name)
            put(COL_SYNCED, 0) // the new status still has to reach Firestore
        }
        writableDatabase.update(TABLE, values, "$COL_ID = ?", arrayOf(id.toString()))
        Log.d(TAG, "Application $id status -> $status")
    }

    fun markSynced(id: Long) {
        val values = ContentValues().apply { put(COL_SYNCED, 1) }
        writableDatabase.update(TABLE, values, "$COL_ID = ?", arrayOf(id.toString()))
        Log.d(TAG, "Application $id marked as synced")
    }

    /** Hides the row and flags the delete for syncing; the row is removed once Firestore confirms. */
    fun markDeleted(id: Long) {
        val values = ContentValues().apply {
            put(COL_DELETED, 1)
            put(COL_SYNCED, 0)
        }
        writableDatabase.update(TABLE, values, "$COL_ID = ?", arrayOf(id.toString()))
        Log.d(TAG, "Application $id marked as deleted")
    }

    fun getById(id: Long): TrackedApplication? =
        readableDatabase.query(TABLE, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) c.toApplication() else null
        }

    fun delete(id: Long) {
        writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
        Log.d(TAG, "Deleted application $id")
    }

    fun clear() {
        writableDatabase.delete(TABLE, null, null)
    }

    private fun TrackedApplication.toValues() = ContentValues().apply {
        put(COL_TYPE, type.name)
        put(COL_INSTITUTION, institution)
        put(COL_PROGRAMME, programme)
        put(COL_STATUS, status.name)
        put(COL_SYNCED, if (synced) 1 else 0)
        put(COL_NAME, applicantName)
        put(COL_EMAIL, email)
        put(COL_CREATED, createdAt)
        put(COL_DELETED, if (deleted) 1 else 0)
    }

    private fun Cursor.toApplication() = TrackedApplication(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        type = ApplicationType.valueOf(getString(getColumnIndexOrThrow(COL_TYPE))),
        institution = getString(getColumnIndexOrThrow(COL_INSTITUTION)),
        programme = getString(getColumnIndexOrThrow(COL_PROGRAMME)),
        status = ApplicationStatus.valueOf(getString(getColumnIndexOrThrow(COL_STATUS))),
        synced = getInt(getColumnIndexOrThrow(COL_SYNCED)) == 1,
        applicantName = getString(getColumnIndexOrThrow(COL_NAME)),
        email = getString(getColumnIndexOrThrow(COL_EMAIL)),
        createdAt = getLong(getColumnIndexOrThrow(COL_CREATED)),
        deleted = getInt(getColumnIndexOrThrow(COL_DELETED)) == 1
    )

    companion object {
        private const val TAG = "ApplicationsDb"
        private const val DB_NAME = "varsivo.db"
        private const val DB_VERSION = 2 // 2 = added the "deleted" column

        private const val TABLE = "applications"
        private const val COL_ID = "id"
        private const val COL_TYPE = "type"
        private const val COL_INSTITUTION = "institution"
        private const val COL_PROGRAMME = "programme"
        private const val COL_STATUS = "status"
        private const val COL_SYNCED = "synced"
        private const val COL_NAME = "applicant_name"
        private const val COL_EMAIL = "email"
        private const val COL_CREATED = "created_at"
        private const val COL_DELETED = "deleted"
    }
}
