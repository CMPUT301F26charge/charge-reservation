
package com.charge.reservation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.charge.reservation.booking.RulesAcknowledgementScreen
import com.charge.reservation.model.Campsite
import com.charge.reservation.model.Reservation
import com.charge.reservation.model.ReservationStatus
import com.charge.reservation.owner.OwnerCampsiteDetailsScreen
import com.charge.reservation.owner.OwnerCampsiteListScreen
import com.charge.reservation.reservation.ActiveReservationsScreen
import com.charge.reservation.reservation.BookingResult
import com.charge.reservation.reservation.ReservationConfirmationScreen
import com.charge.reservation.reservation.ReservationDetailsScreen
import java.time.LocalDate


//use navcontroller to navigate to different ui displays (similar to how lab 7 works)
//this is the only function called in MainActivity
@Composable
fun CampsiteApp() {
    val navcontroller = rememberNavController()

    // ADDED: Temporary identities for checkpoint demonstration.
    val currentOwnerId = "demo-owner"
    val currentEntrantId = "demo-entrant"
    val today = LocalDate.now()

    // ADDED: Demo campsites.
    val campsites = remember {
        listOf(
            Campsite(
                id = "camp-001",
                ownerId = currentOwnerId,
                name = "Pine Lake Campground",
                location = "Banff, Alberta",
                description = "Lakeside camping with fire pits",
                maxOccupancy = 4,
                pricePerNight = 65.0,
                rules = "Quiet hours begin at 10 PM.",
                cancellationPolicy = "Cancel 48 hours in advance."
            ),
            Campsite(
                id = "camp-002",
                ownerId = currentOwnerId,
                name = "Crystal Lake Campground",
                location = "Jasper, Alberta",
                description = "Quiet forest campsite",
                maxOccupancy = 6,
                pricePerNight = 80.0,
                rules = "No open fires during fire bans.",
                cancellationPolicy = "Cancel 24 hours in advance."
            )
        )
    }

    // ADDED: In-memory demo reservations.
    // These are NOT saved to Firestore.
    val reservations = remember {
        mutableStateListOf<Reservation>()
    }

    var nextBookingNumber by remember {
        mutableIntStateOf(1)
    }

    var bookingAttemptNumber by remember {
        mutableIntStateOf(0)
    }

    var bookingResult by remember {
        mutableStateOf<BookingResult?>(null)
    }

    val campsiteNames = campsites.associate {
        it.id to it.name
    }

    // ADDED: Demo booking dates.
    val checkInDate = today.plusDays(14)
    val checkOutDate = today.plusDays(16)

    NavHost(navController = navcontroller, startDestination = "home") {
        composable("home") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeScreen(navcontroller) //home screen ui

                // ADDED: Temporary navigation for checkpoint demo.
                Text("Checkpoint Feature Demo")
                Text(
                    "Demo mode: reservations are stored in memory only."
                )

                Button(
                    onClick = {
                        navcontroller.navigate("owner_campsites")
                    }
                ) {
                    Text("My Campsites")
                }

                Button(
                    onClick = {
                        bookingAttemptNumber++
                        navcontroller.navigate("booking_rules")
                    }
                ) {
                    Text("Demo: Reserve Pine Lake")
                }

                Button(
                    onClick = {
                        navcontroller.navigate("active_reservations")
                    }
                ) {
                    Text("My Active Reservations")
                }
            }
        }

        // ADDED: Issue #32 - Owner campsite list.
        composable("owner_campsites") {
            OwnerCampsiteListScreen(
                currentOwnerId = currentOwnerId,
                campsites = campsites,
                onCampsiteClick = { campsiteId ->
                    navcontroller.navigate(
                        "owner_campsite_details/${Uri.encode(campsiteId)}"
                    )
                },
                // ADDED: Back to Home
                onBackClick = {
                    navcontroller.popBackStack()
                }
            )
        }

        // ADDED: Issue #30 - Owner campsite details.
        composable("owner_campsite_details/{campsiteId}") { entry ->
            val campsiteId =
                entry.arguments?.getString("campsiteId")

            val selectedCampsite = campsites.find {
                it.id == campsiteId &&
                        it.ownerId == currentOwnerId &&
                        !it.removed
            }

            OwnerCampsiteDetailsScreen(
                campsite = selectedCampsite,
                onBackClick = {
                    navcontroller.popBackStack()
                }
            )
        }

        // ADDED: Issue #49 - Rules acknowledgement.
        composable("booking_rules") {
            val selectedCampsite = campsites.first {
                it.id == "camp-001"
            }

            RulesAcknowledgementScreen(
                campsite = selectedCampsite,
                bookingId = "demo-attempt-$bookingAttemptNumber",
                onConfirmClick = {
                    // ADDED: Simulate a booking operation.
                    // A real booking must be confirmed by Firestore.
                    val conflictingReservation =
                        reservations.any { reservation ->
                            reservation.campsiteId ==
                                    selectedCampsite.id &&
                                    reservation.status ==
                                    ReservationStatus.CONFIRMED &&
                                    reservation.checkInDate != null &&
                                    reservation.checkOutDate != null &&
                                    checkInDate.isBefore(
                                        reservation.checkOutDate
                                    ) &&
                                    checkOutDate.isAfter(
                                        reservation.checkInDate
                                    )
                        }

                    if (conflictingReservation) {
                        bookingResult = BookingResult.Failure(
                            "The selected dates are unavailable."
                        )
                    } else {
                        val reservation = Reservation(
                            id = "DEMO-${nextBookingNumber}",
                            campsiteId = selectedCampsite.id,
                            lotteryId = "demo-lottery",
                            entrantId = currentEntrantId,
                            checkInDate = checkInDate,
                            checkOutDate = checkOutDate,
                            guestCount = 2,
                            status = ReservationStatus.CONFIRMED
                        )

                        nextBookingNumber++

                        // Save within demo session only.
                        reservations.add(reservation)

                        bookingResult = BookingResult.Success(
                            reservation = reservation,
                            campsiteName = selectedCampsite.name
                        )
                    }

                    navcontroller.navigate("booking_result")
                },
                onBackClick = {
                    navcontroller.popBackStack()
                }
            )
        }

        // ADDED: Issue #14 - Reservation confirmation.
        composable("booking_result") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Text(
                    "DEMO ONLY: No reservation is saved to Firestore."
                )

                ReservationConfirmationScreen(
                    result = bookingResult
                        ?: BookingResult.Failure(
                            "No booking result is available."
                        ),
                    onDoneClick = {
                        navcontroller.navigate("active_reservations") {
                            popUpTo("home")
                        }
                    }
                )
            }
        }

        // ADDED: Issue #16 - Active reservations.
        composable("active_reservations") {
            ActiveReservationsScreen(
                entrantId = currentEntrantId,
                reservations = reservations,
                campsiteNames = campsiteNames,
                onReservationClick = { reservationId ->
                    navcontroller.navigate(
                        "reservation_details/${Uri.encode(reservationId)}"
                    )
                },
                // ADDED: Back to Home
                onBackClick = {
                    navcontroller.popBackStack()
                }
            )
        }

        // ADDED: Issue #16 - Reservation details.
        composable("reservation_details/{reservationId}") { entry ->
            val reservationId =
                entry.arguments?.getString("reservationId")

            val selectedReservation = reservations.find {
                it.id == reservationId &&
                        it.entrantId == currentEntrantId
            }

            ReservationDetailsScreen(
                reservation = selectedReservation,
                campsiteName = selectedReservation?.let {
                    campsiteNames[it.campsiteId]
                } ?: "Unknown campsite",
                onBackClick = {
                    navcontroller.popBackStack()
                }
            )
        }
    }
}
