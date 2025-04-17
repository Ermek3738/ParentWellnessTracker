package com.ermek.parentwellness.data.repository

import android.util.Log
import com.ermek.parentwellness.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    private val TAG = "AuthRepository"

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Attempting to sign in with email: $email")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()

            authResult.user?.let {
                Log.d(TAG, "Sign in successful for user: ${it.uid}")
                Result.success(it)
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, name: String, isParent: Boolean): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Creating Firebase Auth account for: $email")
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()

            authResult.user?.let { firebaseUser ->
                Log.d("AuthRepository", "Auth account created: ${firebaseUser.uid}")

                // Create user document in Firestore
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    fullName = name,
                    isParent = isParent,
                    createdAt = System.currentTimeMillis()
                )

                try {
                    Log.d("AuthRepository", "Attempting to store user in Firestore")
                    usersCollection.document(firebaseUser.uid).set(user).await()
                    Log.d("AuthRepository", "User stored in Firestore successfully")
                } catch (e: Exception) {
                    // If Firestore fails, we still consider registration successful
                    // since Firebase Auth account was created
                    Log.e("AuthRepository", "Failed to create Firestore document: ${e.message}")
                }

                Result.success(firebaseUser)
            } ?: Result.failure(Exception("User creation failed - null user returned"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firebase Auth account creation failed", e)
            Result.failure(e)
        }
    }

    // Example optimization for AuthRepository.kt
    suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val documentSnapshot = usersCollection.document(firebaseUser.uid).get().await()

                val user = documentSnapshot.toObject(User::class.java)

                // If we have a profile object with a fullName, but the main fullName is empty, use the one from profile
                if (user?.fullName.isNullOrEmpty() && user?.profile?.fullName?.isNotEmpty() == true) {
                    user.fullName = user.profile.fullName
                }

                // Same for other fields if needed
                if (user?.birthDate.isNullOrEmpty() && user?.profile?.birthDate?.isNotEmpty() == true) {
                    user.birthDate = user.profile.birthDate
                }

                if (user?.gender.isNullOrEmpty() && user?.profile?.gender?.isNotEmpty() == true) {
                    user.gender = user.profile.gender
                }

                Log.d("AuthRepository", "User data fetched: ${user?.fullName ?: "Unknown"}")
                user
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error fetching user data", e)
                null
            }
        }
    }

    fun signOut() {
        Log.d(TAG, "Signing out user")
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = auth.currentUser != null
        Log.d(TAG, "Checking if user is logged in: $isLoggedIn")
        return isLoggedIn
    }
}
