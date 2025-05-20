package com.ermek.parentwellness.data.repository

import android.util.Log
import com.ermek.parentwellness.data.model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
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

    private val tag = "ProfileRepository"

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

            // Add empty lists for relationship fields if they don't exist
            if (!updates.containsKey("caregiverIds")) updates["caregiverIds"] = emptyList<String>()
            if (!updates.containsKey("parentIds")) updates["parentIds"] = emptyList<String>()

            // Use isParent field explicitly
            updates["isParent"] = user.isParent

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
            Log.d(tag, "Attempting to link caregiver $caregiverId to parent $parentId")

            // Get the parent and caregiver documents to verify they exist
            val parentDocRef = usersCollection.document(parentId)
            val caregiverDocRef = usersCollection.document(caregiverId)

            val parentDoc = parentDocRef.get().await()
            val caregiverDoc = caregiverDocRef.get().await()

            if (!parentDoc.exists()) {
                Log.e(tag, "Parent document not found: $parentId")
                return Result.failure(Exception("Parent not found"))
            }

            if (!caregiverDoc.exists()) {
                Log.e(tag, "Caregiver document not found: $caregiverId")
                return Result.failure(Exception("Caregiver not found"))
            }

            // Update the parent document to add caregiver to caregiverIds array
            parentDocRef.update("caregiverIds", FieldValue.arrayUnion(caregiverId)).await()

            // Also update in parents collection if it exists
            val parentCollectionRef = parentsCollection.document(parentId)
            if (parentCollectionRef.get().await().exists()) {
                parentCollectionRef.update("caregiverIds", FieldValue.arrayUnion(caregiverId)).await()
            }

            // Update the caregiver document to add parent to parentIds array
            caregiverDocRef.update("parentIds", FieldValue.arrayUnion(parentId)).await()

            // Also update in caregivers collection if it exists
            val caregiverCollectionRef = caregiversCollection.document(caregiverId)
            if (caregiverCollectionRef.get().await().exists()) {
                caregiverCollectionRef.update("parentIds", FieldValue.arrayUnion(parentId)).await()
            }

            Log.d(tag, "Successfully linked caregiver $caregiverId to parent $parentId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error linking caregiver to parent: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun unlinkCaregiverFromParent(parentId: String, caregiverId: String): Result<Boolean> {
        return try {
            Log.d(tag, "Unlinking caregiver $caregiverId from parent $parentId")

            // Update in users collection
            usersCollection.document(parentId)
                .update("caregiverIds", FieldValue.arrayRemove(caregiverId))
                .await()

            usersCollection.document(caregiverId)
                .update("parentIds", FieldValue.arrayRemove(parentId))
                .await()

            // Update in specialized collections if they exist
            val parentDoc = parentsCollection.document(parentId).get().await()
            if (parentDoc.exists()) {
                parentsCollection.document(parentId)
                    .update("caregiverIds", FieldValue.arrayRemove(caregiverId))
                    .await()
            }

            val caregiverDoc = caregiversCollection.document(caregiverId).get().await()
            if (caregiverDoc.exists()) {
                caregiversCollection.document(caregiverId)
                    .update("parentIds", FieldValue.arrayRemove(parentId))
                    .await()
            }

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

            // Get the parent document from users collection
            val parentDoc = usersCollection.document(parentId).get().await()

            if (!parentDoc.exists()) {
                Log.e(tag, "Parent document not found: $parentId")
                return Result.failure(Exception("Parent not found"))
            }

            // Get the caregiver IDs from the document - safely handle the cast by using empty list as default
            val rawCaregiverIds = parentDoc.get("caregiverIds")
            val caregiverIds = if (rawCaregiverIds is List<*>) {
                rawCaregiverIds.filterIsInstance<String>()
            } else {
                emptyList()
            }

            if (caregiverIds.isEmpty()) {
                Log.d(tag, "No caregivers found for parent")
                return Result.success(emptyList())
            }

            // Fetch all caregivers
            val caregivers = mutableListOf<User>()
            for (caregiverId in caregiverIds) {
                // Try to get from users collection first
                val caregiverDoc = usersCollection.document(caregiverId).get().await()

                if (caregiverDoc.exists()) {
                    val caregiver = caregiverDoc.toObject(User::class.java)
                    if (caregiver != null) {
                        caregivers.add(caregiver)
                    }
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

            // Get the caregiver document from users collection
            val caregiverDoc = usersCollection.document(caregiverId).get().await()

            if (!caregiverDoc.exists()) {
                Log.e(tag, "Caregiver document not found: $caregiverId")
                return Result.failure(Exception("Caregiver not found"))
            }

            // Get the parent IDs from the document - safely handle the cast by using empty list as default
            val rawParentIds = caregiverDoc.get("parentIds")
            val parentIds = if (rawParentIds is List<*>) {
                rawParentIds.filterIsInstance<String>()
            } else {
                emptyList()
            }

            if (parentIds.isEmpty()) {
                Log.d(tag, "No parents found for caregiver")
                return Result.success(emptyList())
            }

            // Fetch all parents
            val parents = mutableListOf<User>()
            for (parentId in parentIds) {
                // Try to get from users collection first
                val parentDoc = usersCollection.document(parentId).get().await()

                if (parentDoc.exists()) {
                    val parent = parentDoc.toObject(User::class.java)
                    if (parent != null) {
                        parents.add(parent)
                    }
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

            // Search in users collection first (preferred)
            var querySnapshot = usersCollection.whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val user = querySnapshot.documents[0].toObject(User::class.java)
                Log.d(tag, "Found user with email: $email, id: ${user?.id}")
                return Result.success(user)
            }

            // Then search in parents collection as fallback
            querySnapshot = parentsCollection.whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val user = querySnapshot.documents[0].toObject(User::class.java)
                Log.d(tag, "Found parent with email: $email, id: ${user?.id}")
                return Result.success(user)
            }

            // Finally search in caregivers collection as fallback
            querySnapshot = caregiversCollection.whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val user = querySnapshot.documents[0].toObject(User::class.java)
                Log.d(tag, "Found caregiver with email: $email, id: ${user?.id}")
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