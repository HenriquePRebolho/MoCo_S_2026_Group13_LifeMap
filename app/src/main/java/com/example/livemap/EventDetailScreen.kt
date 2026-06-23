package com.example.livemap

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livemap.composables.DateTimePickerModal
import com.example.livemap.composables.EventInfoField
import com.example.livemap.data.model.Event
import com.example.livemap.ui.events.EventDetailState
import com.example.livemap.ui.events.EventDetailViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onPickLocation: () -> Unit = {},
    pickedLocation: PickedLocation? = null,
    onPickedLocationConsumed: () -> Unit = {},
    viewModel: EventDetailViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EventDetailViewModel(eventId) as T
            }
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            is EventDetailState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is EventDetailState.Error -> Text(s.message, modifier = Modifier.align(Alignment.Center))
            is EventDetailState.Loaded -> EventDetailContent(
                event = s.event,
                isOwner = s.isOwner,
                isJoined = s.isJoined,
                onBack = onBack,
                onToggleJoin = viewModel::toggleJoin,
                onSave = viewModel::updateEvent,
                onPickLocation = onPickLocation,
                pickedLocation = pickedLocation,
                onPickedLocationConsumed = onPickedLocationConsumed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailContent(
    event: Event,
    isOwner: Boolean,
    isJoined: Boolean,
    onBack: () -> Unit,
    onToggleJoin: () -> Unit,
    onSave: (Event, Double?, Double?) -> Unit,
    onPickLocation: () -> Unit = {},
    pickedLocation: PickedLocation? = null,
    onPickedLocationConsumed: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }

    // Editable state
    var name by remember(event) { mutableStateOf(event.name) }
    var description by remember(event) { mutableStateOf(event.description) }
    var locationText by remember(event) { mutableStateOf(event.locationText) }
    var dateTime by remember(event) { mutableStateOf(event.dateTime) }
    var limitPeople by remember(event) { mutableStateOf(event.limitPeople.toString()) }
    var isPublic by remember(event) { mutableStateOf(event.isPublic) }

    // Coordinates resolved from the map picker. Null means "not picked": on save
    // the address text is geocoded (if it changed) or the existing coords kept.
    // Cleared when the text is edited manually so it gets re-validated.
    var locationLat by remember(event) { mutableStateOf<Double?>(null) }
    var locationLng by remember(event) { mutableStateOf<Double?>(null) }

    // When the picker returns a result, fill the address + store coordinates.
    LaunchedEffect(pickedLocation) {
        pickedLocation?.let {
            locationText = it.address
            locationLat = it.lat
            locationLng = it.lng
            onPickedLocationConsumed()
        }
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.back), contentDescription = "Back")
            }
            Text("Event Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        // Placeholder for Photo
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Image(
                painter = painterResource(R.drawable.park),
                contentDescription = "Event photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(16.dp))

        EventInfoField(
            valor = name,
            onChange = { name = it },
            isEditing = isEditing,
            fontSize = 22.sp,
            font = Font(R.font.lexend_bold, FontWeight.Bold)
        )

        EventInfoField(
            valor = description,
            onChange = { description = it },
            isEditing = isEditing,
            fontSize = 16.sp,
            font = Font(R.font.lexend_medium, FontWeight.Normal)
        )

        Spacer(Modifier.height(8.dp))

        // Date & Time
        var showPicker by remember { mutableStateOf(false) }
        val displayDate = dateTime?.toDate()?.let { 
            SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(it) 
        } ?: "TBD"

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.schedule), contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            if (isEditing) {
                TextButton(onClick = { showPicker = true }) {
                    Text(displayDate)
                }
            } else {
                Text(displayDate, fontSize = 16.sp)
            }
        }

        if (showPicker) {
            DateTimePickerModal(
                onDateTimeSelected = { localDateTime ->
                    val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
                    dateTime = Timestamp(Date.from(instant))
                    showPicker = false
                },
                onDismiss = { showPicker = false }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Location
        EventInfoField(
            valor = locationText,
            onChange = {
                locationText = it
                // Manual edit invalidates the picked coordinates so the address
                // gets re-validated (geocoded) on save.
                locationLat = null
                locationLng = null
            },
            isEditing = isEditing,
            fontSize = 16.sp,
            font = Font(R.font.lexend_light, FontWeight.Normal),
            leadingIconPainter = painterResource(R.drawable.location_on)
        )

        if (isEditing) {
            TextButton(onClick = onPickLocation) {
                Icon(
                    painter = painterResource(R.drawable.location_on),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Select on map")
            }
        }

        Spacer(Modifier.height(8.dp))

        // People Limit
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.group), contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("${event.participantIds.size} / ", fontSize = 16.sp)
            if (isEditing) {
                OutlinedTextField(
                    value = limitPeople,
                    onValueChange = { if (it.all { char -> char.isDigit() }) limitPeople = it },
                    modifier = Modifier.width(60.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
            } else {
                Text(if (event.limitPeople == 0) "Unlimited" else event.limitPeople.toString(), fontSize = 16.sp)
            }
            Text(" joined", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        if (isOwner) {
            if (isEditing) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                event.copy(
                                    name = name,
                                    description = description,
                                    locationText = locationText,
                                    dateTime = dateTime,
                                    limitPeople = limitPeople.toIntOrNull() ?: 0,
                                    isPublic = isPublic
                                ),
                                locationLat,
                                locationLng
                            )
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            } else {
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Event")
                }
            }
        } else {
            Button(
                onClick = onToggleJoin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isJoined) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isJoined) "Leave Event" else "Join Event")
            }
        }
    }
}
