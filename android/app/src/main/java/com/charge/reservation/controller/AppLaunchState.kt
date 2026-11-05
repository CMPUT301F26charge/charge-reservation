package com.charge.reservation.controller

import com.charge.reservation.model.UserProfile

/**
 * Describes the current stage when launching the app.
 */
sealed class AppLaunchState {
    // authentication or profile loading is in progress.
    data object Loading: AppLaunchState()

    // account exists, but profile needs to be created.
    data class CreateProfile(
        val userId: String
    ): AppLaunchState()

    // profile successfully loaded and ready to use.
    data class ProfileReady(
        val profile: UserProfile
    ): AppLaunchState()

    // The profile is removed.
    data object ProfileRemoved: AppLaunchState()

    //Launch failed
    data class Error(
        val message: String
    ): AppLaunchState()

}