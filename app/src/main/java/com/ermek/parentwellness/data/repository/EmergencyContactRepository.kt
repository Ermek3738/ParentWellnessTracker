package com.ermek.parentwellness.data.repository

import android.util.Log
import com.ermek.parentwellness.data.model.EmergencyContact
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EmergencyContactRepository {
    private val tag = "EmergencyContactRepo"
    private val firestore = Firebase.firestore
    private val authRepository = AuthRepository()

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()

    // Get all emergency contacts for the current user
    suspend fun getEmergencyContacts(): Result<List<EmergencyContact>> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(com.ermek.parentwellness.data.model.User::class.java)

            val contacts = user?.emergencyContacts ?: emptyList()
            _emergencyContacts.value = contacts.sortedBy { it.priority }

            Result.success(contacts)
        } catch (e: Exception) {
            Log.e(tag, "Error getting emergency contacts", e)
            Result.failure(e)
        }
    }

    // Add a new emergency contact
    suspend fun addEmergencyContact(
        name: String,
        phoneNumber: String,
        relationship: String,
        isNotified: Boolean = true
    ): Result<EmergencyContact> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Get current contacts
            val currentContacts = _emergencyContacts.value.toMutableList()

            // Create new contact
            val newContact = EmergencyContact(
                id = UUID.randomUUID().toString(),
                name = name,
                phoneNumber = phoneNumber,
                relationship = relationship,
                priority = currentContacts.size,
                isNotified = isNotified
            )

            // Add to list
            currentContacts.add(newContact)

            // Update Firestore
            firestore.collection("users").document(userId)
                .update("emergencyContacts", currentContacts)
                .await()

            // Update local state
            _emergencyContacts.value = currentContacts

            Result.success(newContact)
        } catch (e: Exception) {
            Log.e(tag, "Error adding emergency contact", e)
            Result.failure(e)
        }
    }

    // Update an existing emergency contact
    suspend fun updateEmergencyContact(contact: EmergencyContact): Result<Boolean> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Get current contacts
            val currentContacts = _emergencyContacts.value.toMutableList()

            // Find and update the contact
            val index = currentContacts.indexOfFirst { it.id == contact.id }
            if (index == -1) {
                return Result.failure(Exception("Contact not found"))
            }

            currentContacts[index] = contact

            // Update Firestore
            firestore.collection("users").document(userId)
                .update("emergencyContacts", currentContacts)
                .await()

            // Update local state
            _emergencyContacts.value = currentContacts

            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error updating emergency contact", e)
            Result.failure(e)
        }
    }

    // Delete an emergency contact
    suspend fun deleteEmergencyContact(contactId: String): Result<Boolean> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Get current contacts
            val currentContacts = _emergencyContacts.value.toMutableList()

            // Remove the contact
            val updated = currentContacts.filter { it.id != contactId }

            // Update priorities
            val updatedWithPriorities = updated.mapIndexed { index, contact ->
                contact.copy(priority = index)
            }

            // Update Firestore
            firestore.collection("users").document(userId)
                .update("emergencyContacts", updatedWithPriorities)
                .await()

            // Update local state
            _emergencyContacts.value = updatedWithPriorities

            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting emergency contact", e)
            Result.failure(e)
        }
    }

    // Reorder emergency contacts (change priorities)
    suspend fun reorderEmergencyContacts(contactIds: List<String>): Result<Boolean> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Get current contacts
            val currentContacts = _emergencyContacts.value.toMutableList()

            // Create a map of contact id to contact
            val contactsMap = currentContacts.associateBy { it.id }

            // Create new ordered list with updated priorities
            val reorderedContacts = contactIds.mapIndexed { index, id ->
                contactsMap[id]?.copy(priority = index)
                    ?: return Result.failure(Exception("Contact not found"))
            }

            // Update Firestore
            firestore.collection("users").document(userId)
                .update("emergencyContacts", reorderedContacts)
                .await()

            // Update local state
            _emergencyContacts.value = reorderedContacts

            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error reordering emergency contacts", e)
            Result.failure(e)
        }
    }
}