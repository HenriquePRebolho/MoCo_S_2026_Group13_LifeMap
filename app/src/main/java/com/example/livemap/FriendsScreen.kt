package com.example.livemap

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livemap.data.model.Friendship
import com.example.livemap.ui.friends.FriendUi
import com.example.livemap.ui.friends.FriendsState
import com.example.livemap.ui.friends.FriendsViewModel
import com.google.firebase.auth.FirebaseAuth

/* ---------- Lavender Dream palette — UNCHANGED ---------- */
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

/* ---------- Screen ---------- */
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel = viewModel(),
    onNavigateToFriendDetail: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(ScreenBg)) {
        when (val s = state) {
            is FriendsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
            is FriendsState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(s.message, color = DarkText, fontWeight = FontWeight.SemiBold)
            }
            is FriendsState.Loaded -> LoadedContent(
                state = s,
                query = query,
                onQueryChange = { query = it },
                onSendRequest = viewModel::sendFriendRequest,
                onAcceptRequest = viewModel::acceptFriendRequest,
                onNavigateToFriendDetail = onNavigateToFriendDetail
            )
        }
    }
}

@Composable
private fun LoadedContent(
    state: FriendsState.Loaded,
    query: String,
    onQueryChange: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onNavigateToFriendDetail: (String) -> Unit
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    fun matches(f: FriendUi): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return f.name.lowercase().contains(q) || f.interests.any { it.lowercase().contains(q) }
    }

    val filteredFriends   = state.myFriends.filter(::matches)
    val filteredRequests  = state.incomingRequests.filter(::matches)
    val filteredSuggested = state.suggested.filter(::matches)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Friends", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = DarkText) }

        item { SearchBarFriends(query, onQueryChange) }

        if (filteredRequests.isNotEmpty()) {
            item { SectionHeader("Friend Requests", filteredRequests.size) }
            items(filteredRequests, key = { it.id }) { friend ->
                FriendCard(
                    friend = friend,
                    commonInterests = state.myInterests.intersect(friend.interests.toSet()).toList(),
                    actionLabel = "Accept",
                    actionEnabled = true,
                    avatarColor = LightBlue,
                    avatarTextColor = BlueTextDark,
                    onAction = { onAcceptRequest(friend.id) },
                    onViewProfile = { onNavigateToFriendDetail(friend.id) }
                )
            }
        }

        item { SectionHeader("My Friends", filteredFriends.size) }
        if (filteredFriends.isEmpty()) {
            item { EmptyHint("No friends match your search.") }
        } else {
            items(filteredFriends, key = { it.id }) { friend ->
                FriendCard(
                    friend = friend,
                    commonInterests = state.myInterests.intersect(friend.interests.toSet()).toList(),
                    actionLabel = "Friends",
                    actionEnabled = false,
                    avatarColor = Lavender,
                    avatarTextColor = DarkText,
                    onAction = { },
                    onViewProfile = { onNavigateToFriendDetail(friend.id) }
                )
            }
        }

        item { SectionHeader("Suggested for you", filteredSuggested.size) }
        if (filteredSuggested.isEmpty()) {
            item { EmptyHint("No suggestions match your search.") }
        } else {
            items(filteredSuggested, key = { it.id }) { friend ->
                val friendshipId = Friendship.buildId(currentUid, friend.id)
                val alreadySent = state.sentRequestFriendshipIds.contains(friendshipId)
                FriendCard(
                    friend = friend,
                    commonInterests = state.myInterests.intersect(friend.interests.toSet()).toList(),
                    actionLabel = if (alreadySent) "Request Sent" else "Add Friend",
                    actionEnabled = !alreadySent,
                    avatarColor = SoftPink,
                    avatarTextColor = PinkTextDark,
                    onAction = { onSendRequest(friend.id) },
                    onViewProfile = { onNavigateToFriendDetail(friend.id) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

/* ---------- LazyListScope.items wrapper ---------- */
private inline fun androidx.compose.foundation.lazy.LazyListScope.items(
    items: List<FriendUi>,
    noinline key: (FriendUi) -> Any,
    crossinline itemContent: @Composable (FriendUi) -> Unit
) = items(count = items.size, key = { key(items[it]) }) { itemContent(items[it]) }

/* ---------- Reusable composables — UNCHANGED from the team's design ---------- */

@Composable
private fun SearchBarFriends(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search by name or interest") },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.search),
                contentDescription = null, tint = MutedText)
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = PrimaryPurple,
            unfocusedBorderColor = ChipBg
        )
    )
}


@Composable
private fun FriendCard(
    friend: FriendUi,
    commonInterests: List<String>,
    actionLabel: String,
    actionEnabled: Boolean,
    avatarColor: Color,
    avatarTextColor: Color,
    onAction: () -> Unit,
    onViewProfile: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(friend.name.first().toString(), fontWeight = FontWeight.Bold,
                        fontSize = 22.sp, color = avatarTextColor)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    val ageStr = if (friend.age > 0) ", ${friend.age}" else ""
                    Text("${friend.name}$ageStr", fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = DarkText)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(R.drawable.location_on),
                            contentDescription = null, tint = MutedText,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(friend.location, color = MutedText, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(friend.bio, fontSize = 14.sp, color = DarkText)

            if (friend.interests.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ChipsRow("Interests", friend.interests, ChipBg, ChipPurpleText)
            }

            if (commonInterests.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                ChipsRow("In common", commonInterests, SoftPink, PinkTextDark)
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onViewProfile,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightBlue,
                        contentColor = BlueTextDark
                    )
                ) { Text("View Profile") }

                Button(
                    onClick = onAction,
                    enabled = actionEnabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple,
                        contentColor = Color.White,
                        disabledContainerColor = SentGrey,
                        disabledContentColor = Color.White
                    )
                ) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun ChipsRow(label: String, items: List<String>, chipColor: Color, textColor: Color) {
    Column {
        Text(label, fontSize = 12.sp, color = MutedText)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.take(5).forEach { item ->
                Surface(shape = RoundedCornerShape(50), color = chipColor) {
                    Text(item, fontSize = 12.sp, color = textColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
    }
}