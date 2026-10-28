package com.charge.reservation.model

data class UserProfile(
    val id: String = "",
    val name: String ="",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String? = null,

    val notificationEnabled: Boolean = true,

    val isEntrant: Boolean = true,
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false
)