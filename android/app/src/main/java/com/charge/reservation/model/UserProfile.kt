package com.charge.reservation.model

/**
 * Holds a user's profile details, preference, and roles.
 *
 * @property id the user's Firebase Authentication UID
 * @property name the user's displayed name
 * @property email the user's email address
 * @property phoneNumber the user's phone number
 * @property profileImageUrl the uploaded profile image URL, or null if absent
 * @property notificationEnabled whether the user has enabled app notifications
 * @property isEntrant whether the user has the entrant role
 * @property isOwner whether the user has the campsite owner role
 * @property isAdmin whether the user has the administrator role
 */
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