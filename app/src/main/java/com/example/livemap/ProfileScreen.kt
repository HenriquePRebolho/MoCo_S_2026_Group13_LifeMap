package com.example.livemap

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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.livemap.R
import com.example.livemap.aux.createImageUri
import com.example.livemap.aux_files.event_types
import com.example.livemap.aux_files.languages
import com.example.livemap.aux_files.stringListSaver
import com.example.livemap.composables.EventInfoField
import com.example.livemap.composables.SimpleSearchBar
import com.example.livemap.ui.auth.AuthViewModel
import com.example.livemap.ui.profile.ProfileViewModel
import com.example.livemap.ui.theme.LiveMapTheme

// Saver for List<String> edit state so it can be kept by rememberSaveable
private val stringListSaver = listSaver<List<String>, String>(
    save = { it.toList() },
    restore = { it }
)

/* ---------- Lavender Dream palette (Friends reference) ---------- */
private val Lavender       = Color(0xFFDCC9F5)
private val SoftPink       = Color(0xFFFFD6E0)
private val LightBlue      = Color(0xFFD4E5FF)
private val PrimaryPurple  = Color(0xFF8B6BBF)
private val ScreenBg       = Color(0xFFFAF7FF)
private val ChipBg         = Color(0xFFECE5F9)
private val DarkText       = Color(0xFF3D2F5C)
private val MutedText      = Color(0xFF6F5F88)
private val SentGrey       = Color(0xFFB8AEC4)
private val ChipPurpleText = Color(0xFF8B6BBF)
private val PinkTextDark   = Color(0xFF8B2C4A)
private val BlueTextDark   = Color(0xFF1E3A5F)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val userState by profileViewModel.user.collectAsState()

    if (userState == null) {
        Box(modifier = Modifier.fillMaxSize().background(ScreenBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryPurple)
        }
        return
    }

    val user = userState!!
    val context = LocalContext.current
    val isOnline by observeConnectivity(context)

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

    // State for delete account
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) capturedImageUri = uri }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) capturedImageUri = tempImageUri }
    )

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

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("My Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DarkText)
                if (!isEditing) {
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PrimaryPurple)
                    }
                }
            }

            // Avatar & Name Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Image
                    Surface(
                        onClick = { if (isEditing) showSourceDialog = true },
                        enabled = isEditing,
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = ChipBg
                    ) {
                        if (capturedImageUri != null) {
                            AsyncImage(
                                model = capturedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (user.photoUrl != null) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Lavender), contentAlignment = Alignment.Center) {
                                Text(
                                    text = name.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkText
                                )
                            }
                        }
                    }

                    if (isEditing) {
                        Text("Tap photo to change", fontSize = 12.sp, color = MutedText)
                    }

                    // Display Name & Bio Info
                    if (isEditing) {
                        ProfileTextField("Display Name", name, onValueChange = { name = it })
                        ProfileTextField("Bio / Description", userDescription, onValueChange = { userDescription = it })
                    } else {
                        Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        if (userDescription.isNotBlank()) {
                            Text(
                                text = userDescription,
                                fontSize = 15.sp,
                                color = MutedText,
                                modifier = Modifier.align(CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // Hobbies Section
            Text("Hobbies", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
            if (isEditing) {
                SimpleSearchBar(
                    label = "Add a hobby...",
                    textFieldState = hobbySearchBarState,
                    onSearch = { },
                    searchResults = hobbiesOptions,
                    onFriendClicked = { hobby -> 
                        addedHobbies = addedHobbies + hobby
                        hobbiesOptions = hobbiesOptions - hobby 
                    }
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                addedHobbies.forEach { hobby ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ChipBg,
                        modifier = if (isEditing) Modifier.clickable {
                            addedHobbies = addedHobbies - hobby
                            if (hobby in event_types) hobbiesOptions = (hobbiesOptions + hobby).sorted()
                        } else Modifier
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(hobby, fontSize = 13.sp, color = ChipPurpleText, fontWeight = FontWeight.SemiBold)
                            if (isEditing) {
                                Spacer(Modifier.width(4.dp))
                                Icon(painterResource(R.drawable.cancel), contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryPurple)
                            }
                        }
                    }
                }
            }

            // Languages Section
            Text("Languages", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
            if (isEditing) {
                SimpleSearchBar(
                    label = "Add a language...",
                    textFieldState = languageSearchBarState,
                    onSearch = { },
                    searchResults = languagesOptions,
                    onFriendClicked = { lang -> 
                        addedLanguages = addedLanguages + lang
                        languagesOptions = languagesOptions - lang
                    }
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                addedLanguages.forEach { language ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SoftPink,
                        modifier = if (isEditing) Modifier.clickable {
                            addedLanguages = addedLanguages - language
                            if (language in languages) languagesOptions = (languagesOptions + language).sorted()
                        } else Modifier
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(language, fontSize = 13.sp, color = PinkTextDark, fontWeight = FontWeight.SemiBold)
                            if (isEditing) {
                                Spacer(Modifier.width(4.dp))
                                Icon(painterResource(R.drawable.cancel), contentDescription = null, modifier = Modifier.size(14.dp), tint = PinkTextDark)
                            }
                        }
                    }
                }
            }

            // Contact Info Fields
            Text("Contact & Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isEditing) {
                        ProfileTextField("Location", userLocation, R.drawable.home, onValueChange = { userLocation = it })
                        ProfileTextField("Instagram / Social", userInstagram, R.drawable.language, onValueChange = { userInstagram = it })
                        ProfileTextField("Phone", userPhone, R.drawable.phone, onValueChange = { userPhone = it })
                    } else {
                        InfoRow(R.drawable.home, "Location", userLocation.ifBlank { "Not set" })
                        InfoRow(R.drawable.language, "Social", userInstagram.ifBlank { "Not set" })
                        InfoRow(R.drawable.phone, "Phone", userPhone.ifBlank { "Not set" })
                        InfoRow(R.drawable.mail, "Email", user.email)
                    }
                }
            }

            // Save / Discard / Logout Actions
            if (isEditing) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            name = user.displayName
                            userDescription = user.description
                            userLocation = user.location
                            userInstagram = user.instagram
                            userPhone = user.phone
                            addedHobbies = user.hobbies
                            addedLanguages = user.languages
                            hobbiesOptions = event_types.filter { it !in user.hobbies }.sorted()
                            languagesOptions = languages.filter { it !in user.languages }.sorted()
                            capturedImageUri = null
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple)
                    ) {
                        Text("Discard")
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
                            profileViewModel.updateProfile(updates, capturedImageUri) { result ->
                                result.onSuccess {
                                    Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show()
                                    capturedImageUri = null
                                    isEditing = false
                                }.onFailure { e ->
                                    Toast.makeText(context, "Couldn't save profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text(if (isOnline) "Save" else "Offline")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { authViewModel.logout() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = { showDeleteDialog = true },
                    enabled = isOnline,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text(if (isOnline) "Delete Account" else "No internet connection (Cannot delete account)")
                }
            }

            Spacer(Modifier.height(40.dp))
        }
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
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deletePassword = ""
                }) { Text("Cancel") }
            }
        )
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Update Profile Picture") },
            text = { Text("Choose how you want to update your profile picture.") },
            confirmButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Gallery") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                            val uri = createImageUri(context)
                            if (uri != null) {
                                tempImageUri = uri
                                cameraLauncher.launch(uri)
                            }
                        }
                        else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text("Camera") }
            }
        )
    }
}

@Composable
private fun InfoRow(icon: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp), tint = MutedText)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MutedText)
            Text(value, fontSize = 15.sp, color = DarkText, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProfileTextField(label: String, value: String, icon: Int? = null, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = MutedText, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = icon?.let { { Icon(painterResource(it), null, modifier = Modifier.size(18.dp), tint = MutedText) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = ChipBg,
                focusedContainerColor = Color(0xFFF9F9F9),
                unfocusedContainerColor = Color(0xFFF9F9F9)
            ),
            singleLine = true
        )
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
        awaitDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}

private fun checkIsOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun PreviewProfileScreen() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = ScreenBg) {
            ProfileScreen()
        }
    }
}
