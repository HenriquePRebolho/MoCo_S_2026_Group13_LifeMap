package com.example.livemap

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.automirrored.filled.Label
import com.example.livemap.R
import com.example.livemap.aux_files.event_types
import com.example.livemap.composables.DateTimePickerModal
import com.example.livemap.composables.SimpleSearchBar
import com.example.livemap.data.model.Event
import com.example.livemap.data.model.User
import com.example.livemap.ui.events.EventDetailState
import com.example.livemap.ui.events.EventDetailViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

/* ---------- Peach + Sage + Honey palette (Events reference) ---------- */
private val Peach        = Color(0xFFFFD4B8)
private val Sage         = Color(0xFFC8D5B0)
private val Honey        = Color(0xFFF0DDB0)
private val Sand         = Color(0xFFE8D0C5)
private val SageDark     = Color(0xFF8FA968)
private val JoinBrown    = Color(0xFFB07A4D)

private val ScreenBg     = Color(0xFFFBF6EE)
private val ChipBg       = Color(0xFFEAE5D6)
private val DarkText     = Color(0xFF3D4A2A)
private val BodyText     = Color(0xFF5C3522)
private val MutedText    = Color(0xFF8B5E47)
private val SageText     = Color(0xFF6B7855)

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
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        when (val s = state) {
            is EventDetailState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = SageDark)
            is EventDetailState.Error -> Text(s.message, modifier = Modifier.align(Alignment.Center), color = DarkText)
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
                onPickLocation = onPickLocation,
                pickedLocation = pickedLocation,
                onPickedLocationConsumed = onPickedLocationConsumed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EventDetailContent(
    event: Event,
    allUsers: List<User>,
    friends: List<User>,
    isOwner: Boolean,
    isJoined: Boolean,
    onBack: () -> Unit,
    onToggleJoin: () -> Unit,
    onSave: (Event, Double?, Double?) -> Unit,
    onPickLocation: () -> Unit = {},
    pickedLocation: PickedLocation? = null,
    onPickedLocationConsumed: () -> Unit = {}
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
    var tags by remember(event) { mutableStateOf(event.tags) }
    
    val tagSearchBarState = remember { TextFieldState("") }
    val tagOptions = remember(tags) { event_types.filter { it !in tags }.sorted() }

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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.back), contentDescription = "Back", tint = DarkText)
            }
            Text("Event Details", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DarkText)
            Spacer(Modifier.weight(1f))
            if (isOwner && !isEditing) {
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SageDark)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name
                if (isEditing) {
                    DetailEditField("Event Name", name, onChange = { name = it }, icon = Icons.Default.Edit)
                } else {
                    Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DarkText)
                }

                // Description
                if (isEditing) {
                    DetailEditField("Description", description, onChange = { description = it }, icon = Icons.Default.Description)
                } else if (description.isNotBlank()) {
                    Text(description, fontSize = 15.sp, color = BodyText)
                }

                // Date & Time
                var showPicker by remember { mutableStateOf(false) }
                val displayDate = dateTime?.toDate()?.let { 
                    SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(it) 
                } ?: "TBD"

                if (isEditing) {
                    DetailClickField("Date & Time", displayDate, icon = R.drawable.schedule) {
                        showPicker = true
                    }
                } else {
                    DetailInfoRow(R.drawable.schedule, displayDate)
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
                val limitInt = limitPeople.toIntOrNull() ?: 0
                if (isEditing) {
                    DetailEditField(
                        "People Limit (0 for no limit)", 
                        limitPeople, 
                        onChange = { limitPeople = it.filter { c -> c.isDigit() } }, 
                        icon = R.drawable.group,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                } else {
                    DetailInfoRow(R.drawable.group, "${participantIds.size} / ${if (limitInt == 0) "∞" else limitInt} joined")
                }

                // Privacy
                if (isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isPublic) painterResource(R.drawable.language) else imageVectorPainter(Icons.Default.Lock),
                                contentDescription = null,
                                tint = SageDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isPublic) "Public Event" else "Private Event", color = BodyText, fontSize = 15.sp)
                        }
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SageDark,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = ChipBg
                            )
                        )
                    }
                } else {
                    DetailInfoRow(
                        if (isPublic) R.drawable.language else Icons.Default.Lock,
                        if (isPublic) "Public Event" else "Private Event"
                    )
                }
            }
        }

        // Tags
        Text("Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
        if (isEditing) {
            SimpleSearchBar(
                label = "Add category...",
                textFieldState = tagSearchBarState,
                onSearch = { },
                searchResults = tagOptions,
                onFriendClicked = { tag -> tags = tags + tag }
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Sand,
                    modifier = if (isEditing) Modifier.clickable { tags = tags - tag } else Modifier
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag, fontSize = 13.sp, color = BodyText, fontWeight = FontWeight.SemiBold)
                        if (isEditing) {
                            Spacer(Modifier.width(4.dp))
                            Icon(painterResource(R.drawable.cancel), contentDescription = null, modifier = Modifier.size(14.dp), tint = MutedText)
                        }
                    }
                }
            }
        }

        // Participants
        Text("Participants", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
        
        if (isEditing && isOwner) {
            val friendSearchBarState = remember { TextFieldState("") }
            val availableFriends = friends.filter { it.uid !in participantIds }

            SimpleSearchBar(
                label = "Invite friends...",
                textFieldState = friendSearchBarState,
                onSearch = { },
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

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Owner
            val owner = allUsers.find { it.uid == event.ownerId }
            ParticipantItem(owner?.displayName ?: "Unknown", isOwner = true)

            // Others
            participantIds.filter { it != event.ownerId }.forEach { uid ->
                val user = allUsers.find { it.uid == uid }
                ParticipantItem(
                    name = user?.displayName ?: "Unknown",
                    isOwner = false,
                    onRemove = if (isEditing && isOwner) { { participantIds = participantIds - uid } } else null
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isOwner) {
            if (isEditing) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SageDark),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SageDark)
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
                                    isPublic = isPublic,
                                    participantIds = participantIds,
                                    tags = tags
                                ),
                                locationLat,
                                locationLng
                            )
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SageDark)
                    ) {
                        Text("Save")
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete Event")
                }
            } else {
                // Button for non-editing owner if needed (already have edit icon)
            }
        } else {
            Button(
                onClick = onToggleJoin,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isJoined) Color(0xFFE57373) else JoinBrown,
                    contentColor = Color.White
                )
            ) {
                Text(if (isJoined) "Leave Event" else "Join Event", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(40.dp))
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
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
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

@Composable
private fun DetailInfoRow(icon: Any, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (icon is Int) painterResource(icon) else imageVectorPainter(icon as ImageVector),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MutedText
        )
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 16.sp, color = BodyText)
    }
}

@Composable
private fun DetailEditField(label: String, value: String, onChange: (String) -> Unit, icon: Any, keyboardOptions: KeyboardOptions = KeyboardOptions.Default) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = SageText, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                val mod = Modifier.size(18.dp)
                if (icon is Int) Icon(painterResource(icon), null, modifier = mod, tint = MutedText)
                else Icon(icon as ImageVector, null, modifier = mod, tint = MutedText)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SageDark,
                unfocusedBorderColor = ChipBg,
                focusedContainerColor = Color(0xFFF9F9F9),
                unfocusedContainerColor = Color(0xFFF9F9F9)
            ),
            keyboardOptions = keyboardOptions,
            singleLine = true
        )
    }
}

@Composable
private fun DetailClickField(label: String, value: String, icon: Any, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = SageText, fontWeight = FontWeight.Medium)
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF9F9F9),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChipBg)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val mod = Modifier.size(18.dp)
                if (icon is Int) Icon(painterResource(icon), null, modifier = mod, tint = MutedText)
                else Icon(icon as ImageVector, null, modifier = mod, tint = MutedText)
                Spacer(Modifier.width(12.dp))
                Text(if (value.isEmpty()) "Select..." else value, color = if (value.isEmpty()) MutedText else DarkText, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ParticipantItem(name: String, isOwner: Boolean, onRemove: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(if (isOwner) Peach else Sage),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold, color = BodyText)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, color = DarkText, fontWeight = FontWeight.Medium)
                    if (isOwner) Text("Organizer", fontSize = 11.sp, color = SageDark, fontWeight = FontWeight.Bold)
                }
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(painterResource(R.drawable.cancel), contentDescription = null, tint = MutedText)
                }
            }
        }
    }
}

@Composable
private fun imageVectorPainter(imageVector: ImageVector) = androidx.compose.ui.graphics.vector.rememberVectorPainter(imageVector)
