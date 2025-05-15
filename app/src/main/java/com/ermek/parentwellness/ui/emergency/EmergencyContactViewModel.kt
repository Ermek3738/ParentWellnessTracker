package com.ermek.parentwellness.ui.emergency

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ermek.parentwellness.data.model.EmergencyContact
import com.ermek.parentwellness.data.repository.EmergencyContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmergencyContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmergencyContactRepository()
    private val context = application.applicationContext

    private val _contactsState = MutableStateFlow<EmergencyContactState>(EmergencyContactState.Loading)
    val contactsState: StateFlow<EmergencyContactState> = _contactsState.asStateFlow()

    // Load emergency contacts
    fun loadEmergencyContacts() {
        viewModelScope.launch {
            _contactsState.value = EmergencyContactState.Loading

            try {
                val result = repository.getEmergencyContacts()

                if (result.isSuccess) {
                    val contacts = result.getOrNull() ?: emptyList()
                    _contactsState.value = EmergencyContactState.Success(contacts)
                } else {
                    _contactsState.value = EmergencyContactState.Error(
                        result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                _contactsState.value = EmergencyContactState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Add emergency contact
    fun addEmergencyContact(
        name: String,
        phoneNumber: String,
        relationship: String,
        isNotified: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                val result = repository.addEmergencyContact(
                    name = name,
                    phoneNumber = phoneNumber,
                    relationship = relationship,
                    isNotified = isNotified
                )

                if (result.isSuccess) {
                    loadEmergencyContacts()
                }
            } catch (e: Exception) {
                _contactsState.value = EmergencyContactState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Update emergency contact
    fun updateEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                val result = repository.updateEmergencyContact(contact)

                if (result.isSuccess) {
                    loadEmergencyContacts()
                }
            } catch (e: Exception) {
                _contactsState.value = EmergencyContactState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Delete emergency contact
    fun deleteEmergencyContact(contactId: String) {
        viewModelScope.launch {
            try {
                val result = repository.deleteEmergencyContact(contactId)

                if (result.isSuccess) {
                    loadEmergencyContacts()
                }
            } catch (e: Exception) {
                _contactsState.value = EmergencyContactState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Call emergency contact
    fun callEmergencyContact(contact: EmergencyContact) {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ContextCompat.startActivity(context, dialIntent, null)
        } catch (e: Exception) {
            // Handle exception
        }
    }
}

// State for the emergency contacts screen
sealed class EmergencyContactState {
    object Loading : EmergencyContactState()
    data class Success(val contacts: List<EmergencyContact>) : EmergencyContactState()
    data class Error(val message: String) : EmergencyContactState()
}