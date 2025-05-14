// In ParentWellnessApplication.kt
package com.ermek.parentwellness

import android.app.Application
import com.ermek.parentwellness.data.repository.HealthDataRepository
import com.google.firebase.FirebaseApp

class ParentWellnessApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize data sync
        val healthDataRepository = HealthDataRepository(this)
        healthDataRepository.initializeSync()
    }
}