package com.example.livemap

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livemap.data.model.User
import com.example.livemap.ui.friends.FriendDetailState
import com.example.livemap.ui.friends.FriendDetailViewModel
import com.example.livemap.ui.friends.RelationshipStatus
import com.google.firebase.Timestamp
import java.util.Calendar

/* ---------- Lavender Dream palette (shared with FriendsScreen) ---------- */
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

/**
 * Entry point. The ViewModel reads the friend's uid from the navigation
 * route via SavedStateHandle — see FriendDetailViewModel.
 */
@Composable
fun FriendDetailScreen(
    onBack: () -> Unit,
    viewModel: FriendDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Surface action results (success/error) as Toasts.
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        when (val s = state) {
            is FriendDetailState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
            }

            is FriendDetailState.Error -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.message, color = DarkText, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) { Text("Back") }
                }
            }

            is FriendDetailState.Loaded -> LoadedDetail(
                user = s.user,
                relationship = s.relationship,
                commonInterests = s.commonInterests,
                onBack = onBack,
                onSendRequest = viewModel::sendFriendRequest,
                onAccept = viewModel::acceptFriendRequest,
                onRemove = viewModel::removeFriendship
            )
        }
    }
}

@Composable
private fun LoadedDetail(
    user: User,
    relationship: RelationshipStatus,
    commonInterests: List<String>,
    onBack: () -> Unit,
    onSendRequest: () -> Unit,
    onAccept: () -> Unit,
    onRemove: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top bar with back button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Back", color = PrimaryPurple, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = DarkText
                )
            }
        }

        // Header card: avatar + name + location + bio
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Big avatar with the user's initial
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(Lavender),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                            color = DarkText
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    val ageStr = user.birthday.toAge().let { if (it > 0) ", $it" else "" }
                    Text(
                        "${user.displayName.ifBlank { "(no name)" }}$ageStr",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = DarkText
                    )

                    if (user.location.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.location_on),
                                contentDescription = null,
                                tint = MutedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(user.location, color = MutedText, fontSize = 14.sp)
                        }
                    }

                    if (user.description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            user.description,
                            fontSize = 14.sp,
                            color = DarkText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        item {
            //Listing("Hobbies", hobbies)
        }
        item {
            //Listing("Languages", languages)
        }

        // Languages
        if (user.languages.isNotEmpty()) {
            item { ChipsBlock("Languages", user.languages, ChipBg, ChipPurpleText) }
        }

        // Contact info — Instagram and Phone, if set
        if (user.instagram.isNotBlank() || user.phone.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Contact",
                            fontSize = 14.sp,
                            color = MutedText,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (user.instagram.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Instagram: ${user.instagram}", color = DarkText, fontSize = 14.sp)
                        }
                        if (user.phone.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Phone: ${user.phone}", color = DarkText, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Relationship action — varies based on current relationship
        item {
            Spacer(Modifier.height(8.dp))
            RelationshipActionButton(
                relationship = relationship,
                onSendRequest = onSendRequest,
                onAccept = onAccept,
                onRemove = onRemove
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun ChipsBlock(
    label: String,
    items: List<String>,
    chipColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, fontSize = 14.sp, color = MutedText, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            // Single-row chips for simplicity; truncates at 6.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items.take(6).forEach { item ->
                    Surface(shape = RoundedCornerShape(50), color = chipColor) {
                        Text(
                            item,
                            fontSize = 12.sp,
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders the right action(s) at the bottom of the profile based on the
 * current relationship. This is where the sealed RelationshipStatus shines:
 * the compiler forces us to handle every possible relationship.
 */
@Composable
private fun RelationshipActionButton(
    relationship: RelationshipStatus,
    onSendRequest: () -> Unit,
    onAccept: () -> Unit,
    onRemove: () -> Unit
) {
    when (relationship) {
        RelationshipStatus.Self -> {
            Text(
                "This is you",
                color = MutedText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }

        RelationshipStatus.NotConnected -> {
            Button(
                onClick = onSendRequest,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple,
                    contentColor = Color.White
                )
            ) { Text("Add Friend") }
        }

        RelationshipStatus.Pending -> {
            Button(
                onClick = { /* no-op */ },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = SentGrey,
                    disabledContentColor = Color.White
                )
            ) { Text("Request Sent") }
        }

        RelationshipStatus.IncomingRequest -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightBlue,
                        contentColor = BlueTextDark
                    )
                ) { Text("Decline") }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple,
                        contentColor = Color.White
                    )
                ) { Text("Accept") }
            }
        }

        RelationshipStatus.Friends -> {
            Button(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SentGrey,
                    contentColor = Color.White
                )
            ) { Text("Remove Friend") }
        }
    }
}

private fun Timestamp?.toAge(): Int {
    if (this == null) return 0
    val birth = Calendar.getInstance().apply { time = toDate() }
    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
    return age.coerceAtLeast(0)
}