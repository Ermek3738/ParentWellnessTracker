package com.ermek.parentwellness.data.repository

import android.util.Log
import com.ermek.parentwellness.data.model.User
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val usersCollection = firestore.collection("users")
    private val parentsCollection = firestore.collection("parents")
    private val caregiversCollection = firestore.collection("caregivers")

    private val TAG = "AuthRepository"

    fun debugAuthInfo() {
        val currentUser = auth.currentUser
        Log.d(TAG, "Debug Auth Info:")
        Log.d(TAG, "Current user: ${currentUser?.uid ?: "null"}")
        Log.d(TAG, "Is user logged in: ${isUserLoggedIn()}")
        Log.d(TAG, "Firebase app name: ${auth.app.name}")
        Log.d(TAG, "Firebase app options: ${auth.app.options}")

        // Check if Firebase project is properly configured
        try {
            val projectId = auth.app.options.projectId
            Log.d(TAG, "Firebase project ID: $projectId")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Firebase project info", e)
        }

        // Check package name
        try {
            val packageName = auth.app.applicationContext.packageName
            Log.d(TAG, "App package name: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting package name", e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Attempting to sign in with email: $email")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()

            authResult.user?.let {
                Log.d(TAG, "Sign in successful for user: ${it.uid}")
                Result.success(it)
            } ?: Result.failure(Exception("Authentication failed: null user returned"))
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Firebase Auth exception during sign in: ${e.message}, code: ${e.errorCode}", e)
            Result.failure(e)
        } catch (e: FirebaseNetworkException) {
            Log.e(TAG, "Network error during sign in: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign in: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, name: String, isParent: Boolean): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Creating Firebase Auth account for: $email")
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()

            authResult.user?.let { firebaseUser ->
                Log.d(TAG, "Auth account created: ${firebaseUser.uid}")

                // Create user document in Firestore
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    fullName = name,
                    isParent = isParent,
                    isParentAlternate = isParent, // Set both for compatibility
                    caregiverIds = emptyList(),
                    parentIds = emptyList(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                try {
                    Log.d(TAG, "Attempting to store user in Firestore with ID: ${user.id}")

                    // First, store user in the main users collection
                    usersCollection.document(firebaseUser.uid).set(user).await()
                    Log.d(TAG, "User stored in users collection successfully")

                    // Add to respective collection based on role (for more organized data structure)
                    if (isParent) {
                        parentsCollection.document(firebaseUser.uid).set(user).await()
                        Log.d(TAG, "Parent stored in parents collection successfully")
                    } else {
                        caregiversCollection.document(firebaseUser.uid).set(user).await()
                        Log.d(TAG, "Caregiver stored in caregivers collection successfully")
                    }
                } catch (e: FirebaseFirestoreException) {
                    Log.e(TAG, "Firestore exception storing user data: ${e.message}, code: ${e.code}", e)
                    // We still consider auth successful if Firestore fails
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create Firestore document: ${e.message}", e)
                }

                Result.success(firebaseUser)
            } ?: Result.failure(Exception("User creation failed - null user returned"))
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Firebase Auth exception during sign up: ${e.message}, code: ${e.errorCode}", e)
            Result.failure(e)
        } catch (e: FirebaseNetworkException) {
            Log.e(TAG, "Network error during sign up: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign up: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val userId = firebaseUser.uid
                Log.d(TAG, "Fetching user data for ID: $userId")

                // Try to get user document
                val documentSnapshot = usersCollection.document(userId).get().await()

                if (documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)
                    Log.d(TAG, "User found: ${user?.fullName ?: "Unknown"}")
                    return@withContext handleUserData(user)
                } else {
                    Log.w(TAG, "No user document found, creating one")

                    // Create a basic user document if it doesn't exist
                    val newUser = User(
                        id = userId,
                        email = firebaseUser.email ?: "",
                        fullName = firebaseUser.displayName ?: "",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    // Save the new user document
                    try {
                        usersCollection.document(userId).set(newUser).await()
                        Log.d(TAG, "Created new user document")
                        return@withContext newUser
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create user document", e)
                        return@withContext newUser // Return anyway for app to function
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user data", e)

                // Create a minimal user to prevent app crashes
                return@withContext User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: ""
                )
            }
        }
    }

    private fun handleUserData(user: User?): User? {
        if (user == null) return null

        // If we have a profile object with a fullName, but the main fullName is empty, use the one from profile
        if (user.fullName.isEmpty() && user.profile?.fullName?.isNotEmpty() == true) {
            user.fullName = user.profile.fullName
        }

        // Same for other fields if needed
        if (user.birthDate.isEmpty() && user.profile?.birthDate?.isNotEmpty() == true) {
            user.birthDate = user.profile.birthDate
        }

        if (user.gender.isEmpty() && user.profile?.gender?.isNotEmpty() == true) {
            user.gender = user.profile.gender
        }

        return user
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
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