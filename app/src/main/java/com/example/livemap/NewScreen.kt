package com.example.livemap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.livemap.ui.theme.LiveMapTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone



@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScreen(vm: CounterViewModel) {
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
        //////////////////////////////////////////////////////

//      val participants: List<String> = listOf("User1", "User2"),
//      val tags: List<String> = listOf("Tag1", "Tag2"),

        var eventName by remember { mutableStateOf("") }
        TextField("Event Name", eventName, onChange = { eventName = it })

        // EVENTS ////////////////////////////////////////////////////////////////
        var events by remember { mutableStateOf(listOf("Football", "Reading")) } ;
        var searchEventQuery by remember { mutableStateOf("") }
        var addedEvents by remember { mutableStateOf(listOf<String>()) }
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

        var eventDescription by remember { mutableStateOf("") }
        TextField("Event Description", eventDescription, onChange = { eventDescription = it })

        var evenLocation by remember { mutableStateOf("") }
        TextField("Event Location", evenLocation, onChange = { evenLocation = it })


        // DATE ////////////////////////////////////////////////////////////
        var selectedDateText by remember { mutableStateOf("") }
        var showDatePicker by remember { mutableStateOf(false) }

        // Your text field component triggering the state change
        PopUpTextField(
            label = "Select Date",
            valor = selectedDateText,
            onClick = { showDatePicker = true }
        )

        // Conditionally show the DatePickerModal
        if (showDatePicker) {
            DatePickerModal(
                onDateSelected = { millis ->
                    if (millis != null) {
                        selectedDateText = convertMillisToDateString(millis)
                    }
                },
                onDismiss = { showDatePicker = false }
            )
        }
        ////////////////////////////////////////////////////////////////////


        // TIME ///////////////////////////////////////////////////////////

        var selectedTimeText by remember { mutableStateOf("") }
        var showTimePicker by remember { mutableStateOf(false) }

        // Your text field component triggering the state change
        PopUpTextField(
            label = "Select Time",
            valor = selectedTimeText,
            onClick = { showTimePicker = true }
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

        /////////////////////////////////////////////////////////////////



        // PEOPLE LIMIT ////////////////////////////////////////////////////////////
        var peopleLimit by remember { mutableStateOf("") }
        OutlinedTextField(
            value = peopleLimit,
            onValueChange = { text -> peopleLimit = text.filter { it.isDigit() } },
            label = { Text("People Limit") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )
        ////////////////////////////////////////////////////////////////////////////


        // PUBLIC /////////////////////////////////////////////////////////////////
        var isPublic by remember { mutableStateOf(true) }
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Public", modifier = Modifier.padding(end = 8.dp))
            Switch(checked = isPublic, onCheckedChange = { isPublic = it })
        }
        ///////////////////////////////////////////////////////////////////////////



        // FRIENDS ////////////////////////////////////////////////////////////////
        var friends by remember { mutableStateOf(vm.profile.friends) } ;
        var searchFriendQuery by remember { mutableStateOf("") }
        var addedFriends by remember { mutableStateOf(listOf<String>()) }
        val searchBarState = remember { TextFieldState(searchFriendQuery) }
        var peopleLimitInt = peopleLimit.toIntOrNull() ?: 0
        SimpleSearchBar(
            label = "Invite friends",
            textFieldState = searchBarState,
            onSearch = { searchFriendQuery = it },
            searchResults = friends,
            onFriendClicked = { if (addedFriends.size < peopleLimitInt) {addedFriends = addedFriends + it; friends = friends - it;} }
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
        var conditions by remember { mutableStateOf(false) }
        conditions = (eventName.isNotEmpty() && evenLocation.isNotEmpty() && selectedDateText.isNotEmpty() &&
                selectedTimeText.isNotEmpty() && peopleLimit.isNotEmpty() && (addedFriends.size <= peopleLimitInt))
        /////////////////////////////////////////////////////////////////////////


        // CREATE EVENT ///////////////////////////////////////////////////////////
        Button(
            onClick = { /* TODO: Implement event creation logic using capturedImageUri
                                Send: eventName, eventTypes, eventDescription, evenLocation, selectedDateText,
                                      selectedTimeText, peopleLimit, isPublic, addedFriends */
            },
            enabled = conditions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(text = "Create new event", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
                painter = painterResource(R.drawable.add_circle),
                tint = Color.White,
                contentDescription = null
            )
        }
    }
}

fun convertMillisToDateString(millis: Long): String {
    // 1. Create a Date object from the milliseconds
    val date = Date(millis)

    // 2. Define the format you want
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

    // 3. Crucial: Tell the formatter to use UTC to prevent the "off-by-one-day" bug
    formatter.timeZone = TimeZone.getTimeZone("UTC")

    // 4. Return the formatted string
    return formatter.format(date)
}



@Composable
fun TextField(label: String, valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // Configure the state to filter out past dates
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Get the current time in UTC milliseconds
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    // Reset hours, minutes, seconds to the very beginning of today
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Allow selection only if the calendar day is today or in the future
                return utcTimeMillis >= calendar.timeInMillis
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}



@Composable
fun PopUpTextField(
    label: String,
    valor: String,
    onClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = valor,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onClick()
                    focusManager.clearFocus()
                }
            },
        shape = RoundedCornerShape(12.dp)
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModal(
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentTime = Calendar.getInstance()

    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    TimePickerDialog(
        onDismiss = { onDismiss() },
        onConfirm = { onConfirm(timePickerState) }
    ) {
        TimePicker(
            state = timePickerState,
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("OK")
            }
        },
        text = { content() }
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    label: String,
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: List<String>,
    modifier: Modifier = Modifier,
    onFriendClicked: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = textFieldState.text.toString(),
            onValueChange = { text ->
                textFieldState.edit { replace(0, length, text) }
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { expanded = it.isFocused },
            label = { Text(label) },
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Icon(painterResource(R.drawable.search), contentDescription = null)
            },
            singleLine = true
        )
        if (expanded) {
            val filteredResults = searchResults.filter {
                it.contains(textFieldState.text.toString(), ignoreCase = true)
            }
            if (filteredResults.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredResults) { result ->
                            ListItem(
                                headlineContent = { Text(result) },
                                modifier = Modifier.clickable {
                                    textFieldState.edit { replace(0, length, "") }
                                    onFriendClicked(result)
                                    expanded = false
                                    onSearch(result)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SearchResultField(
    valor: String,
    onRemoveFriendClicked: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.nico), // TODO: add profile picture to user
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(
                text = valor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = { onRemoveFriendClicked(valor) }) {
                Icon(
                    painterResource(R.drawable.cancel),
                    contentDescription = "Remove friend",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}








private fun createImageUri(context: Context): Uri? {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        Toast.makeText(context, "Error creating file for photo: ${e.message}", Toast.LENGTH_SHORT).show()
        null
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
