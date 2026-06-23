package com.example.livemap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.livemap.aux.createImageUri
import com.example.livemap.composables.PopUpTextField
import com.example.livemap.composables.DateTimePickerModal
import com.example.livemap.composables.SearchResultField
import com.example.livemap.composables.SimpleSearchBar
import com.example.livemap.composables.TextField
import com.example.livemap.aux_files.event_types
import com.example.livemap.aux_files.localDateTimeSaver
import com.example.livemap.aux_files.stringListSaver
import com.example.livemap.ui.theme.LiveMapTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livemap.ui.events.NewEventState
import com.example.livemap.ui.events.NewEventViewModel
import com.google.firebase.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date


/** Result handed back from the map location picker (see LocationPickerScreen). */
data class PickedLocation(val lat: Double, val lng: Double, val address: String)

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScreen(
    vm: CounterViewModel,
    newEventViewModel: NewEventViewModel = viewModel(),
    onPickLocation: () -> Unit = {},
    pickedLocation: PickedLocation? = null,
    onPickedLocationConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by newEventViewModel.state.collectAsState()

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
        .verticalScroll(rememberScrollState())) {
        Text(
            text = "Create Event",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Image Selection Area
        Surface(
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
                        Icon(
                            painter = painterResource(id = R.drawable.add_circle),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add Event Photo",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        ///////////////////////////////////////////////////////


        // NAME ///////////////////////////////////////////////////////////////////
        var eventName by rememberSaveable { mutableStateOf("") }
        TextField("Event Name", eventName, onChange = { eventName = it })
        ///////////////////////////////////////////////////////////////////////////

        // EVENTS ////////////////////////////////////////////////////////////////
        var events by rememberSaveable(stateSaver = stringListSaver) { mutableStateOf(event_types) }
        var searchEventQuery by remember { mutableStateOf("") }
        var addedEvents by rememberSaveable(stateSaver = stringListSaver) { mutableStateOf(listOf<String>()) }
        val eventSearchBarState = remember { TextFieldState(searchEventQuery) }
        SimpleSearchBar(
            label = "Event type",
            textFieldState = eventSearchBarState,
            onSearch = { searchEventQuery = it },
            searchResults = events,
            onFriendClicked = { addedEvents = addedEvents + it; events = events - it },
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
                        modifier = Modifier
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = event,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
        ///////////////////////////////////////////////////////////////////////////


        // NAME ////////////////////////////////////////////////////////////
        var eventDescription by rememberSaveable { mutableStateOf("") }
        TextField("Event Description", eventDescription, onChange = { eventDescription = it })
        ////////////////////////////////////////////////////////////////////


        // LOCATION /////////////////////////////////////////////////////////
        var eventLocation by rememberSaveable { mutableStateOf("") }
        // Coordinates resolved from the map picker. Null until the user picks a
        // point; cleared when the text is edited manually so the address gets
        // re-validated (geocoded) on submit.
        var eventLat by rememberSaveable { mutableStateOf<Double?>(null) }
        var eventLng by rememberSaveable { mutableStateOf<Double?>(null) }

        // When the picker returns a result, fill the field + store coordinates.
        LaunchedEffect(pickedLocation) {
            pickedLocation?.let {
                eventLocation = it.address
                eventLat = it.lat
                eventLng = it.lng
                onPickedLocationConsumed()
            }
        }

        TextField("Event Location", eventLocation, onChange = {
            eventLocation = it
            // Manual edit invalidates the picked coordinates.
            eventLat = null
            eventLng = null
        })
        TextButton(onClick = onPickLocation) {
            Icon(
                painter = painterResource(R.drawable.location_on),
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text("Select on map")
        }
        ////////////////////////////////////////////////////////////////////


        // DATETIME ////////////////////////////////////////////////////////
        var showPicker by remember { mutableStateOf(false) }
        var selectedDateTime by rememberSaveable(stateSaver = localDateTimeSaver) { mutableStateOf<LocalDateTime?>(null) }

        // Trigger field (reuses your existing PopUpTextField)
        PopUpTextField(
            label = "Date & Time",
            valor = selectedDateTime
                ?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm"))
                ?: "",
            onClick = { showPicker = true }
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
        ////////////////////////////////////////////////////////////////////


        // PUBLIC /////////////////////////////////////////////////////////////////
        var isPublic by rememberSaveable { mutableStateOf(true) }
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var text by remember { mutableStateOf(if (isPublic) "Public" else "Private")}
            Text(text, modifier = Modifier.padding(end = 8.dp))
            Switch(checked = isPublic, onCheckedChange = { isPublic = it })
        }
        ///////////////////////////////////////////////////////////////////////////



        // PEOPLE LIMIT ////////////////////////////////////////////////////////////
        var peopleLimit by rememberSaveable { mutableStateOf("") }
        var peopleLimitInt = peopleLimit.toIntOrNull() ?: 0

        OutlinedTextField(
            value = peopleLimit,
            onValueChange = { text -> peopleLimit = text.filter { it.isDigit() } },
            label = { Text("People Limit (0 for no limit)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )
        ////////////////////////////////////////////////////////////////////////////


        // FRIENDS ////////////////////////////////////////////////////////////////
        var friends by rememberSaveable(stateSaver = stringListSaver) { mutableStateOf(vm.profile.friends) }
        var searchFriendQuery by remember { mutableStateOf("") }
        var addedFriends by rememberSaveable(stateSaver = stringListSaver) { mutableStateOf(listOf<String>()) }
        val searchBarState = remember { TextFieldState(searchFriendQuery) }

        SimpleSearchBar(
            label = "Invite friends",
            textFieldState = searchBarState,
            onSearch = { searchFriendQuery = it },
            searchResults = friends,
            onFriendClicked = {
                if (peopleLimitInt == 0 || addedFriends.size < peopleLimitInt) {
                    addedFriends = addedFriends + it
                    friends = friends - it
                } else {
                    Toast.makeText(context, "People limit reached", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            addedFriends.forEach { addedFriend ->
                SearchResultField(
                    valor = addedFriend,
                    onRemoveFriendClicked = { removedFriend ->
                        addedFriends = addedFriends - removedFriend
                        friends = (friends + removedFriend).sorted()
                    }
                )
            }
        }
        ///////////////////////////////////////////////////////////////////////////


        // CONDITIONS ///////////////////////////////////////////////////////////
        val conditions = eventName.isNotBlank() &&
                eventLocation.isNotBlank() &&
                selectedDateTime != null &&
                peopleLimit.isNotEmpty() &&
                (peopleLimitInt == 0 || addedFriends.size <= peopleLimitInt)
        /////////////////////////////////////////////////////////////////////////


        // CREATE EVENT ///////////////////////////////////////////////////////////
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
                    invitedFriends = addedFriends,
                    locationLat = eventLat,
                    locationLng = eventLng
                )
            },
            enabled = conditions && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = "Create new event", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    painter = painterResource(R.drawable.add_circle),
                    tint = Color.White,
                    contentDescription = null
                )
            }
        }

        // Handle success/error states. Done in a LaunchedEffect so the Toast
        // fires once per state change (not on every recomposition) and so an
        // invalid location ("Invalid location") is surfaced as a Toast.
        LaunchedEffect(state) {
            when (val s = state) {
                is NewEventState.Success -> {
                    Toast.makeText(context, "Event created successfully!", Toast.LENGTH_SHORT).show()
                    newEventViewModel.resetState()
                    // Clear the form for the next event.
                    eventName = ""
                    eventDescription = ""
                    eventLocation = ""
                    eventLat = null
                    eventLng = null
                    selectedDateTime = null
                    addedEvents = emptyList()
                    addedFriends = emptyList()
                    peopleLimit = ""
                    // Restore the full list of selectable event types.
                    events = event_types
                }
                is NewEventState.Error -> {
                    Toast.makeText(context, s.message, Toast.LENGTH_SHORT).show()
                    newEventViewModel.resetState()
                }
                else -> {}
            }
        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PreviewNewScreen() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NewScreen(vm = CounterViewModel())
        }
    }
}
