package com.ermek.parentwellness.data.repository

import android.util.Log
import com.ermek.parentwellness.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ProfileRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    private val TAG = "ProfileRepository"

    suspend fun saveUserProfile(user: User): Result<User> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            Log.d(TAG, "Saving user profile for ID: $userId with data: ${user.fullName}, ${user.gender}, ${user.birthDate}")

            // Make sure we preserve the user ID and email from auth
            val email = auth.currentUser?.email ?: user.email

            // Create a map of non-null fields to update
            val updates = mutableMapOf<String, Any>(
                "id" to userId,
                "email" to email,
                "updatedAt" to System.currentTimeMillis()
            )

            // Only add non-empty values to the update
            if (user.fullName.isNotEmpty()) updates["fullName"] = user.fullName
            if (user.gender.isNotEmpty()) updates["gender"] = user.gender
            if (user.birthDate.isNotEmpty()) updates["birthDate"] = user.birthDate
            if (user.phoneNumber.isNotEmpty()) updates["phoneNumber"] = user.phoneNumber
            if (!user.profilePictureUrl.isNullOrEmpty()) updates["profilePictureUrl"] = user.profilePictureUrl

            // Check if document exists
            val docRef = usersCollection.document(userId)
            val document = docRef.get().await()

            if (document.exists()) {
                // Document exists, update it
                Log.d(TAG, "Updating existing user document with fields: ${updates.keys}")
                docRef.update(updates).await()
            } else {
                // Document doesn't exist, create it with merge option
                Log.d(TAG, "Creating new user document with fields: ${updates.keys}")
                docRef.set(updates, SetOptions.merge()).await()
            }

            // Fetch the updated user to return
            val updatedDoc = docRef.get().await()
            val updatedUser = updatedDoc.toObject(User::class.java) ?: user

            Log.d(TAG, "User profile saved successfully: ${updatedUser.fullName}")
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile", e)
            Result.failure(e)
        }
    }
    // Add these methods to your ProfileRepository.kt file

    suspend fun linkCaregiverToParent(parentId: String, caregiverId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Linking caregiver $caregiverId to parent $parentId")

            // Get current parent data
            val parentDoc = usersCollection.document(parentId).get().await()
            val parent = parentDoc.toObject(User::class.java) ?:
            return Result.failure(Exception("Parent not found"))

            // Get current caregiver data
            val caregiverDoc = usersCollection.document(caregiverId).get().await()
            val caregiver = caregiverDoc.toObject(User::class.java) ?:
            return Result.failure(Exception("Caregiver not found"))

            // Update parent's caregiver list
            val updatedCaregiverIds = parent.caregiverIds.toMutableList()
            if (!updatedCaregiverIds.contains(caregiverId)) {
                updatedCaregiverIds.add(caregiverId)
            }

            // Update caregiver's parent list
            val updatedParentIds = caregiver.parentIds.toMutableList()
            if (!updatedParentIds.contains(parentId)) {
                updatedParentIds.add(parentId)
            }

            // Perform both updates in a transaction
            firestore.runTransaction { transaction ->
                // Update parent document
                transaction.update(
                    usersCollection.document(parentId),
                    "caregiverIds", updatedCaregiverIds
                )

                // Update caregiver document
                transaction.update(
                    usersCollection.document(caregiverId),
                    "parentIds", updatedParentIds
                )
            }.await()

            Log.d(TAG, "Successfully linked caregiver to parent")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error linking caregiver to parent", e)
            Result.failure(e)
        }
    }

    suspend fun unlinkCaregiverFromParent(parentId: String, caregiverId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Unlinking caregiver $caregiverId from parent $parentId")

            // Get current parent data
            val parentDoc = usersCollection.document(parentId).get().await()
            val parent = parentDoc.toObject(User::class.java) ?:
            return Result.failure(Exception("Parent not found"))

            // Get current caregiver data
            val caregiverDoc = usersCollection.document(caregiverId).get().await()
            val caregiver = caregiverDoc.toObject(User::class.java) ?:
            return Result.failure(Exception("Caregiver not found"))

            // Update parent's caregiver list
            val updatedCaregiverIds = parent.caregiverIds.toMutableList()
            updatedCaregiverIds.remove(caregiverId)

            // Update caregiver's parent list
            val updatedParentIds = caregiver.parentIds.toMutableList()
            updatedParentIds.remove(parentId)

            // Perform both updates in a transaction
            firestore.runTransaction { transaction ->
                // Update parent document
                transaction.update(
                    usersCollection.document(parentId),
                    "caregiverIds", updatedCaregiverIds
                )

                // Update caregiver document
                transaction.update(
                    usersCollection.document(caregiverId),
                    "parentIds", updatedParentIds
                )
            }.await()

            Log.d(TAG, "Successfully unlinked caregiver from parent")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error unlinking caregiver from parent", e)
            Result.failure(e)
        }
    }

    suspend fun getCaregiversForParent(parentId: String): Result<List<User>> {
        return try {
            Log.d(TAG, "Fetching caregivers for parent $parentId")

            // Get parent to get list of caregiver IDs
            val parentDoc = usersCollection.document(parentId).get().await()
            val parent = parentDoc.toObject(User::class.java) ?:
            return Result.failure(Exception("Parent not found"))

            val caregiverIds = parent.caregiverIds

            if (caregiverIds.isEmpty()) {
                Log.d(TAG, "No caregivers found for parent")
                return Result.success(emptyList())
            }

            // Fetch all caregivers
            val caregivers = mutableListOf<User>()
            for (caregiverId in caregiverIds) {
                val caregiverDoc = usersCollection.document(caregiverId).get().await()
                val caregiver = caregiverDoc.toObject(User::class.java)

                if (caregiver != null) {
                    caregivers.add(caregiver)
                }
            }

            Log.d(TAG, "Found ${caregivers.size} caregivers for parent")
            Result.success(caregivers)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching caregivers for parent", e)
            Result.failure(e)
        }
    }

    suspend fun getParentsForCaregiver(caregiverId: String): Result<List<User>> {
        return try {
            Log.d(TAG, "Fetching parents for caregiver $caregiverId")

            // Get caregiver to get list of parent IDs
            val caregiverDoc = usersCollection.document(caregiverId).get().await()
            val caregiver = caregiverDoc.toObject(User::class.java) ?:
            return Result.failure(Exception("Caregiver not found"))

            val parentIds = caregiver.parentIds

            if (parentIds.isEmpty()) {
                Log.d(TAG, "No parents found for caregiver")
                return Result.success(emptyList())
            }

            // Fetch all parents
            val parents = mutableListOf<User>()
            for (parentId in parentIds) {
                val parentDoc = usersCollection.document(parentId).get().await()
                val parent = parentDoc.toObject(User::class.java)

                if (parent != null) {
                    parents.add(parent)
                }
            }

            Log.d(TAG, "Found ${parents.size} parents for caregiver")
            Result.success(parents)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching parents for caregiver", e)
            Result.failure(e)
        }
    }

    suspend fun findUserByEmail(email: String): Result<User?> {
        return try {
            Log.d(TAG, "Searching for user with email: $email")

            val querySnapshot = usersCollection.whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.d(TAG, "No user found with email: $email")
                return Result.success(null)
            }

            val user = querySnapshot.documents[0].toObject(User::class.java)
            Log.d(TAG, "Found user with email: $email, id: ${user?.id}")

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding user by email", e)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): Result<User> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            Log.d(TAG, "Fetching user profile for ID: $userId")

            val documentSnapshot = usersCollection.document(userId).get().await()

            if (!documentSnapshot.exists()) {
                Log.w(TAG, "User document does not exist for ID: $userId")

                // Create a basic user with data from FirebaseAuth
                val email = auth.currentUser?.email ?: ""
                val defaultUser = User(id = userId, email = email)

                Log.d(TAG, "Created default user with email: $email")
                return Result.success(defaultUser)
            }

            var user = documentSnapshot.toObject(User::class.java)

            if (user == null) {
                Log.w(TAG, "Failed to parse user data, creating default user")
                user = User(id = userId, email = auth.currentUser?.email ?: "")
            } else {
                // Ensure ID is set properly
                if (user.id.isEmpty()) {
                    user = user.copy(id = userId)
                }

                // Ensure email is set properly
                if (user.email.isEmpty()) {
                    user = user.copy(email = auth.currentUser?.email ?: "")
                }

                Log.d(TAG, "User profile fetched: ${user.fullName}, email: ${user.email}")
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
            Result.failure(e)
        }
    }
}