package com.example.livemap

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import coil.compose.AsyncImage
import com.example.livemap.composables.DateTimePickerModal
import com.example.livemap.composables.EventInfoField
import com.example.livemap.composables.SearchResultField
import com.example.livemap.composables.SimpleSearchBar
import com.example.livemap.data.model.Event
import com.example.livemap.data.model.User
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
    viewModel: EventDetailViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EventDetailViewModel(eventId) as T
            }
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            is EventDetailState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is EventDetailState.Error -> Text(s.message, modifier = Modifier.align(Alignment.Center))
            is EventDetailState.Loaded -> EventDetailContent(
                event = s.event,
                allUsers = allUsers,
                friends = friends,
                isOwner = s.isOwner,
                isJoined = s.isJoined,
                onBack = onBack,
                onToggleJoin = viewModel::toggleJoin,
                onSave = viewModel::updateEvent,
                onDelete = { viewModel.deleteEvent(onSuccess = onBack) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailContent(
    event: Event,
    allUsers: List<User>,
    friends: List<User>,
    isOwner: Boolean,
    isJoined: Boolean,
    onBack: () -> Unit,
    onToggleJoin: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Editable state
    var name by remember(event) { mutableStateOf(event.name) }
    var description by remember(event) { mutableStateOf(event.description) }
    var locationText by remember(event) { mutableStateOf(event.locationText) }
    var dateTime by remember(event) { mutableStateOf(event.dateTime) }
    var limitPeople by remember(event) { mutableStateOf(event.limitPeople.toString()) }
    var isPublic by remember(event) { mutableStateOf(event.isPublic) }
    var participantIds by remember(event) { mutableStateOf(event.participantIds) }

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
            onChange = { locationText = it },
            isEditing = isEditing,
            fontSize = 16.sp,
            font = Font(R.font.lexend_light, FontWeight.Normal),
            leadingIconPainter = painterResource(R.drawable.location_on)
        )

        Spacer(Modifier.height(8.dp))

        // People Limit
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.group), contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("${participantIds.size} / ", fontSize = 16.sp)
            if (isEditing) {
                OutlinedTextField(
                    value = limitPeople,
                    onValueChange = { if (it.all { char -> char.isDigit() }) limitPeople = it },
                    modifier = Modifier.width(60.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
            } else {
                Text(if (limitPeople.toIntOrNull() == 0) "-" else limitPeople, fontSize = 16.sp)
            }
            Text(" joined", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Participants / Invite Friends
        Text("Participants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        if (isEditing && isOwner) {
            var searchFriendQuery by remember { mutableStateOf("") }
            val searchBarState = remember { androidx.compose.foundation.text.input.TextFieldState(searchFriendQuery) }
            val availableFriends = friends.filter { it.uid !in participantIds }

            SimpleSearchBar(
                label = "Invite friends",
                textFieldState = searchBarState,
                onSearch = { searchFriendQuery = it },
                searchResults = availableFriends.map { it.displayName },
                onFriendClicked = { name ->
                    val user = availableFriends.find { it.displayName == name }
                    if (user != null) {
                        val limit = limitPeople.toIntOrNull() ?: 0
                        if (limit == 0 || participantIds.size < limit) {
                            participantIds = participantIds + user.uid
                        } else {
                            Toast.makeText(context, "People limit reached", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            // Show owner first
            val owner = allUsers.find { it.uid == event.ownerId }
            SearchResultField(
                valor = (owner?.displayName ?: "Unknown") + " (Owner)",
                isEditing = false,
                onRemoveFriendClicked = {}
            )

            // Show participants
            participantIds.forEach { uid ->
                if (uid != event.ownerId) {
                    val user = allUsers.find { it.uid == uid }
                    SearchResultField(
                        valor = user?.displayName ?: "Unknown",
                        isEditing = isEditing && isOwner,
                        onRemoveFriendClicked = { _ ->
                            participantIds = participantIds - uid
                        }
                    )
                }
            }
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
                            onSave(event.copy(
                                name = name,
                                description = description,
                                locationText = locationText,
                                dateTime = dateTime,
                                limitPeople = limitPeople.toIntOrNull() ?: 0,
                                isPublic = isPublic,
                                participantIds = participantIds
                            ))
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
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Event")
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this event? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
