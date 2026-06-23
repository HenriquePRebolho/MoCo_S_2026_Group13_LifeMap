package com.example.livemap

// TODO: set fields to not be editable
// TODO: create button that allow changes, then converts into confirm and cancel button
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.livemap.aux.createImageUri
import com.example.livemap.aux_files.event_types
import com.example.livemap.aux_files.languages
import com.example.livemap.composables.EventInfoField
import com.example.livemap.composables.SimpleSearchBar
import com.example.livemap.ui.auth.AuthViewModel
import com.example.livemap.ui.profile.ProfileViewModel
import com.example.livemap.ui.theme.LiveMapTheme

// Saver for List<String> edit state so it can be kept by rememberSaveable
// (the default autoSaver can't persist an arbitrary List).
private val stringListSaver = listSaver<List<String>, String>(
    save = { it.toList() },
    restore = { it }
)

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {

    val userState by profileViewModel.user.collectAsState()

    if (userState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = userState!!

    val context = LocalContext.current
    val isOnline by observeConnectivity(context)

    // --- State for editing ---
    // rememberSaveable (not plain remember) so an in-progress edit survives a
    // bottom-tab switch and configuration changes; plain remember was discarded
    // when the screen left composition, which is why edits "erased" on navigation.
    var isEditing by rememberSaveable(user.uid) { mutableStateOf(false) }
    var name by rememberSaveable(user.uid) { mutableStateOf(user.displayName) }
    var userDescription by rememberSaveable(user.uid) { mutableStateOf(user.description) }
    var userLocation by rememberSaveable(user.uid) { mutableStateOf(user.location) }
    var userInstagram by rememberSaveable(user.uid) { mutableStateOf(user.instagram) }
    var userPhone by rememberSaveable(user.uid) { mutableStateOf(user.phone) }
    var addedHobbies by rememberSaveable(user.uid, stateSaver = stringListSaver) { mutableStateOf(user.hobbies) }
    var addedLanguages by rememberSaveable(user.uid, stateSaver = stringListSaver) { mutableStateOf(user.languages) }
    var hobbiesOptions by remember(user.uid) { mutableStateOf(event_types.filter { it !in addedHobbies }) }
    var languagesOptions by remember(user.uid) { mutableStateOf(languages.filter { it !in addedLanguages }) }
    val hobbySearchBarState = remember(user.uid) { TextFieldState("") }
    val languageSearchBarState = remember(user.uid) { TextFieldState("") }

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



    Column(
        //horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(horizontal=28.dp, vertical=16.dp).verticalScroll(rememberScrollState())
    ) {
        Column(horizontalAlignment = CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {

            // PHOTO //////////////////////////////////////////////////////////////////
            Surface(
                enabled = isEditing,
                onClick = { showSourceDialog = true },
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.background,
                shape = CircleShape
            ) {
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
                    Icon(
                        painter = painterResource(id = R.drawable.account_circle),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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


            // NAME ////////////////////////////////////////////////////////////////////
            EventInfoField(valor = name, onChange = { name = it }, isEditing = isEditing, horizontalArrangement = Arrangement.Center, font = Font(R.font.lexend_bold, FontWeight.Bold))
            ///////////////////////////////////////////////////////////////////////////


            // DESCRIPTION ////////////////////////////////////////////////////////////
            EventInfoField(valor = userDescription, onChange = { userDescription = it }, isEditing = isEditing, horizontalArrangement = Arrangement.Center)
            ///////////////////////////////////////////////////////////////////////////
        }


        // TAGS ////////////////////////////////////////////////////////////////
        Text("Hobbies", modifier = Modifier.padding(top=4.dp))
        if (isEditing) {
            SimpleSearchBar(
                label = "Hobbies",
                textFieldState = hobbySearchBarState,
                onSearch = { },
                searchResults = hobbiesOptions,
                onFriendClicked = { hobby -> 
                    addedHobbies = addedHobbies + hobby
                    hobbiesOptions = hobbiesOptions - hobby 
                },
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical=8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            addedHobbies.forEach { hobby ->
                Surface(
                    shape = RoundedCornerShape(50), // Pill shape
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = hobby, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        if (isEditing) {
                            IconButton(onClick = { 
                                addedHobbies = addedHobbies - hobby
                                if (hobby in event_types) hobbiesOptions = (hobbiesOptions + hobby).sorted()
                            }) {
                                Icon(
                                    painterResource(R.drawable.cancel),
                                    contentDescription = "Remove hobby",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
        ///////////////////////////////////////////////////////////////////////////


        // LANGUAGES //////////////////////////////////////////////////////////////
        Text("Languages", modifier = Modifier.padding(top=4.dp))
        if (isEditing) {
            SimpleSearchBar(
                label = "Languages",
                textFieldState = languageSearchBarState,
                onSearch = { },
                searchResults = languagesOptions,
                onFriendClicked = { lang -> 
                    addedLanguages = addedLanguages + lang
                    languagesOptions = languagesOptions - lang
                },
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical=8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            addedLanguages.forEach { language ->
                Surface(
                    shape = RoundedCornerShape(50), // Pill shape
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = language, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        if (isEditing) {
                            IconButton(onClick = { 
                                addedLanguages = addedLanguages - language
                                if (language in languages) languagesOptions = (languagesOptions + language).sorted()
                            }) {
                                Icon(
                                    painterResource(R.drawable.cancel),
                                    contentDescription = "Remove language",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
        ///////////////////////////////////////////////////////////////////////////


        // LOCATION ///////////////////////////////////////////////////////////////
        EventInfoField(valor = userLocation, onChange = { userLocation = it }, isEditing = isEditing, leadingIconPainter = painterResource(R.drawable.home))
        ///////////////////////////////////////////////////////////////////////////


        // BIRTHDAY ///////////////////////////////////////////////////////////////
        EventInfoField(valor = userInstagram, onChange = { userInstagram = it }, isEditing = isEditing, leadingIconPainter = painterResource(R.drawable.language))
        ///////////////////////////////////////////////////////////////////////////


        // PHONE //////////////////////////////////////////////////////////////////
        EventInfoField(valor = userPhone, onChange = { userPhone = it }, isEditing = isEditing, leadingIconPainter = painterResource(R.drawable.phone))
        ///////////////////////////////////////////////////////////////////////////


        // EMAIL //////////////////////////////////////////////////////////////////
        val userEmail = user.email
        EventInfoField(valor = userEmail, onChange = { /* Email is usually not editable this way */ }, isEditing = false, leadingIconPainter = painterResource(R.drawable.mail))
        ///////////////////////////////////////////////////////////////////////////


        // Note: Password and sensitive info are handled by Firebase Auth, not stored in Firestore User doc.


        if (isEditing) {
            Button(
                onClick = {
                    // Revert the draft back to the persisted values, then exit edit mode.
                    name = user.displayName
                    userDescription = user.description
                    userLocation = user.location
                    userInstagram = user.instagram
                    userPhone = user.phone
                    addedHobbies = user.hobbies
                    addedLanguages = user.languages
                    hobbiesOptions = event_types.filter { it !in user.hobbies }.sorted()
                    languagesOptions = languages.filter { it !in user.languages }.sorted()
                    hobbySearchBarState.edit { replace(0, length, "") }
                    languageSearchBarState.edit { replace(0, length, "") }
                    isEditing = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Discard changes")
            }
            Button(
                enabled = isOnline,
                onClick = {
                    val updates = mapOf(
                        "displayName" to name,
                        "displayNameLower" to name.lowercase(),
                        "description" to userDescription,
                        "hobbies" to addedHobbies,
                        "languages" to addedLanguages,
                        "location" to userLocation,
                        "instagram" to userInstagram,
                        "phone" to userPhone
                    )
                    // Only leave edit mode if the write actually persisted; otherwise
                    // surface the error so a rejected write isn't lost silently.
                    profileViewModel.updateProfile(updates) { result ->
                        result
                            .onSuccess {
                                Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show()
                                isEditing = false
                            }
                            .onFailure { e ->
                                Toast.makeText(
                                    context,
                                    "Couldn't save profile: ${e.localizedMessage ?: "unknown error"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isOnline) "Save changes" else "No internet connection")
            }
        } else {
            Button(
                enabled = isOnline,
                onClick = { isEditing = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isOnline) "Edit profile" else "No internet connection")
            }
        }
        ///////////////////////////////////////////////////////////////////////////

        // TEMPORARY logout button
        Button(onClick = { authViewModel.logout() }) {
            //  Icon(painterResource(R.drawable.logout), contentDescription = null)
            Text("Logout")
        }

        // Permanently deletes the user's account and all profile data.
        var showDeleteDialog by remember { mutableStateOf(false) }
        var deletePassword by remember { mutableStateOf("") }

        Button(
            onClick = { showDeleteDialog = true },
            enabled = isOnline,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isOnline) "Delete Account" else "No internet connection")
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    deletePassword = ""
                },
                title = { Text("Delete account") },
                text = {
                    Column {
                        Text("This permanently deletes your account and profile. This can't be undone. Enter your password to confirm.")
                        OutlinedTextField(
                            value = deletePassword,
                            onValueChange = { deletePassword = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        authViewModel.deleteAccount(deletePassword) { result ->
                            result
                                .onSuccess {
                                    Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { e ->
                                    Toast.makeText(
                                        context,
                                        "Couldn't delete account: ${e.localizedMessage ?: "unknown error"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        showDeleteDialog = false
                        deletePassword = ""
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        deletePassword = ""
                    }) { Text("Cancel") }
                }
            )
        }
    }
}


@Composable
fun observeConnectivity(context: Context): State<Boolean> {
    return produceState(initialValue = checkIsOnline(context)) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { value = true }
            override fun onLost(network: android.net.Network) { value = false }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

private fun checkIsOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

@Composable
fun ProfileInfoButton(field: String, value: String, svg: Int, color: Color) {
    Surface(shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 10.dp))
    {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, modifier = Modifier.padding(start = 16.dp)) {
                Icon(
                    painter = painterResource(svg),
                    tint = Color.White,
                    contentDescription = null,
                    modifier = Modifier.background(color).padding(5.dp)
                )
            }
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.Center) {
                Text(text = field, fontWeight = FontWeight.Light, modifier = Modifier.fillMaxWidth())
                Text(text = value, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable()
fun PreviewProfileScreen() {
        LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfileScreen()
        }
    }
}