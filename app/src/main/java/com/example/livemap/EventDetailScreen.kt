package com.example.livemap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.livemap.aux.createImageUri
import com.example.livemap.composables.DatePickerModal
import com.example.livemap.composables.DateTimePickerModal
import com.example.livemap.composables.EventInfoField
import com.example.livemap.composables.SearchResultField
import com.example.livemap.composables.SimpleSearchBar
import com.example.livemap.composables.TimePickerModal
import com.example.livemap.aux_files.event_types
import com.example.livemap.ui.theme.LiveMapTheme
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable()
fun EventDetailScreen(vm: CounterViewModel, event_id : Int) {
    var event = vm.event
    val user = vm.profile
    val isOwner : Boolean by remember { mutableStateOf(event.owner == user.name) }
    var isEditing : Boolean by remember { mutableStateOf(false) };

    var inEvent : Boolean by remember { mutableStateOf(event.participants.contains(user.name)) }


    // PHOTO //////////////////////////////////////////////////////////////////
    val context = LocalContext.current
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }

    // Launcher for Gallery (using Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                capturedImageUri = uri
            }
        }
    )

    // Launcher for Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                capturedImageUri = tempImageUri
            }
        }
    )

    // Permission Launcher for Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createImageUri(context)
                if (uri != null) {
                    tempImageUri = uri
                    cameraLauncher.launch(uri)
                }
            } else {
                Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }
    )


    Column(modifier = Modifier
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
    ) {
        // Image Selection Area
        Surface(
            enabled = isOwner && isEditing,
            onClick = { showSourceDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (capturedImageUri != null) {
                    AsyncImage(
                        model = capturedImageUri,
                        contentDescription = "Selected event image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = {
                            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT)
                                .show()
                        }
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.add_circle),
//                            contentDescription = null,
//                            modifier = Modifier.size(48.dp),
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                        Text(
//                            text = "Add Event Photo",
//                            style = MaterialTheme.typography.labelLarge,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
                        Image(painterResource(R.drawable.park), contentDescription = "Event photo", modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        if (showSourceDialog) {
            AlertDialog(
                onDismissRequest = { showSourceDialog = false },
                title = { Text("Select Image Source") },
                text = { Text("Choose how you want to add a photo for your event.") },
                confirmButton = {
                    TextButton(onClick = {
                        showSourceDialog = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("Gallery")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSourceDialog = false
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) -> {
                                val uri = createImageUri(context)
                                if (uri != null) {
                                    tempImageUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            }

                            else -> {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }) {
                        Text("Camera")
                    }
                }
            )
        }
        ///////////////////////////////////////////////////////////////////////////


        // NAME ///////////////////////////////////////////////////////////////////
        var eventName by remember { mutableStateOf(event.name) }
        EventInfoField(valor = eventName, onChange = { eventName = it }, isEditing = isOwner && isEditing, fontSize = 20.sp, font = Font(R.font.lexend_bold, FontWeight.Bold),)
        ///////////////////////////////////////////////////////////////////////////


        // DESCRIPTION ////////////////////////////////////////////////////////////
        var eventDescription by remember { mutableStateOf(event.description) }
        EventInfoField(valor = eventDescription, onChange = { eventDescription = it }, isEditing = isOwner && isEditing, fontSize = 16.sp, font = Font(R.font.lexend_medium, FontWeight.Normal),)
        ///////////////////////////////////////////////////////////////////////////


        // DATE & TIME ///////////////////////////////////////////////////////////////
        var selectedDateText by remember { mutableStateOf(event.date) }
        var showDatePicker by remember { mutableStateOf(false) }

        var selectedTimeText by remember { mutableStateOf("") }
        var showTimePicker by remember { mutableStateOf(false) }


        var showPicker by remember { mutableStateOf(false) }
        var selectedDateTime by remember { mutableStateOf<LocalDateTime?>(null) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // DATE ///////////////////////////////////////////////////////////////////
            val dateWidth = if (isEditing && isOwner)  0.47f else 0.35f
            EventInfoField(
                valor = selectedDateText,
                onChange = { selectedDateText = it },
                isEditing = isOwner && isEditing,
                fontSize = 16.sp,
                font = Font(R.font.lexend_medium, FontWeight.Normal),
                leadingIconPainter = painterResource(R.drawable.schedule),
                width = dateWidth,
            )

            if (showPicker) {
                DateTimePickerModal(
                    onDateTimeSelected = { dateTime ->
                        selectedDateTime = dateTime
                        showPicker = false
                    },
                    onDismiss = { showPicker = false }
                )
            }

            // Conditionally show the DatePickerModal
            if (showDatePicker) {
                DatePickerModal(
                    onDateSelected = { millis ->
                        if (millis != null) {
                            selectedDateText = "isnf"//convertMillisToDateString(millis)
                        }
                    },
                    onDismiss = { showDatePicker = false }
                )
            }
            ///////////////////////////////////////////////////////////////////////////

            Text("-", modifier = Modifier.padding(horizontal=2.dp))

            // TIME ///////////////////////////////////////////////////////////////////
            EventInfoField(
                valor = event.timeStart,
                onChange = { event.timeStart = it },
                isEditing = isOwner && isEditing,
                fontSize = 16.sp,
                font = Font(R.font.lexend_medium, FontWeight.Normal),
                width = 1f,
            )

            // Conditionally show the DatePickerModal
            if (showTimePicker) {
                TimePickerModal(
                    onConfirm = { timePickerState ->
                        selectedTimeText = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                    onDismiss = { showTimePicker = false }
                )
            }
            ///////////////////////////////////////////////////////////////////////////
        }
        ///////////////////////////////////////////////////////////////////////////


        // LOCATION ///////////////////////////////////////////////////////////////
        var eventLocation by remember { mutableStateOf(event.location) }
        EventInfoField(valor = eventLocation, onChange = { eventLocation = it }, isEditing = isOwner && isEditing, fontSize = 16.sp, font = Font(R.font.lexend_light, FontWeight.Normal), leadingIconPainter = painterResource(R.drawable.location_on),)
        ///////////////////////////////////////////////////////////////////////////


        // TAGS ////////////////////////////////////////////////////////////////
        var events by remember { mutableStateOf(event_types) } ;
        var searchEventQuery by remember { mutableStateOf("") }
        var addedEvents by remember { mutableStateOf(vm.event.tags) }
        val eventSearchBarState = remember { TextFieldState(searchEventQuery) }
        Text("Tags:", modifier = Modifier.padding(top=4.dp))
        if (isEditing && isOwner) {
            SimpleSearchBar(
                label = "Event type",
                textFieldState = eventSearchBarState,
                onSearch = { searchEventQuery = it },
                searchResults = events,
                onFriendClicked = { addedEvents = addedEvents + it; events = events - it },
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical=8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            addedEvents.forEach { event ->
                Surface(
                    shape = RoundedCornerShape(50), // Pill shape
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = event, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        if (isEditing) {
                            IconButton(onClick = { addedEvents = addedEvents - event; events = events + event }) {
                                Icon(
                                    painterResource(R.drawable.cancel),
                                    contentDescription = "Remove friend",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
        ///////////////////////////////////////////////////////////////////////////


        // PUBLIC /////////////////////////////////////////////////////////////////
        var isPublic by remember { mutableStateOf(true) }
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var text by remember { mutableStateOf(if (isPublic) "Public" else "Private")}
            if (isOwner && isEditing) {
                Text("Public", modifier = Modifier.padding(end = 8.dp))
                Switch(checked = isPublic, onCheckedChange = { isPublic = it })
            } else {
                Text(text, modifier = Modifier.padding(end = 8.dp))
            }
        }
        ///////////////////////////////////////////////////////////////////////////


        // JOINED ////////////////////////////////////////////////////////////////
        var joined by remember { mutableIntStateOf(event.participants.size) }
        var peopleLimit by remember { mutableStateOf("") }
        peopleLimit = if (event.limitPeople.toString() == "") "-" else (event.limitPeople.toString())
        val peopleLimitWidth = if (isEditing && isOwner)  0.3f else 0.07f
        val peopleLimitPadding = if (isEditing && isOwner) 8.dp else 0.dp
        Row(verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.group), contentDescription = "")
            Text("$joined/", fontSize = 16.sp, fontWeight=FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            EventInfoField(valor = peopleLimit, onChange = { text -> peopleLimit = text.filter { it.isDigit() && (it.toInt() >= joined)} }, isEditing = isOwner && isEditing, fontSize = 16.sp, font = Font(R.font.lexend_medium, FontWeight.Normal), width = peopleLimitWidth, modifier = Modifier.padding(horizontal = peopleLimitPadding))
            Text("joined")
        }
        ///////////////////////////////////////////////////////////////////////////


        // OWNER //////////////////////////////////////////////////////////////////
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            onClick = { /*TODO: send to owner profile if owner is not user*/}
        ) {
            val owner = if (user.name == event.owner) {
                "You (Admin)"
            } else {
                vm.event.owner + " (Admin)"
            }
            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.nico),
                    contentDescription = "Profile picture",
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = owner,
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            }
        }
        //////////////////////////////////////////////////////////////////////////


        // PARTICIPANTS ///////////////////////////////////////////////////////////
        var friends by remember { mutableStateOf(vm.profile.friends) } ;
        var searchFriendQuery by remember { mutableStateOf("") }
        var addedFriends by remember { mutableStateOf(vm.event.participants) }
        val searchBarState = remember { TextFieldState(searchFriendQuery) }
        var peopleLimitInt = peopleLimit.toIntOrNull() ?: 0
        if (isEditing && isOwner) {
            SimpleSearchBar(
                label = "Invite friends",
                textFieldState = searchBarState,
                onSearch = { searchFriendQuery = it },
                searchResults = friends,
                onFriendClicked = { if (addedFriends.size < peopleLimitInt) {addedFriends = addedFriends + it; friends = friends - it;} }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            addedFriends.forEach { addedFriend ->
                SearchResultField(
                    valor = addedFriend,
                    onRemoveFriendClicked = { removedFriend ->
                        addedFriends = addedFriends - removedFriend
                        friends = (friends + removedFriend).sorted()
                        joined -= 1
                    },
                    isEditing = (isEditing && isOwner)
                )
            }
        }
        //////////////////////////////////////////////////////////////////////////





        // CONDITIONS ///////////////////////////////////////////////////////////
        var conditionsOwner by remember { mutableStateOf(false) }
        conditionsOwner = (eventName.isNotEmpty() && eventLocation.isNotEmpty() && selectedDateText.isNotEmpty() &&
                selectedTimeText.isNotEmpty() && peopleLimit.isNotEmpty())
        conditionsOwner = if(peopleLimit != "")  conditionsOwner && (addedFriends.size <= peopleLimitInt) else conditionsOwner

        var conditionsUser by remember { mutableStateOf(false) }
        conditionsUser = (event.participants.contains(user.name))
        /////////////////////////////////////////////////////////////////////////


        if (isOwner) {
            if (isEditing) {
                Button(onClick = {isEditing = false}, modifier = Modifier.fillMaxWidth(),) {
                    Text("Discard changes")
                }
                Button(onClick = {isEditing = true}, modifier = Modifier.fillMaxWidth(),) {
                    Text("Save changes")
                }
            } else {
                Button(onClick = {isEditing = true}, modifier = Modifier.fillMaxWidth(),) {
                    Text("Edit")
                }
            }
        } else {
            Button(onClick = {inEvent = !inEvent; if(inEvent) addedFriends.filter { person -> person != user.name }}, modifier = Modifier.fillMaxWidth(),) {
                if (inEvent) {
                    Text(text = "Leave")
                } else {
                    Text(text = "Join")
                }
            }
        }
    }
}




@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PreviewEventDetailScreen() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EventDetailScreen(vm = CounterViewModel(), 0)
        }
    }
}