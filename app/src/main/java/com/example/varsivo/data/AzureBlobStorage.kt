package com.example.varsivo.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Uploads and downloads files in an Azure Blob Storage container through its REST API.
 * Access uses a container SAS (shared access signature) limited to read/add/create/write on
 * the "profile-photos" container only, with an expiry date.
 *
 * Reference: Microsoft (2025) *Put Blob (REST API) - Azure Storage*
 * https://learn.microsoft.com/en-us/rest/api/storageservices/put-blob
 */
class AzureBlobStorage(private val containerSasUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** False until the SAS URL has been added to secrets.properties. */
    val isConfigured: Boolean
        get() = containerSasUrl.startsWith("https://") && containerSasUrl.contains("?")

    /** https://account.blob.core.windows.net/container/<name>?<sas> */
    private fun blobUrl(blobName: String): String {
        val base = containerSasUrl.substringBefore("?").trimEnd('/')
        val sas = containerSasUrl.substringAfter("?")
        return "$base/$blobName?$sas"
    }

    /** PUT Blob: creates or replaces [blobName]. Throws IOException when Azure refuses it. */
    suspend fun upload(blobName: String, bytes: ByteArray, contentType: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(blobUrl(blobName))
            .put(bytes.toRequestBody(contentType.toMediaType()))
            .header("x-ms-blob-type", "BlockBlob")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Azure upload failed: HTTP ${response.code}")
        }
        Log.i(TAG, "Uploaded $blobName (${bytes.size} bytes) to Azure Blob Storage")
    }

    /** GET Blob: returns the bytes, or null when the blob does not exist. */
    suspend fun download(blobName: String): ByteArray? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(blobUrl(blobName)).get().build()
        client.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> null
                !response.isSuccessful -> throw IOException("Azure download failed: HTTP ${response.code}")
                else -> response.body?.bytes()
            }
        }
    }

    private companion object {
        const val TAG = "AzureBlobStorage"
    }
}
