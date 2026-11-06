package com.charge.reservation.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.charge.reservation.R

private data class BottomTab(
    val route: String,
    val label: String,
    val iconResId: Int
)

@Composable
fun AppBottomBar(
    activeRole: String?,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val tabs = when (activeRole) {
        "Entrant" -> listOf(
            BottomTab("home", "Explore", R.drawable.ic_nav_explore),
            BottomTab("lotteries", "Lotteries", R.drawable.ic_nav_lotteries),
            BottomTab("bookings", "Bookings", R.drawable.ic_nav_bookings),
            BottomTab("profile", "Profile", R.drawable.ic_nav_profile)
        )

        "Owner" -> listOf(
            BottomTab("home", "Explore", R.drawable.ic_nav_explore),
            BottomTab("ownerCampsites", "Campsites", R.drawable.ic_nav_campsites),
            BottomTab("bookings", "Bookings", R.drawable.ic_nav_bookings),
            BottomTab("profile", "Profile", R.drawable.ic_nav_profile)
        )

        "Admin" -> listOf(
            BottomTab("home", "Explore", R.drawable.ic_nav_explore),
            BottomTab("adminCampsites", "Campsites", R.drawable.ic_nav_campsites),
            BottomTab("bookings", "Bookings", R.drawable.ic_nav_bookings),
            BottomTab("admin", "Admin", R.drawable.ic_nav_admin)
        )

        else -> emptyList()
    }

    if (tabs.isEmpty()) return

    // These screens belong to the owner's Campsites tab.
    val selectedRoute = when (currentRoute) {
        "ownerCampsiteDetails/{campsiteId}",
        "createCampsite" -> "ownerCampsites"

        "personalInfo",
        "messages",
        "policies" -> "profile"

        "notifications" -> {
            if (activeRole == "Admin") {
                "admin"
            } else {
                "profile"
            }
        }

        "adminProfiles",
        "adminImages",
        "adminNotificationLogs",
        "adminMessageLogs" -> "admin"

        else -> currentRoute
    }
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconResId),
                        contentDescription = null
                    )
                },
                label = {
                    Text(tab.label)
                }
            )
        }
    }
}