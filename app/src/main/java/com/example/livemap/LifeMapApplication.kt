package com.example.livemap

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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

        // Configure Firestore: enable persistent local cache (SQLite-backed) with no size limit.
        // without this, the app would still cache data but only in memory and data would be lost when the app is killed. 
        val settings = firestoreSettings {
            setLocalCacheSettings(
                persistentCacheSettings {
                    // CACHE_SIZE_UNLIMITED tells Firestore not to evict cached documents based on size.
                    setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                }
            )
        }
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}