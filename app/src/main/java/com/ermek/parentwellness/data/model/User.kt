package com.ermek.parentwellness.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val id: String = "",
    val email: String = "",
    var fullName: String = "",

    @get:PropertyName("isParent")
    @set:PropertyName("isParent")
    var isParent: Boolean = true,

    // We can keep this for backward compatibility
    @get:PropertyName("parent")
    @set:PropertyName("parent")
    var isParentAlternate: Boolean = true,

    val profile: UserProfile? = null,
    var birthDate: String = "",
    var gender: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",

    // Relationship fields
    var caregiverIds: List<String> = emptyList(),
    var parentIds: List<String> = emptyList(),

    // Health data permissions - for caregivers to access parent data
    val healthDataPermissions: Map<String, List<String>> = mapOf(),

    // Emergency contact information
    val emergencyContacts: List<EmergencyContact> = emptyList(),

    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class UserProfile(
    val birthDate: String = "",
    val fullName: String = "",
    val gender: String = "",
    val id: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class EmergencyContact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val priority: Int = 0,
    val isNotified: Boolean = true
)