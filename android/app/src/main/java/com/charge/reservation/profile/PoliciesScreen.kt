package com.charge.reservation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * One titled section of the policy page.
 *
 * @property title the section heading
 * @property points the rules or statements listed under the heading
 */
private data class PolicySection(
    val title: String,
    val points: List<String>
)

private val policySections = listOf(
    PolicySection(
        title = "Your profile",
        points = listOf(
            "Keep your name, email address, and phone number accurate so " +
                "campsite owners can identify you and contact you.",
            "Profile pictures must be appropriate and must not show other " +
                "people without their permission.",
            "You can delete your profile at any time from the Profile menu. " +
                "Deleting your profile cannot be undone.",
            "Administrators may remove profiles that break these guidelines. " +
                "A removed profile can no longer use the app."
        )
    ),
    PolicySection(
        title = "Lotteries",
        points = listOf(
            "You can join a lottery only while its registration is open, " +
                "and only once per lottery.",
            "You can leave a lottery at any time before the draw. After the " +
                "draw, you cannot join or leave.",
            "Winners are chosen at random from the entrants on the waiting " +
                "list when the draw takes place.",
            "If you win, you receive an invitation to book. Use it or decline " +
                "it before the booking deadline. Unused invitations expire " +
                "after the deadline.",
            "An invitation can be used for one reservation only."
        )
    ),
    PolicySection(
        title = "Reservations",
        points = listOf(
            "Your stay must fall within the lottery's permitted booking dates.",
            "Your guest count cannot be more than the campsite's available " +
                "capacity for each night of your stay.",
            "A campsite owner may reject a reservation. If this happens, " +
                "you will be notified.",
            "Cancellations follow the cancellation policy shown on each " +
                "campsite's page. Read it before booking.",
            "Follow the campsite's rules during your stay."
        )
    ),
    PolicySection(
        title = "Campsite owners",
        points = listOf(
            "Listings, photos, prices, locations, and available dates must " +
                "be accurate and kept up to date.",
            "Honour confirmed reservations. Reject or cancel a reservation " +
                "only for a genuine reason, and record that reason.",
            "Announcements sent to entrants must relate to your campsite " +
                "or lottery. Do not send advertising or spam.",
            "Use entrants' contact details only to manage their lottery " +
                "entries and reservations."
        )
    ),
    PolicySection(
        title = "Ratings",
        points = listOf(
            "You can give each campsite one rating from 1 to 5, and you can " +
                "update it later.",
            "Ratings should reflect your honest experience. Do not rate a " +
                "campsite you own."
        )
    ),
    PolicySection(
        title = "Messages and conduct",
        points = listOf(
            "Be respectful. Harassment, threats, hate speech, and explicit " +
                "content are not allowed.",
            "Do not send spam or share other people's personal information.",
            "Administrators may review messages and notifications to " +
                "investigate reports and enforce these guidelines."
        )
    ),
    PolicySection(
        title = "Notifications",
        points = listOf(
            "You can turn lottery and owner notifications on or off in " +
                "Notification settings.",
            "If notifications are off, you can still see your lottery results " +
                "in My lotteries. Check it so you don't miss a booking deadline."
        )
    ),
    PolicySection(
        title = "Your information",
        points = listOf(
            "The app stores your name, email address, phone number, profile " +
                "picture, and notification preference.",
            "When you join a lottery, the campsite owner can see your name " +
                "on the list of entrants.",
            "Your lottery entries and reservations are shared with the owner " +
                "of the campsite they relate to.",
            "Administrators can view profiles and images to keep the app safe."
        )
    )
)

/**
 * Displays the app's policies and community guidelines.
 */
@Composable
fun PoliciesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Policies and guidelines",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )

        Text(
            text = "By using the app, you agree to follow these rules.",
            style = MaterialTheme.typography.bodyMedium
        )

        policySections.forEach { section ->
            HorizontalDivider()
            PolicySectionView(section)
        }
    }
}

@Composable
private fun PolicySectionView(section: PolicySection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )

        section.points.forEach { point ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = point,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
