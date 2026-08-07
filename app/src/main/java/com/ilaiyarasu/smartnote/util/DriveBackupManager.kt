package com.ilaiyarasu.smartnote.util

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.ilaiyarasu.smartnote.data.Note

private const val BACKUP_FILE_NAME = "smartnote_backup.json"

class DriveBackupManager(private val context: Context) {

    fun getSignInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestIdToken("664332059630-0f92egn5i4mont1kn1maqibq55lcvli6.apps.googleusercontent.com")
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    suspend fun silentSignIn(): GoogleSignInAccount? = withContext(Dispatchers.IO) {
        try {
            val client = getSignInClient()
            val account = Tasks.await(client.silentSignIn())
            android.util.Log.d("DriveDebug", "Silent sign-in SUCCEEDED: ${account?.email}")
            account
        } catch (e: Exception) {
            android.util.Log.e("DriveDebug", "Silent sign-in FAILED: ${e.message}")
            null
        }
    }

    private fun buildDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Smartnote").build()
    }

    private fun notesToJson(notes: List<Note>): String {
        val array = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("category", note.category)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
                put("isDeleted", note.isDeleted)
                put("reminderTime", note.reminderTime ?: JSONObject.NULL)
                put("isPinned", note.isPinned)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun jsonToNotes(json: String): List<Note> {
        val array = JSONArray(json)
        val notes = mutableListOf<Note>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            notes.add(
                Note(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    content = obj.getString("content"),
                    category = obj.getString("category"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    reminderTime = if (obj.isNull("reminderTime")) null else obj.optLong("reminderTime"),
                    isPinned = obj.optBoolean("isPinned", false)
                )
            )
        }
        return notes
    }

    private fun findBackupFileId(service: Drive): String? {
        val result = service.files().list()
            .setSpaces("drive")
            .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
            .setFields("files(id, name, modifiedTime)")
            .setOrderBy("modifiedTime desc")
            .execute()

        val files = result.files ?: emptyList()
        android.util.Log.d("DriveDebug", "Found ${files.size} backup file(s): ${files.map { "${it.id} (${it.modifiedTime})" }}")

        if (files.isEmpty()) return null

        if (files.size > 1) {
            android.util.Log.d("DriveDebug", "Cleaning up ${files.size - 1} duplicates...")
            files.drop(1).forEach { duplicate ->
                try {
                    service.files().delete(duplicate.id).execute()
                    android.util.Log.d("DriveDebug", "Deleted duplicate backup file: ${duplicate.id}")
                } catch (e: Exception) {
                    android.util.Log.e("DriveDebug", "Failed to delete duplicate", e)
                }
            }
        }

        return files.first().id
    }

    suspend fun backupNotes(account: GoogleSignInAccount, notes: List<Note>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val service = buildDriveService(account)
                val jsonContent = notesToJson(notes)
                val encryptedContent = EncryptionHelper.encrypt(context, jsonContent)
                val content = ByteArrayContent("application/octet-stream", encryptedContent.toByteArray())

                val existingFileId = findBackupFileId(service)
                if (existingFileId != null) {
                    service.files().update(existingFileId, null, content).execute()
                } else {
                    val fileMetadata = File().setName(BACKUP_FILE_NAME)
                    service.files().create(fileMetadata, content)
                        .setFields("id")
                        .execute()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun restoreNotes(account: GoogleSignInAccount): Result<List<Note>> =
        withContext(Dispatchers.IO) {
            try {
                val service = buildDriveService(account)
                val fileId = findBackupFileId(service)
                    ?: return@withContext Result.failure(Exception("No backup found on Google Drive"))

                android.util.Log.d("DriveDebug", "Starting download of file: $fileId")
                val outputStream = java.io.ByteArrayOutputStream()
                service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                val encryptedContent = outputStream.toString("UTF-8")
                val jsonContent = EncryptionHelper.decrypt(context, encryptedContent)
                android.util.Log.d("DriveDebug", "Download complete, parsing JSON...")

                val notes = jsonToNotes(jsonContent)
                android.util.Log.d("DriveDebug", "Restore SUCCESS: Parsed ${notes.size} notes")
                Result.success(notes)
            } catch (e: Exception) {
                android.util.Log.e("DriveDebug", "Restore FAILED: ${e.message}", e)
                Result.failure(e)
            }
        }
}
