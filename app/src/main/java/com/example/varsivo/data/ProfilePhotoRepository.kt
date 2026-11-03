package com.example.varsivo.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Profile photos:
 *  1. the picked image is shrunk to at most 512 px and compressed to JPEG,
 *  2. uploaded to Azure Blob Storage as "<uid>.jpg",
 *  3. the blob name is saved on the student's Firestore profile ("photoBlob"),
 *  4. a copy is kept on the phone so it shows instantly and offline.
 */
class ProfilePhotoRepository(
    private val context: Context,
    private val blobs: AzureBlobStorage,
    private val userStore: UserStore
) {

    private val localFile: File get() = File(context.filesDir, "profile_photo.jpg")

    val isConfigured: Boolean get() = blobs.isConfigured

    /** Returns true when the photo reached Azure. */
    suspend fun upload(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val uid = userStore.remoteId ?: return@withContext false
        val bytes = compress(uri) ?: return@withContext false
        localFile.writeBytes(bytes)
        val blobName = "$uid.jpg"
        blobs.upload(blobName, bytes, "image/jpeg")
        withTimeoutOrNull(6_000L) {
            FirebaseFirestore.getInstance().collection("users").document(uid).set(
                mapOf("photoBlob" to blobName, "photoUpdatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            ).await()
        }
        true
    }

    /** The phone's copy first; otherwise downloads "<uid>.jpg" from Azure (e.g. on a new phone). */
    suspend fun load(): Bitmap? = withContext(Dispatchers.IO) {
        if (localFile.exists()) return@withContext BitmapFactory.decodeFile(localFile.path)
        val uid = userStore.remoteId ?: return@withContext null
        if (!blobs.isConfigured) return@withContext null
        val bytes = try {
            blobs.download("$uid.jpg")
        } catch (e: Exception) {
            Log.w(TAG, "Could not download the profile photo: ${e.message}")
            null
        } ?: return@withContext null
        localFile.writeBytes(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun compress(uri: Uri): ByteArray? {
        val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return null
        val scale = MAX_SIZE.toFloat() / maxOf(original.width, original.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
        } else {
            original
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    private companion object {
        const val TAG = "ProfilePhotoRepository"
        const val MAX_SIZE = 512
        const val JPEG_QUALITY = 85
    }
}
