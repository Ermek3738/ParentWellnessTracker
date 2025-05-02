package com.ermek.parentwellness.data.repository

import android.util.Log
import com.ermek.parentwellness.data.model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ProfileRepository {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val usersCollection = firestore.collection("users")
    private val parentsCollection = firestore.collection("parents")
    private val caregiversCollection = firestore.collection("caregivers")

    private val tag = "ProfileRepository" // Fixed: TAG -> tag (lowercase)

    suspend fun saveUserProfile(user: User): Result<User> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

            Log.d(tag, "Saving user profile for ID: $userId with data: ${user.fullName}, ${user.gender}, ${user.birthDate}")

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
            if (user.profilePictureUrl.isNotEmpty()) updates["profilePictureUrl"] = user.profilePictureUrl

            // Determine which collection to use
            val isParent = user.isParent
            val primaryCollection = if (isParent) parentsCollection else caregiversCollection

            // Check if document exists in the primary collection
            val docRef = primaryCollection.document(userId)
            val document = docRef.get().await()

            if (document.exists()) {
                // Document exists, update it
                Log.d(tag, "Updating existing user document with fields: ${updates.keys}")
                docRef.update(updates).await()
            } else {
                // Document doesn't exist, create it with merge option
                Log.d(tag, "Creating new user document with fields: ${updates.keys}")
                docRef.set(updates, SetOptions.merge()).await()
            }

            // Also update in the users collection for backward compatibility
            val usersDocRef = usersCollection.document(userId)
            usersDocRef.set(updates, SetOptions.merge()).await()

            // Fetch the updated user to return
            val updatedDoc = docRef.get().await()
            val updatedUser = updatedDoc.toObject(User::class.java) ?: user

            Log.d(tag, "User profile saved successfully: ${updatedUser.fullName}")
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(tag, "Error saving user profile", e)
            Result.failure(e)
        }
    }

    suspend fun linkCaregiverToParent(parentId: String, caregiverId: String): Result<Boolean> {
        return try {
            firestore.runTransaction { transaction ->
                // Get user documents from the primary users collection
                val parentRef = usersCollection.document(parentId)
                val caregiverRef = usersCollection.document(caregiverId)

                val parentDoc = transaction.get(parentRef)
                val caregiverDoc = transaction.get(caregiverRef)

                // Fixed: Safe handling of nullable lists
                val parent = parentDoc.toObject(User::class.java)
                    ?: throw Exception("Parent not found")
                val caregiver = caregiverDoc.toObject(User::class.java)
                    ?: throw Exception("Caregiver not found")

                // Update parent's caregiver list - Fixed Elvis operator handling
                val updatedCaregiverIds = parent.caregiverIds.toMutableList()
                if (!updatedCaregiverIds.contains(caregiverId)) {
                    updatedCaregiverIds.add(caregiverId)
                }

                // Update caregiver's parent list - Fixed Elvis operator handling
                val updatedParentIds = caregiver.parentIds.toMutableList()
                if (!updatedParentIds.contains(parentId)) {
                    updatedParentIds.add(parentId)
                }

                // Perform updates
                transaction.update(parentRef, "caregiverIds", updatedCaregiverIds)
                transaction.update(caregiverRef, "parentIds", updatedParentIds)
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error linking caregiver to parent", e)
            Result.failure(e)
        }
    }

    suspend fun unlinkCaregiverFromParent(parentId: String, caregiverId: String): Result<Boolean> {
        return try {
            Log.d(tag, "Unlinking caregiver $caregiverId from parent $parentId")

            // Get current parent data
            val parentDoc = parentsCollection.document(parentId).get().await()
            var parent = parentDoc.toObject(User::class.java)

            // If not found in parents collection, try users collection
            if (parent == null) {
                val parentDocUsers = usersCollection.document(parentId).get().await()
                parent = parentDocUsers.toObject(User::class.java)
            }

            if (parent == null) {
                return Result.failure(Exception("Parent not found"))
            }

            // Get current caregiver data
            val caregiverDoc = caregiversCollection.document(caregiverId).get().await()
            var caregiver = caregiverDoc.toObject(User::class.java)

            // If not found in caregivers collection, try users collection
            if (caregiver == null) {
                val caregiverDocUsers = usersCollection.document(caregiverId).get().await()
                caregiver = caregiverDocUsers.toObject(User::class.java)
            }

            if (caregiver == null) {
                return Result.failure(Exception("Caregiver not found"))
            }

            // Update parent's caregiver list - Fixed: safe handling of nullable lists
            val updatedCaregiverIds = parent.caregiverIds.toMutableList()
            updatedCaregiverIds.remove(caregiverId)

            // Update caregiver's parent list - Fixed: safe handling of nullable lists
            val updatedParentIds = caregiver.parentIds.toMutableList()
            updatedParentIds.remove(parentId)

            // Perform both updates in a transaction
            firestore.runTransaction { transaction ->
                // Update parent document in both collections
                transaction.update(parentsCollection.document(parentId), "caregiverIds", updatedCaregiverIds)
                transaction.update(usersCollection.document(parentId), "caregiverIds", updatedCaregiverIds)

                // Update caregiver document in both collections
                transaction.update(caregiversCollection.document(caregiverId), "parentIds", updatedParentIds)
                transaction.update(usersCollection.document(caregiverId), "parentIds", updatedParentIds)
            }.await()

            Log.d(tag, "Successfully unlinked caregiver from parent")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error unlinking caregiver from parent", e)
            Result.failure(e)
        }
    }

    suspend fun getCaregiversForParent(parentId: String): Result<List<User>> {
        return try {
            Log.d(tag, "Fetching caregivers for parent $parentId")

            // First check in parents collection
            var parentDoc = parentsCollection.document(parentId).get().await()
            var parent = parentDoc.toObject(User::class.java)

            // If not found, check in users collection for backward compatibility
            if (parent == null) {
                parentDoc = usersCollection.document(parentId).get().await()
                parent = parentDoc.toObject(User::class.java)
            }

            if (parent == null) {
                return Result.failure(Exception("Parent not found"))
            }

            // Fixed: Safe handling of nullable lists
            val caregiverIds = parent.caregiverIds

            if (caregiverIds.isEmpty()) {
                Log.d(tag, "No caregivers found for parent")
                return Result.success(emptyList())
            }

            // Fetch all caregivers
            val caregivers = mutableListOf<User>()
            for (caregiverId in caregiverIds) {
                // First try caregivers collection
                var caregiverDoc = caregiversCollection.document(caregiverId).get().await()
                var caregiver = caregiverDoc.toObject(User::class.java)

                // If not found, try users collection
                if (caregiver == null) {
                    caregiverDoc = usersCollection.document(caregiverId).get().await()
                    caregiver = caregiverDoc.toObject(User::class.java)
                }

                if (caregiver != null) {
                    caregivers.add(caregiver)
                }
            }

            Log.d(tag, "Found ${caregivers.size} caregivers for parent")
            Result.success(caregivers)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching caregivers for parent", e)
            Result.failure(e)
        }
    }

    suspend fun getParentsForCaregiver(caregiverId: String): Result<List<User>> {
        return try {
            Log.d(tag, "Fetching parents for caregiver $caregiverId")

            // First check in caregivers collection
            var caregiverDoc = caregiversCollection.document(caregiverId).get().await()
            var caregiver = caregiverDoc.toObject(User::class.java)

            // If not found, check in users collection for backward compatibility
            if (caregiver == null) {
                caregiverDoc = usersCollection.document(caregiverId).get().await()
                caregiver = caregiverDoc.toObject(User::class.java)
            }

            if (caregiver == null) {
                return Result.failure(Exception("Caregiver not found"))
            }

            // Fixed: Safe handling of nullable lists
            val parentIds = caregiver.parentIds

            if (parentIds.isEmpty()) {
                Log.d(tag, "No parents found for caregiver")
                return Result.success(emptyList())
            }

            // Fetch all parents
            val parents = mutableListOf<User>()
            for (parentId in parentIds) {
                // First try parents collection
                var parentDoc = parentsCollection.document(parentId).get().await()
                var parent = parentDoc.toObject(User::class.java)

                // If not found, try users collection
                if (parent == null) {
                    parentDoc = usersCollection.document(parentId).get().await()
                    parent = parentDoc.toObject(User::class.java)
                }

                if (parent != null) {
                    parents.add(parent)
                }
            }

            Log.d(tag, "Found ${parents.size} parents for caregiver")
            Result.success(parents)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching parents for caregiver", e)
            Result.failure(e)
        }
    }

    suspend fun findUserByEmail(email: String): Result<User?> {
        return try {
            Log.d(tag, "Searching for user with email: $email")

            // First search in parents collection
            var querySnapshot = parentsCollection.whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val user = querySnapshot.documents[0].toObject(User::class.java)
                Log.d(tag, "Found parent with email: $email, id: ${user?.id}")
                return Result.success(user)
            }

            // Then search in caregivers collection
            querySnapshot = caregiversCollection.whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val user = querySnapshot.documents[0].toObject(User::class.java)
                Log.d(tag, "Found caregiver with email: $email, id: ${user?.id}")
                return Result.success(user)
            }

            // Finally search in users collection for backward compatibility
            querySnapshot = usersCollection.whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val user = querySnapshot.documents[0].toObject(User::class.java)
                Log.d(tag, "Found user with email: $email, id: ${user?.id}")
                return Result.success(user)
            }

            Log.d(tag, "No user found with email: $email")
            return Result.success(null)
        } catch (e: Exception) {
            Log.e(tag, "Error finding user by email", e)
            Result.failure(e)
        }
    }
}