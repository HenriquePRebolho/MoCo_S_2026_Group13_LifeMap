package com.example.livemap

import android.annotation.SuppressLint
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livemap.ui.theme.LiveMapTheme

/* ---------- Lavender Dream palette (Friends screen) ---------- */
private val Lavender       = Color(0xFFDCC9F5)  // lavanda principal (avatar amigos)
private val SoftPink       = Color(0xFFFFD6E0)  // rosa suave (avatar sugeridos + "in common")
private val LightBlue      = Color(0xFFD4E5FF)  // azul claro (avatar requests + View Profile)
private val PrimaryPurple  = Color(0xFF8B6BBF)  // morado fuerte (botones acción)
private val ScreenBg       = Color(0xFFFAF7FF)  // fondo lavanda muy claro
private val ChipBg         = Color(0xFFECE5F9)  // fondo de chips de interés
private val DarkText       = Color(0xFF3D2F5C)  // texto principal oscuro
private val MutedText      = Color(0xFF6F5F88)  // texto secundario
private val SentGrey       = Color(0xFFB8AEC4)  // gris lavanda para "Request Sent"/"Friends"
private val ChipPurpleText = Color(0xFF8B6BBF)  // texto en chips de interés
private val PinkTextDark   = Color(0xFF8B2C4A)  // texto en chips "in common"
private val BlueTextDark   = Color(0xFF1E3A5F)  // texto en botón "View Profile"

/* ---------- UI-only data class ---------- */
data class FriendUi(
    val id: Int,
    val name: String,
    val age: Int,
    val location: String,
    val bio: String,
    val interests: List<String>
)

/* ---------- Sample data (placeholder — replace with Firebase later) ---------- */
private val sampleSelfInterests = listOf("Hiking", "Coding", "Coffee", "Music")

private val sampleFriends = listOf(
    FriendUi(1, "Anna",   24, "Tübingen",   "Loves long hikes and good coffee.",         listOf("Hiking", "Coffee", "Photography")),
    FriendUi(2, "Marco",  29, "Stuttgart",  "Software dev, weekend climber.",            listOf("Coding", "Climbing", "Music"))
)

private val sampleRequests = listOf(
    FriendUi(3, "Sofia",  22, "Reutlingen", "Art student, always sketching.",            listOf("Art", "Coffee", "Books")),
    FriendUi(4, "Liam",   27, "Tübingen",   "Football every Sunday.",                    listOf("Football", "Music", "Travel"))
)

private val sampleSuggested = listOf(
    FriendUi(5, "Mia",    26, "Tübingen",   "Reader, runner, casual chef.",              listOf("Coffee", "Books", "Cooking")),
    FriendUi(6, "Jonas",  31, "Stuttgart",  "Music producer, into hiking too.",          listOf("Music", "Hiking", "Coding")),
    FriendUi(7, "Elena",  23, "Reutlingen", "Yoga in the morning, movies at night.",     listOf("Yoga", "Movies", "Coffee")),
    FriendUi(8, "David",  28, "Tübingen",   "Board games & specialty coffee.",           listOf("Games", "Coffee", "Coding"))
)

/* ---------- Screen ---------- */
@Composable
fun FriendsScreen(vm: CounterViewModel) {
    var query by remember { mutableStateOf("") }
    val sentRequests = remember { mutableStateListOf<Int>() }

    fun matches(f: FriendUi): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return f.name.lowercase().contains(q) ||
                f.interests.any { it.lowercase().contains(q) }
    }

    val filteredSuggested = sampleSuggested.filter(::matches)
    val filteredFriends   = sampleFriends.filter(::matches)
    val filteredRequests  = sampleRequests.filter(::matches)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Friends",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = DarkText
                )
            }

            item { SearchBarFriends(query = query, onQueryChange = { query = it }) }

            // Friend requests — avatares azules
            if (filteredRequests.isNotEmpty()) {
                item { SectionHeader("Friend Requests", filteredRequests.size) }
                items(filteredRequests) { friend ->
                    val alreadySent = sentRequests.contains(friend.id)
                    FriendCard(
                        friend = friend,
                        commonInterests = sampleSelfInterests.intersect(friend.interests.toSet()).toList(),
                        actionLabel = if (alreadySent) "Request Sent" else "Accept",
                        actionEnabled = !alreadySent,
                        avatarColor = LightBlue,
                        avatarTextColor = BlueTextDark,
                        onAction = { sentRequests.add(friend.id) },
                        onViewProfile = { /* TODO: navigate to FriendDetailScreen */ }
                    )
                }
            }

            // My friends — avatares lavanda
            item { SectionHeader("My Friends", filteredFriends.size) }
            if (filteredFriends.isEmpty()) {
                item { EmptyHint("No friends match your search.") }
            } else {
                items(filteredFriends) { friend ->
                    FriendCard(
                        friend = friend,
                        commonInterests = sampleSelfInterests.intersect(friend.interests.toSet()).toList(),
                        actionLabel = "Friends",
                        actionEnabled = false,
                        avatarColor = Lavender,
                        avatarTextColor = DarkText,
                        onAction = { },
                        onViewProfile = { /* TODO: navigate to FriendDetailScreen */ }
                    )
                }
            }

            // Suggested — avatares rosa
            item { SectionHeader("Suggested for you", filteredSuggested.size) }
            if (filteredSuggested.isEmpty()) {
                item { EmptyHint("No suggestions match your search.") }
            } else {
                items(filteredSuggested) { friend ->
                    val alreadySent = sentRequests.contains(friend.id)
                    FriendCard(
                        friend = friend,
                        commonInterests = sampleSelfInterests.intersect(friend.interests.toSet()).toList(),
                        actionLabel = if (alreadySent) "Request Sent" else "Add Friend",
                        actionEnabled = !alreadySent,
                        avatarColor = SoftPink,
                        avatarTextColor = PinkTextDark,
                        onAction = { sentRequests.add(friend.id) },
                        onViewProfile = { /* TODO: navigate to FriendDetailScreen */ }
                    )
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

/* ---------- Reusable composables ---------- */

@Composable
private fun SearchBarFriends(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search by name or interest") },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = MutedText
            )
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
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = DarkText
        )
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(50), color = ChipBg) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                color = PrimaryPurple,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        color = MutedText,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 4.dp)
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
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.name.first().toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = avatarTextColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${friend.name}, ${friend.age}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkText
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.location_on),
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = friend.location,
                            color = MutedText,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(text = friend.bio, fontSize = 14.sp, color = DarkText)

            Spacer(Modifier.height(8.dp))
            ChipsRow(
                label = "Interests",
                items = friend.interests,
                chipColor = ChipBg,
                textColor = ChipPurpleText
            )

            if (commonInterests.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                ChipsRow(
                    label = "In common",
                    items = commonInterests,
                    chipColor = SoftPink,
                    textColor = PinkTextDark
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
private fun ChipsRow(
    label: String,
    items: List<String>,
    chipColor: Color,
    textColor: Color
) {
    Column {
        Text(text = label, fontSize = 12.sp, color = MutedText)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.take(5).forEach { item ->
                Surface(shape = RoundedCornerShape(50), color = chipColor) {
                    Text(
                        text = item,
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

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PreviewFriendsScreen() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            FriendsScreen(vm = CounterViewModel())
        }
    }
}
