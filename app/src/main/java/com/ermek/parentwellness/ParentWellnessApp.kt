package com.ermek.parentwellness

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.initialize

class ParentWellnessApp : Application() {
    private val tag = "ParentWellnessApp"

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize Firebase with the latest API
            Firebase.initialize(this)
            Log.d(tag, "Firebase initialized successfully")

            Log.d(tag, "Firebase setup complete")

        } catch (e: Exception) {
            Log.e(tag, "Error initializing Firebase: ${e.message}", e)
        }
    }
}