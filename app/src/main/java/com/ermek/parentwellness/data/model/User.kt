package com.ermek.parentwellness.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val id: String = "",
    val email: String = "",
    var fullName: String = "",

    @get:PropertyName("isParent")
    @set:PropertyName("isParent")
    var isParent: Boolean = true,

    @get:PropertyName("parent")
    @set:PropertyName("parent")
    var isParentAlternate: Boolean = true,

    val profile: UserProfile? = null,
    var birthDate: String = "",
    var gender: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",

    // New fields for caregiver relationships
    val caregiverIds: List<String> = emptyList(),
    val parentIds: List<String> = emptyList(),

    val createdAt: Long = 0
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