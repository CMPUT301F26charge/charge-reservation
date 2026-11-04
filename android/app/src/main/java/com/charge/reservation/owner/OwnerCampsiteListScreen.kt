
package com.charge.reservation.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charge.reservation.model.Campsite

/**
 * Displays the current owner's active campsite listings.
 *
 * Removed campsites and listings belonging to other owners
 * are excluded.
 */
@Composable
fun OwnerCampsiteListScreen(
    currentOwnerId: String,
    campsites: List<Campsite>,
    onCampsiteClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onRegisterCampsiteClick: () -> Unit
) {
    // ADDED: Use testable owner filtering logic.
    val ownerCampsites = OwnerCampsiteFilter.filter(
        campsites = campsites,
        ownerId = currentOwnerId
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ADDED: Back navigation
        Button(onClick = onBackClick) {
            Text("Back")
        }

        Text(
            text = "My Campsites",
            fontSize = 28.sp
        )

        Button(onClick = onRegisterCampsiteClick) {
            Text("Register a campsite")
        }

        if (ownerCampsites.isEmpty()) {
            Text("No registered campsites yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = ownerCampsites,
                    key = { it.id }
                ) { campsite ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCampsiteClick(campsite.id)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = campsite.name,
                                fontSize = 20.sp
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(campsite.location)

                            campsite.maxOccupancy?.let {
                                Text("Maximum occupancy: $it guests")
                            }
                        }
                    }
                }
            }
        }
    }
}
