package com.example.livemap.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun uploadProfilePicture(uid: String, uri: Uri): Result<String> = runCatching {
        val storageRef = storage.reference.child("profile_pictures/$uid.jpg")
        storageRef.putFile(uri).await()
        storageRef.downloadUrl.await().toString()
    }

    suspend fun uploadEventImage(eventId: String, uri: Uri): Result<String> = runCatching {
        val storageRef = storage.reference.child("event_images/$eventId.jpg")
        storageRef.putFile(uri).await()
        storageRef.downloadUrl.await().toString()
    }
}
