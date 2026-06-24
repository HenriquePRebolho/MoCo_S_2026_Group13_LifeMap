package com.example.livemap

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livemap.aux_files.event_types
import com.example.livemap.composables.DateTimePickerModal
import com.example.livemap.composables.SimpleSearchBar
import com.example.livemap.ui.events.NewEventState
import com.example.livemap.ui.events.NewEventViewModel
import com.example.livemap.ui.events.PickedLocation
import com.example.livemap.ui.theme.LiveMapTheme
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date


/* ---------- Peach + Sage + Honey palette (Events reference) ---------- */
private val Peach        = Color(0xFFFFD4B8)
private val Sage         = Color(0xFFC8D5B0)
private val SageDark     = Color(0xFF8FA968)
private val JoinBrown    = Color(0xFFB07A4D)
private val Sand         = Color(0xFFE8D0C5)

private val ScreenBg     = Color(0xFFFBF6EE)
private val ChipBg       = Color(0xFFEAE5D6)
private val DarkText     = Color(0xFF3D4A2A)
private val BodyText     = Color(0xFF5C3522)
private val MutedText    = Color(0xFF8B5E47)
private val SageText     = Color(0xFF6B7855)

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScreen(
    newEventViewModel: NewEventViewModel = viewModel(),
    onPickLocation: () -> Unit = {},
    pickedLocation: PickedLocation? = null,
    onPickedLocationConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by newEventViewModel.state.collectAsState()

    // Form state is delegated to the ViewModel so it survives navigating to the
    // map location picker and back (a disposed composition loses plain remember).
    var eventName by newEventViewModel.eventName
    var eventDescription by newEventViewModel.eventDescription
    var eventLocation by newEventViewModel.eventLocation
    var selectedDateTime by newEventViewModel.selectedDateTime
    var peopleLimit by newEventViewModel.peopleLimit
    var isPublic by newEventViewModel.isPublic
    var addedEvents by newEventViewModel.addedEvents
    var addedFriendIds by newEventViewModel.addedFriendIds

    // Coordinates resolved from the map picker.
    var eventLat by newEventViewModel.eventLat
    var eventLng by newEventViewModel.eventLng

    // When the picker returns a result, fill the field + store coordinates.
    LaunchedEffect(pickedLocation) {
        pickedLocation?.let {
            eventLocation = it.address
            eventLat = it.lat
            eventLng = it.lng
            onPickedLocationConsumed()
        }
    }

    val peopleLimitInt = peopleLimit.toIntOrNull() ?: 0

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create Event",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NewEventTextField("Event Name", eventName, onChange = { eventName = it }, icon = Icons.Default.Edit)
                    NewEventTextField("Event Description", eventDescription, onChange = { eventDescription = it }, icon = Icons.Default.Description)
                    
                    Column {
                        NewEventTextField(
                            "Event Location", 
                            eventLocation, 
                            onChange = { 
                                eventLocation = it
                                // Manual edit invalidates the picked coordinates.
                                eventLat = null
                                eventLng = null
                            }, 
                            icon = R.drawable.location_on
                        )
                        TextButton(
                            onClick = onPickLocation,
                            modifier = Modifier.padding(start = 32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.location_on),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = SageDark
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Select on map", color = SageDark, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    var showPicker by remember { mutableStateOf(false) }
                    val displayDateTime = selectedDateTime?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm")) ?: ""
                    NewEventClickField("Date & Time", displayDateTime, icon = R.drawable.schedule) {
                        showPicker = true
                    }

                    if (showPicker) {
                        DateTimePickerModal(
                            onDateTimeSelected = { dateTime ->
                                selectedDateTime = dateTime
                                showPicker = false
                            },
                            onDismiss = { showPicker = false }
                        )
                    }

                    NewEventTextField(
                        label = "People Limit (0 for no limit)",
                        value = peopleLimit,
                        onChange = { text -> peopleLimit = text.filter { it.isDigit() } },
                        icon = R.drawable.group,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(if (isPublic) R.drawable.language else R.drawable.password),
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
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SageDark,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = ChipBg
                            )
                        )
                    }
                }
            }

            // TAGS
            Text("Event Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
            var eventsOptions by remember { mutableStateOf(event_types.filter { it !in addedEvents }) }
            val eventSearchBarState = remember { TextFieldState("") }

            SimpleSearchBar(
                label = "Add category...",
                textFieldState = eventSearchBarState,
                onSearch = { },
                searchResults = eventsOptions,
                onFriendClicked = { tag ->
                    addedEvents = addedEvents + tag
                    eventsOptions = eventsOptions - tag
                },
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                addedEvents.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Sand,
                        modifier = Modifier.clickable {
                            addedEvents = addedEvents - tag
                            eventsOptions = (eventsOptions + tag).sorted()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tag, fontSize = 13.sp, color = BodyText, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(painterResource(R.drawable.cancel), contentDescription = null, modifier = Modifier.size(14.dp), tint = MutedText)
                        }
                    }
                }
            }

            // INVITE FRIENDS
            Text("Invite Friends", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
            val allUsers by newEventViewModel.allUsers.collectAsState()
            val friends by newEventViewModel.friends.collectAsState()
            val searchBarState = remember { TextFieldState("") }

            val availableFriends = friends.filter { it.uid !in addedFriendIds }

            SimpleSearchBar(
                label = "Search friends...",
                textFieldState = searchBarState,
                onSearch = { },
                searchResults = availableFriends.map { it.displayName },
                onFriendClicked = { name ->
                    val user = availableFriends.find { it.displayName == name }
                    if (user != null) {
                        if (peopleLimitInt == 0 || addedFriendIds.size < peopleLimitInt) {
                            addedFriendIds = addedFriendIds + user.uid
                        } else {
                            Toast.makeText(context, "People limit reached", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                addedFriendIds.forEach { uid ->
                    val user = allUsers.find { it.uid == uid }
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
                                    modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Peach),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(user?.displayName?.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold, color = BodyText)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(user?.displayName ?: "Unknown", color = DarkText, fontWeight = FontWeight.Medium)
                            }
                            IconButton(onClick = { addedFriendIds = addedFriendIds - uid }, modifier = Modifier.size(24.dp)) {
                                Icon(painterResource(R.drawable.cancel), contentDescription = null, tint = MutedText)
                            }
                        }
                    }
                }
            }

            val conditions = eventName.isNotBlank() &&
                    eventLocation.isNotBlank() &&
                    selectedDateTime != null

            val isSubmitting = state is NewEventState.Submitting

            Button(
                onClick = {
                    val date = selectedDateTime?.let {
                        val instant = it.atZone(ZoneId.systemDefault()).toInstant()
                        Date.from(instant)
                    }
                    newEventViewModel.createEvent(
                        name = eventName,
                        description = eventDescription,
                        locationText = eventLocation,
                        dateTime = date,
                        isPublic = isPublic,
                        limitPeople = peopleLimitInt,
                        tags = addedEvents,
                        invitedFriends = addedFriendIds,
                        locationLat = eventLat,
                        locationLng = eventLng
                    )
                },
                enabled = conditions && !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JoinBrown,
                    contentColor = Color.White,
                    disabledContainerColor = ChipBg,
                    disabledContentColor = MutedText
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(text = "Create Event", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            when (val s = state) {
                is NewEventState.Success -> {
                    Toast.makeText(context, "Event created successfully!", Toast.LENGTH_SHORT).show()
                    newEventViewModel.resetState()
                    newEventViewModel.resetForm()
                }
                is NewEventState.Error -> {
                    Text(text = s.message, color = Color.Red, modifier = Modifier.padding(top = 8.dp), fontSize = 13.sp)
                }
                else -> {}
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun NewEventTextField(label: String, value: String, onChange: (String) -> Unit, icon: Any, keyboardOptions: KeyboardOptions = KeyboardOptions.Default) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = SageText, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                val modifier = Modifier.size(18.dp)
                if (icon is Int) Icon(painterResource(icon), null, modifier = modifier, tint = MutedText)
                else Icon(icon as ImageVector, null, modifier = modifier, tint = MutedText)
            },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
private fun NewEventClickField(label: String, value: String, icon: Any, onClick: () -> Unit) {
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
                val modifier = Modifier.size(18.dp)
                if (icon is Int) Icon(painterResource(icon), null, modifier = modifier, tint = MutedText)
                else Icon(icon as ImageVector, null, modifier = modifier, tint = MutedText)
                Spacer(Modifier.width(12.dp))
                Text(if (value.isEmpty()) "Select..." else value, color = if (value.isEmpty()) MutedText else DarkText, fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNewScreen() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NewScreen()
        }
    }
}
