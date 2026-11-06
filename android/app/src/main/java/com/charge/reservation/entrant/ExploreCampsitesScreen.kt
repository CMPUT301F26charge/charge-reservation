package com.charge.reservation.entrant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.charge.reservation.model.Campsite
import com.charge.reservation.repository.CampsiteRepository
import com.charge.reservation.ui.CampsiteCard
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun ExploreCampsitesScreen(
    navController: NavController,
    onCampsiteClick: (Campsite) -> Unit
) {
    val repository = remember { CampsiteRepository() }

    var campsites by remember {
        mutableStateOf<List<Campsite>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var searchText by rememberSaveable {
        mutableStateOf("")
    }

    val normalizedSearch = searchText.trim()
        .replace(Regex("\\s+"), " ")

    val filteredCampsites = campsites.filter { campsite ->
        val normalizedName = campsite.name.trim()
            .replace(Regex("\\s+"), " ")

        normalizedName.contains(normalizedSearch, ignoreCase = true)
    }

    DisposableEffect(repository) {
        val listener = repository.listenToActiveCampsites(
            onSuccess = { result ->
                campsites = result
                isLoading = false
                errorMessage = ""
            },
            onFailure = { exception ->
                errorMessage = exception.message ?: "Failed to load campsites"
                isLoading = false
            }
        )

        onDispose {
            listener.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(16.dp)
    ) {
        Text(
            text = "Explore campsites",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search campsites") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    TextButton(onClick = { searchText = "" }) {
                        Text("Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                CircularProgressIndicator()
            }

            errorMessage.isNotEmpty() -> {
                Text(errorMessage, color = Color.White)
            }

            campsites.isEmpty() -> {
                Text(
                    "No campsites available.",
                    color = Color.White
                )
            }

            filteredCampsites.isEmpty() -> {
                Text(
                    "No campsites match your search.",
                    color = Color.White
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredCampsites, key = { it.id }) { campsite ->
                        CampsiteCard(
                            campsite = campsite,
                            onClick = {
                                onCampsiteClick(campsite)
                            }
                        )
                    }
                }
            }
        }
    }
}