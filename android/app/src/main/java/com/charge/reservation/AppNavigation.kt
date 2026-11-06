package com.charge.reservation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.charge.reservation.notifications.NotificationsRoute
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.net.Uri
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.charge.reservation.reservation.BookingsScreen

import com.charge.reservation.owner.OwnerCampsiteDetailsScreen
import com.charge.reservation.model.Campsite
import com.charge.reservation.owner.OwnerCampsiteListScreen
import com.charge.reservation.model.UserProfile
import com.charge.reservation.repository.CampsiteRepository
import com.charge.reservation.model.AvailabilityDay
import com.charge.reservation.owner.CreateCampsiteScreen
import com.charge.reservation.ui.AppBottomBar
import com.charge.reservation.profile.ProfileMenuScreen
import com.charge.reservation.admin.AdminMenuScreen
import com.charge.reservation.entrant.ExploreCampsitesScreen
import com.charge.reservation.ui.CampsiteDetailsScreen
import com.charge.reservation.repository.LotteryLookupRepository
import com.charge.reservation.repository.FoundLottery
import com.charge.reservation.lottery.CreateLotteryScreen
import com.charge.reservation.repository.LotteryRepository
import com.charge.reservation.model.Lottery
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.charge.reservation.booking.InvitationBookingScreen

import com.charge.reservation.profile.EditProfileScreen
import com.charge.reservation.repository.UserProfileRepository


import com.charge.reservation.profile.NotificationSettingsScreen

import com.charge.reservation.repository.WaitlistRepository
import com.charge.reservation.repository.LotteryEntrantsRepository

import com.charge.reservation.repository.OwnerBookingsRepository
import com.charge.reservation.model.Reservation

/**
 * Connects role navigation with campsite and lottery repository operations.
 * Entrant lottery and booking destinations are currently placeholders.
 *
 * @param profile signed-in user's profile used for role and ownership checks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    profile: UserProfile,
    onProfileUpdated: (UserProfile) -> Unit) {
    val navController = rememberNavController()
    val currentScreenEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentScreenEntry?.destination?.route
    val showBackButton = currentRoute in listOf(
        "ownerCampsiteDetails/{campsiteId}",
        "createCampsite",
        "createLottery/{campsiteId}",
        "invitation/{lotteryId}",
        "personalInfo",
        "notifications",
        "messages",
        "policies",
        "adminProfiles",
        "adminImages",
        "adminNotificationLogs",
        "adminMessageLogs",
        "notificationSettings"
    ) && navController.previousBackStackEntry != null
    val campsiteRepository = remember { CampsiteRepository() }
    val lotteryLookupRepository = remember { LotteryLookupRepository() }
    val lotteryRepository = remember { LotteryRepository() }

    val availableRoles = buildList<String> {
        if (!profile.removed) {
            if (profile.isEntrant) add("Entrant")
            if (profile.isOwner) add("Owner")
            if (profile.isAdmin) add("Admin")
        }
    }

    var selectedRole by rememberSaveable(profile.id) {
        mutableStateOf(
            if ("Owner" in availableRoles) {
                "Owner"
            } else {
                availableRoles.firstOrNull().orEmpty()
            }
        )
    }

    // Check the role selection against the profile's current roles.
    val activeRole = selectedRole.takeIf { it in availableRoles }
        ?: availableRoles.firstOrNull()

    var showRoleMenu by remember(profile.id) {
        mutableStateOf(false)
    }

    var ownerCampsites by remember(profile.id) {
        mutableStateOf<List<Campsite>>(emptyList())
    }
    var isLoadingOwnerCampsites by remember(profile.id) {
        mutableStateOf(true)
    }
    var ownerCampsitesError by remember(profile.id) {
        mutableStateOf("")
    }
    var ownerListRetry by remember(profile.id) {
        mutableStateOf(0)
    }
    val userProfileRepository = remember {
        UserProfileRepository()
    }
    val waitlistRepository = remember { WaitlistRepository() }
    val lotteryEntrantsRepository = remember {
        LotteryEntrantsRepository()
    }
    val ownerBookingsRepository = remember {
        OwnerBookingsRepository()
    }

    if (profile.isOwner && !profile.removed) {
        DisposableEffect(profile.id, campsiteRepository, ownerListRetry) {
            isLoadingOwnerCampsites = true
            ownerCampsitesError = ""

            val listener = campsiteRepository.listenToOwnerCampsites(
                ownerId = profile.id,
                onSuccess = { campsites ->
                    ownerCampsites = campsites
                    ownerCampsitesError = ""
                    isLoadingOwnerCampsites = false
                },
                onFailure = { exception ->
                    Log.e("OwnerCampsites","Can not load owner campsites",exception)

                    ownerCampsites = emptyList()
                    ownerCampsitesError = "Can not load your campsites. Please try again."
                    isLoadingOwnerCampsites = false
                }
            )

            onDispose { listener.remove() }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Reservation App")
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_nav_back),
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate("notifications") {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_nav_notifications
                            ),
                            contentDescription = "Notifications"
                        )
                    }
                    Box {
                        TextButton(
                            enabled = availableRoles.size > 1,
                            onClick = { showRoleMenu = true }
                        ) {
                            Text("Role: ${activeRole ?: "None"}")
                        }

                        DropdownMenu(
                            expanded = showRoleMenu,
                            onDismissRequest = { showRoleMenu = false }
                        ) {
                            availableRoles.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        showRoleMenu = false

                                        if (role != activeRole) {
                                            selectedRole = role

                                            navController.navigate("home") {
                                                popUpTo("home") {
                                                    inclusive = true
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            AppBottomBar(
                activeRole = activeRole,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo("home")
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.Center
        ) {

            composable("home") {
                ExploreCampsitesScreen(
                    navController = navController,
                    onCampsiteClick = { campsite ->
                        navController.navigate(
                            "entrantCampsiteDetails/${Uri.encode(campsite.id)}"
                        )
                    }
                )
            }

            composable("entrantCampsiteDetails/{campsiteId}") { entry ->
                val campsiteId = entry.arguments?.getString("campsiteId").orEmpty()

                var selectedCampsite by remember(campsiteId) {
                    mutableStateOf<Campsite?>(null)
                }
                var isLoadingCampsite by remember(campsiteId) {
                    mutableStateOf(true)
                }
                var campsiteError by remember(campsiteId) {
                    mutableStateOf("")
                }
                var isJoiningWaitlist by remember(campsite.id) {
                    mutableStateOf(false)
                }

                var joinMessage by remember(campsite.id) {
                    mutableStateOf("")
                }

                DisposableEffect(campsiteId) {
                    val listener = campsiteRepository.listenToActiveCampsites(
                        onSuccess = { campsites ->
                            selectedCampsite = campsites.find { it.id == campsiteId }
                            isLoadingCampsite = false
                            campsiteError = ""
                        },
                        onFailure = { exception ->
                            campsiteError = exception.message ?: "Could not load campsite"
                            isLoadingCampsite = false
                        }
                    )

                    onDispose { listener.remove() }
                }

                when {
                    isLoadingCampsite -> CircularProgressIndicator()
                    campsiteError.isNotEmpty() -> Text("Could not load campsite: $campsiteError")
                    selectedCampsite == null -> Text("Campsite not found.")
                    else -> {
                        CampsiteDetailsScreen(
                            campsite = campsite,
                            onBack = { navController.popBackStack() },
                            onJoinLottery = {
                                val selectedLottery = foundLottery?.lottery

                                if (
                                    profile.isEntrant &&
                                    !profile.removed &&
                                    selectedLottery != null &&
                                    !isJoiningWaitlist
                                ) {
                                    isJoiningWaitlist = true
                                    joinMessage = ""

                                    waitlistRepository.joinWaitlist(
                                        lotteryId = selectedLottery.id,
                                        entrantId = profile.id,
                                        onSuccess = {
                                            isJoiningWaitlist = false
                                            joinMessage = "Joined lottery successfully."

                                            foundLottery = foundLottery?.copy(
                                                waitlistCount =
                                                    (foundLottery?.waitlistCount ?: 0) + 1
                                            )
                                        },
                                        onFailure = { exception ->
                                            isJoiningWaitlist = false
                                            joinMessage = exception.message
                                                ?: "Unable to join this lottery."
                                        }
                                    )
                                }
                            },
                            lottery = foundLottery?.lottery,
                            waitlistCount = foundLottery?.waitlistCount ?: 0
                        )
                    }
                }
            }
        }
                        selectedCampsite?.let { campsite ->
                            var foundLottery by remember(campsite.id) {
                                mutableStateOf<FoundLottery?>(null)
                            }

                            var isLoadingLottery by remember(campsite.id) {
                                mutableStateOf(true)
                            }

                            var lotteryError by remember(campsite.id) {
                                mutableStateOf("")
                            }

        composable("profile") {
            ProfileMenuScreen(
                onPersonalInfoClick = {
                    navController.navigate("personalInfo")
                },
                onNotificationsClick = {
                    navController.navigate("notificationSettings")
                },
                onMessagesClick = {
                    navController.navigate("messages")
                },
                onPoliciesClick = {
                    navController.navigate("policies")
                }
            )
        }
                            var availabilityDays by remember(campsite.id) {
                                mutableStateOf<List<AvailabilityDay>>(emptyList())
                            }

                            var isLoadingAvailability by remember(campsite.id) {
                                mutableStateOf(true)
                            }

                            var availabilityError by remember(campsite.id) {
                                mutableStateOf("")
                            }

                            val lotteryAction = rememberLotteryAction(
                                lottery = foundLottery?.lottery,
                                waitlistCount = foundLottery?.waitlistCount ?: 0,
                                entrantId = profile.id
                            )

                            DisposableEffect(campsite.id) {
                                var active = true

                                isLoadingLottery = true
                                lotteryError = ""

                                lotteryLookupRepository.getForCampsite(
                                    campsiteId = campsite.id,
                                    onSuccess = { result ->
                                        if (active) {
                                            foundLottery = result
                                            isLoadingLottery = false
                                        }
                                    },
                                    onFailure = { exception ->
                                        if (active) {
                                            Log.e("LotteryLookup", "Failed to load lottery", exception)

                                            lotteryError = "Lottery information is unavailable. " +
                                                    "Please check your internet connection and try again."

                                            isLoadingLottery = false
                                        }
                                    }
                                )

                                onDispose { active = false }
                            }


                            DisposableEffect(campsite.id) {
                                var active = true

                                isLoadingAvailability = true
                                availabilityError = ""

                                campsiteRepository.getAvailability(
                                    campsiteId = campsite.id,
                                    onSuccess = { days ->
                                        if (active) {
                                            availabilityDays = days
                                            isLoadingAvailability = false
                                        }
                                    },
                                    onFailure = { exception ->
                                        if (active) {
                                            Log.e("CampsiteAvailability", "Failed to load dates", exception)

                                            availabilityError = "Could not load available dates. Please try again."
                                            isLoadingAvailability = false
                                        }
                                    }
                                )

                                onDispose { active = false }
                            }

        composable("notificationSettings") {

            var isSaving by remember {
                mutableStateOf(false)
            }

            var errorMessage by remember {
                mutableStateOf("")
            }

            NotificationSettingsScreen(
                notificationEnabled = profile.notificationEnabled,
                isSaving = isSaving,
                errorMessage = errorMessage,
                onSave = { enabled ->

                    if (!isSaving) {
                        isSaving = true
                        errorMessage = ""

                        userProfileRepository.updateNotificationPreference(
                            userId = profile.id,
                            enabled = enabled,

                            onSuccess = {
                                isSaving = false

                                // Update the in-memory profile after
                                // Firestore successfully saves the change.
                                onProfileUpdated(
                                    profile.copy(
                                        notificationEnabled = enabled
                                    )
                                )
                            },

                            onFailure = { exception ->
                                isSaving = false

                                Log.e(
                                    "NotificationSettings",
                                    "Could not save preference",
                                    exception
                                )

                                errorMessage =
                                    "Could not save notification settings. " +
                                            "Please try again."
                            }
                        )
                    }
                }
            )
        }



        composable("notifications") {
            Text("Notifications, not connected yet")
        }
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (isLoadingLottery) {
                                    Text("Loading lottery...")
                                } else if (lotteryError.isNotEmpty()) {
                                    Text(lotteryError)
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    CampsiteDetailsScreen(
                                        campsite = campsite,
                                        onBack = { navController.popBackStack() },
                                        lottery = foundLottery?.lottery,
                                        waitlistCount = lotteryAction.waitlistCount,
                                        lotteryButtonState = lotteryAction.buttonState,
                                        isLotteryActionWorking = lotteryAction.isWorking,
                                        lotteryActionError = lotteryAction.error,
                                        onLotteryAction = lotteryAction.onClick,

                                        availabilityDays = availabilityDays,
                                        isLoadingAvailability = isLoadingAvailability,
                                        availabilityError = availabilityError

                                    )
                                }
                            }
                        }
                    }
                }
            }

            composable("lotteries") {
                if (profile.isEntrant && !profile.removed) {
                    Text("Lottery entries, not connected yet")
                } else {
                    Text("Only entrants can view this page.")
                }
            }

            composable(
                route = "invitation/{lotteryId}",
                arguments = listOf(navArgument("lotteryId") { type = NavType.StringType })
            ) { entry ->
                val lotteryId = entry.arguments?.getString("lotteryId")
                if (lotteryId == null || !profile.isEntrant || profile.removed) {
                    Text("Only entrants can open invitations.")
                } else {
                    InvitationBookingScreen(
                        lotteryId = lotteryId,
                        onBack = { navController.popBackStack() },
                        onBooked = { navController.popBackStack() }
                    )
                }
            }

            composable("bookings") {
                if (profile.isEntrant && !profile.removed) {
                    BookingsScreen(
                        entrantId = profile.id,
                        onBackClick = {
                            // Return to the existing Home destination.
                            navController.navigate("home") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    )
                } else {
                    Text("Only entrants can view bookings.")
                }
            }

            composable("profile") {
                ProfileMenuScreen(
                    onPersonalInfoClick = {
                        navController.navigate("personalInfo")
                    },
                    onNotificationsClick = {
                        navController.navigate("notifications")
                    },
                    onMessagesClick = {
                        navController.navigate("messages")
                    },
                    onPoliciesClick = {
                        navController.navigate("policies")
                    }
                )
            }

            // Save personal information and update the app's current profile.
            composable("personalInfo") { entry ->
                var isSaving by remember { mutableStateOf(false) }
                var saveError by remember { mutableStateOf("") }

                EditProfileScreen(
                    profile = profile,
                    isSaving = isSaving,
                    saveError = saveError,
                    onSave = { name, email, phoneNumber, imageUri ->
                        if (!isSaving) {
                            isSaving = true
                            saveError = ""

                            userProfileRepository.updateProfile(
                                profile = profile,
                                name = name,
                                email = email,
                                phoneNumber = phoneNumber,
                                imageUri = imageUri,
                                onSuccess = { updatedProfile ->
                                    isSaving = false
                                    onProfileUpdated(updatedProfile)

                                    // Navigate back only if this editor is still open.
                                    if (navController.currentBackStackEntry == entry) {
                                        navController.popBackStack()
                                    }
                                },
                                onFailure = { exception ->
                                    isSaving = false

                                    Log.e(
                                        "ProfileUpdate",
                                        "Could not update profile",
                                        exception
                                    )

                                    saveError =
                                        if (exception is IllegalArgumentException) {
                                            exception.message
                                                ?: "Please check your information."
                                        } else {
                                            "Could not save your profile. Please try again."
                                        }
                                }
                            )
                        }
                    }
                )
            }

            composable("messages") {
                Text("Messages, not connected yet")
            }

            composable("policies") {
                Text("Policy and guidelines, not connected yet")
            }

            composable("notifications") {
                NotificationsRoute(
                    userId = profile.id,
                    onOpenLottery = { lotteryId: String -> Unit },
                    navController
                )
            }

            composable("adminCampsites") {
                if (profile.isAdmin && !profile.removed) {
                    Text("Browse campsites, not connected yet")
                } else {
                    Text("Only administrators can view this page.")
                }
            }

            composable("admin") {
                if (profile.isAdmin && !profile.removed) {
                    AdminMenuScreen(
                        onProfilesClick = {
                            navController.navigate("adminProfiles")
                        },
                        onImagesClick = {
                            navController.navigate("adminImages")
                        },
                        onNotificationLogsClick = {
                            navController.navigate("adminNotificationLogs")
                        },
                        onMessageLogsClick = {
                            navController.navigate("adminMessageLogs")
                        }
                    )
                } else {
                    Text("Only administrators can view this page.")
                }
            }

            composable("adminProfiles") {
                if (profile.isAdmin && !profile.removed) {
                    Text("Browse profiles, not connected yet")
                } else {
                    Text("Only administrators can view this page.")
                }
            }

            composable("adminImages") {
                if (profile.isAdmin && !profile.removed) {
                    Text("Browse images, not connected yet")
                } else {
                    Text("Only administrators can view this page.")
                }
            }


            when {
                !profile.isOwner || profile.removed -> {
                    Text("Only campsite owners can view their campsite listings.")
            composable("adminNotificationLogs") {
                if (profile.isAdmin && !profile.removed) {
                    Text("Notification logs, not connected yet")
                } else {
                    Text("Only administrators can view this page.")
                }
            }

            composable("adminMessageLogs") {
                if (profile.isAdmin && !profile.removed) {
                    Text("Message logs, not connected yet")
                } else {
                    Text("Only administrators can view this page.")
                }
            }

            composable ("ownerCampsites"){
                if (!profile.isOwner || profile.removed) {
                    Text("Only owners can view their campsite listings.")
                } else {
                    when {
                        isLoadingOwnerCampsites -> {
                            CircularProgressIndicator()
                        }

                        ownerCampsitesError.isNotEmpty() -> {
                            Column {
                                Text(ownerCampsitesError)

                                Button(
                                    onClick = { ownerListRetry++ }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }

                        else -> {
                            OwnerCampsiteListScreen(
                                currentOwnerId = profile.id,
                                campsites = ownerCampsites,
                                onCampsiteClick = { campsiteId ->
                                    navController.navigate(
                                        "ownerCampsiteDetails/${Uri.encode(campsiteId)}"
                                    )
                                },
                                onRegisterCampsiteClick = {
                                    navController.navigate("createCampsite")
                                }
                            )
                        }
                    }


                }
            }

            composable("ownerCampsiteDetails/{campsiteId}") { entry ->
                val campsiteId = entry.arguments?.getString("campsiteId")

                when {
                    !profile.isOwner || profile.removed -> {
                        Text("Only campsite owners can view their campsite listings.")
                    }

                    isLoadingOwnerCampsites -> {
                        CircularProgressIndicator()
                    }

                    ownerCampsitesError.isNotEmpty() -> {
                        Column {
                            Text(ownerCampsitesError)

                            Button(onClick = { ownerListRetry++ }) {
                                Text("Retry")
                            }
                        }
                    }

                    else -> {
                        val campsite = ownerCampsites.find {
                            it.id == campsiteId
                        }

                    var bookedReservations by remember(campsite?.id) {
                        mutableStateOf<List<Reservation>>(emptyList())
                    }

                    var isLoadingBookings by remember(campsite?.id) {
                        mutableStateOf(true)
                    }

                    var bookingsError by remember(campsite?.id) {
                        mutableStateOf("")
                    }


                    if (campsite != null) {
                        DisposableEffect(campsite.id, ownerBookingsRepository) {
                            isLoadingBookings = true
                            bookingsError = ""

                            val listener = ownerBookingsRepository.listenToBookings(
                                campsiteId = campsite.id,
                                onSuccess = { reservations ->
                                    bookedReservations = reservations
                                    isLoadingBookings = false
                                    bookingsError = ""
                                },
                                onFailure = { exception ->
                                    Log.e(
                                        "OwnerBookings",
                                        "Could not load reservations",
                                        exception
                                    )

                                    bookingsError =
                                        "Could not load reservations."
                                    isLoadingBookings = false
                                }
                            )

                            onDispose {
                                listener.remove()
                            }
                        }
                        DisposableEffect(campsite.id, availabilityRetry) {
                            var active = true
                        var availabilityDays by remember(campsite?.id) {
                            mutableStateOf<List<AvailabilityDay>>(emptyList())
                        }
                        var isLoadingAvailability by remember(campsite?.id) {
                            mutableStateOf(true)
                        }
                        var availabilityError by remember(campsite?.id) {
                            mutableStateOf("")
                        }
                        var availabilityRetry by remember(campsite?.id) {
                            mutableStateOf(0)
                        }

                        var lotteries by remember(profile.id, campsite?.id) {
                            mutableStateOf<List<Lottery>>(emptyList())
                        }

                        var isLoadingLotteries by remember(profile.id, campsite?.id) {
                            mutableStateOf(true)
                        }

                        var lotteriesError by remember(profile.id, campsite?.id) {
                            mutableStateOf("")
                        }

                        var lotteriesRetry by remember(profile.id, campsite?.id) {
                            mutableStateOf(0)
                        }

                        if (campsite != null) {
                            DisposableEffect(campsite.id, availabilityRetry) {
                                var active = true

                                isLoadingAvailability = true
                                availabilityError = ""

                                campsiteRepository.getAvailability(
                                    campsiteId = campsite.id,
                                    onSuccess = { days ->
                                        if (active) {
                                            availabilityDays = days
                                            isLoadingAvailability = false
                                        }
                                    },
                                    onFailure = { exception ->
                                        if (active) {
                                            Log.e(
                                                "CampsiteAvailability",
                                                "Can not load availability",
                                                exception
                                            )

                                            availabilityDays = emptyList()
                                            availabilityError =
                                                "Can not load availability. Please try again."
                                            isLoadingAvailability = false
                                        }
                                    }
                                )

                                onDispose { active = false }
                            }

                            DisposableEffect(
                                profile.id,
                                campsite.id,
                                lotteriesRetry,
                                currentScreenEntry
                            ) {
                                var active = true

                                // Reload when returning to this campsite's details.
                                if (currentScreenEntry == entry) {
                                    isLoadingLotteries = true
                                    lotteriesError = ""

                                    lotteryRepository.getOwnerCampsiteLotteries(
                                        ownerId = profile.id,
                                        campsiteId = campsite.id,
                                        onSuccess = { savedLotteries ->
                                            if (active) {
                                                lotteries = savedLotteries
                                                isLoadingLotteries = false
                                            }
                                        },
                                        onFailure = { exception ->
                                            if (active) {
                                                Log.e(
                                                    "CampsiteLotteries",
                                                    "Can not load campsite lotteries",
                                                    exception
                                                )

                                                lotteries = emptyList()
                                                lotteriesError =
                                                    "Can not load lotteries. Please try again."
                                                isLoadingLotteries = false
                                            }
                                        }
                                    )
                                }

                    OwnerCampsiteDetailsScreen(
                        campsite = campsite,
                        availabilityDays = availabilityDays,
                        isLoadingAvailability = isLoadingAvailability,
                        availabilityError = availabilityError,
                        lotteries = lotteries,
                        isLoadingLotteries = isLoadingLotteries,
                        lotteriesError = lotteriesError,
                        onRetryLotteries = { lotteriesRetry++ },
                        onRetryAvailability = { availabilityRetry++ },
                        bookedReservations = bookedReservations,
                        isLoadingBookings = isLoadingBookings,
                        bookingsError = bookingsError,

                        onCreateLotteryClick = {
                            if (campsite != null) {
                                navController.navigate(
                                    "createLottery/${Uri.encode(campsite.id)}"
                                )
                                // Ignore results after this request's effect is disposed.
                                onDispose { active = false }
                            }
                        },
                        onUpdateWaitlistCapacity = {
                                lotteryId, newCapacity, onSuccess, onFailure ->

                            waitlistRepository.updateWaitlistCapacity(
                                lotteryId = lotteryId,
                                ownerId = profile.id,
                                newCapacity = newCapacity,
                                onSuccess = {
                                    lotteries = lotteries.map { lottery ->
                                        if (lottery.id == lotteryId) {
                                            lottery.copy(waitlistCapacity = newCapacity)
                                        } else {
                                            lottery
                                        }
                                    }

                                    onSuccess()
                                },
                                onFailure = { exception ->
                                    Log.e(
                                        "WaitlistCapacity",
                                        "Could not update capacity",
                                        exception
                                    )

                                    onFailure(
                                        exception.message ?: "Could not update capacity."
                                    )
                                }
                            )
                        },

                        onLoadLotteryEntrants = {
                                lotteryId, onSuccess, onFailure ->

                            lotteryEntrantsRepository.getEntrants(
                                lotteryId = lotteryId,
                                ownerId = profile.id,
                                onSuccess = onSuccess,
                                onFailure = { exception ->
                                    Log.e(
                                        "LotteryEntrants",
                                        "Could not load lottery entrants",
                                        exception
                                    )
                                    onFailure(
                                        exception.message ?: "Could not load entrants."
                                    )
                                }
                            )
                        }


                    )
                        OwnerCampsiteDetailsScreen(
                            campsite = campsite,
                            availabilityDays = availabilityDays,
                            isLoadingAvailability = isLoadingAvailability,
                            availabilityError = availabilityError,
                            lotteries = lotteries,
                            isLoadingLotteries = isLoadingLotteries,
                            lotteriesError = lotteriesError,
                            onRetryLotteries = { lotteriesRetry++ },
                            onRetryAvailability = { availabilityRetry++ },
                            onCreateLotteryClick = {
                                if (campsite != null) {
                                    navController.navigate(
                                        "createLottery/${Uri.encode(campsite.id)}"
                                    )
                                }
                            }
                        )
                    }
                }
            }

            composable("createLottery/{campsiteId}") { entry ->
                val campsiteId = entry.arguments?.getString("campsiteId")

                val campsite = ownerCampsites.find {
                    it.id == campsiteId &&
                            it.ownerId == profile.id &&
                            !it.removed
                }

                when {
                    !profile.isOwner || profile.removed -> {
                        Text("Only campsite owners can create lotteries.")
                    }

                    isLoadingOwnerCampsites -> {
                        CircularProgressIndicator()
                    }

                    ownerCampsitesError.isNotEmpty() -> {
                        Column {
                            Text(ownerCampsitesError)

                            Button(onClick = { ownerListRetry++ }) {
                                Text("Retry")
                            }
                        }
                    }

                    campsite == null -> {
                        Text("Campsite not found.")
                    }

                    else -> {
                        val lotteryId = rememberSaveable(profile.id, campsite.id) {
                            lotteryRepository.generateLotteryId()
                        }

                        var isSaving by remember(campsite.id) {
                            mutableStateOf(false)
                        }

                        var errorMessage by remember(campsite.id) {
                            mutableStateOf("")
                        }

                        CreateLotteryScreen(
                            campsite = campsite,
                            lotteryId = lotteryId,
                            isSaving = isSaving,
                            errorMessage = errorMessage,
                            onCreateLottery = { lottery ->
                                if (!isSaving) {
                                    isSaving = true
                                    errorMessage = ""

                                    lotteryRepository.createLottery(
                                        lottery = lottery,
                                        onSuccess = {
                                            isSaving = false

                                            if (navController.currentBackStackEntry == entry) {
                                                navController.popBackStack()
                                            }
                                        },
                                        onFailure = { exception ->
                                            Log.e(
                                                "LotteryCreation",
                                                "Cannot publish lottery",
                                                exception
                                            )

                                            isSaving = false
                                            errorMessage =
                                                if (exception is IllegalArgumentException) {
                                                    exception.message
                                                        ?: "Please check the lottery details."
                                                } else {
                                                    "Cannot publish lottery. Please try again."
                                                }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }

            composable("createCampsite") { entry ->
                if (!profile.isOwner || profile.removed) {
                    Text("Only campsite owners can register a campsite.")
                } else {
                    val campsiteId = rememberSaveable(profile.id) {
                        campsiteRepository.generateCampsiteId()
                    }

                    var isSaving by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf("") }

                    CreateCampsiteScreen(
                        ownerId = profile.id,
                        campsiteId = campsiteId,
                        isSaving = isSaving,
                        errorMessage = errorMessage,
                        onCreateCampsite = { campsite, availabilityDays, imageUri ->
                            if (!isSaving) {
                                isSaving = true
                                errorMessage = ""

                                campsiteRepository.createCampsite(
                                    campsite = campsite,
                                    availabilityDays = availabilityDays,
                                    imageUri = imageUri,
                                    onSuccess = {
                                        isSaving = false

                                        if (navController.currentBackStackEntry == entry) {
                                            navController.popBackStack()
                                        }
                                    },
                                    onFailure = { exception ->
                                        Log.e("CampsiteCreation", "Can not create campsite", exception)

                                        isSaving = false
                                        errorMessage =
                                            if (exception is IllegalArgumentException) {
                                                exception.message
                                                    ?: "Please check the campsite details."
                                            } else {
                                                "Can not create campsite. Please try again."
                                            }
                                    }
                                )
                            }
                        }
                    )
                }

            }
        }
    }
}