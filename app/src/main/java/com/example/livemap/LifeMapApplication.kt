package com.example.livemap

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

/**
 * Custom Application class - instantiated once when the app process starts.
 *
 * Responsibilities:
 *   1. Initialize Firebase visibly.
 *   2. Enable Firestore offline persistence with an unlimited local cache.
 *      This is what allows the app to read/write events while offline; the
 *      writes are queued and synced automatically when connectivity returns.
 */

class LifeMapApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase using the values from google-services.json.
        FirebaseApp.initializeApp(this)

        // Configure Firestore: enable persistent (disk-backed) local cache.
        val settings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { })
        }
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}