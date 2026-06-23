package com.example.livemap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.livemap.ui.events.EventUi
import com.example.livemap.ui.events.EventsState
import com.example.livemap.ui.events.EventsViewModel

/* ---------- Peach + Sage + Honey palette (Events screen) — UNCHANGED ---------- */
private val Peach        = Color(0xFFFFD4B8)
private val Sage         = Color(0xFFC8D5B0)
private val Honey        = Color(0xFFF0DDB0)
private val Sand         = Color(0xFFE8D0C5)
private val SageDark     = Color(0xFF8FA968)
private val JoinedGreen  = Color(0xFF6B9444)
private val OwnerOrange  = Color(0xFFE8A57A)
private val JoinBrown    = Color(0xFFB07A4D)

private val ScreenBg     = Color(0xFFFBF6EE)
private val ChipBg       = Color(0xFFEAE5D6)
private val DarkText     = Color(0xFF3D4A2A)
private val BodyText     = Color(0xFF5C3522)
private val MutedText    = Color(0xFF8B5E47)
private val HoneyText    = Color(0xFF6B4A0E)
private val SageText     = Color(0xFF6B7855)

/* ---------- Filter option lists — UNCHANGED ---------- */
private val DistanceOptions  = listOf("1 km", "5 km", "10 km", "25 km")
private val CategoryOptions  = listOf("Sport", "Study", "Social", "Art", "Food", "Music")
private val AgeOptions       = listOf("18-25", "26-35", "36-45", "Any")
private val GenderOptions    = listOf("Any", "Male", "Female", "Mixed")
private val MaxPeopleOptions = listOf("5", "10", "20", "Any")
private val TimeOptions      = listOf("Today", "Tomorrow", "This week", "Any")

enum class CardKind { AVAILABLE, JOINED, OWNER }

/* ---------- Screen ---------- */
@Composable
fun EventsScreen(
    viewModel: EventsViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        when (val s = state) {
            is EventsState.Loading -> LoadingContent()
            is EventsState.Error -> ErrorContent(s.message)
            is EventsState.Loaded -> LoadedContent(
                state = s,
                distance = filters.distance,
                category = filters.category,
                age = filters.ageRange,
                gender = filters.gender,
                maxPeople = filters.maxPeople,
                time = filters.time,
                onDistance = viewModel::setDistance,
                onCategory = viewModel::setCategory,
                onAge = viewModel::setAge,
                onGender = viewModel::setGender,
                onMaxPeople = viewModel::setMaxPeople,
                onTime = viewModel::setTime,
                onClear = viewModel::clearFilters,
                onJoin = viewModel::joinEvent,
                onEventClick = onNavigateToDetail
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SageDark)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = DarkText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoadedContent(
    state: EventsState.Loaded,
    distance: String?, onDistance: (String?) -> Unit,
    category: String?, onCategory: (String?) -> Unit,
    age: String?, onAge: (String?) -> Unit,
    gender: String?, onGender: (String?) -> Unit,
    maxPeople: String?, onMaxPeople: (String?) -> Unit,
    time: String?, onTime: (String?) -> Unit,
    onClear: () -> Unit,
    onJoin: (String) -> Unit,
    onEventClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Events",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = DarkText
            )
        }

        item {
            FiltersCard(
                distance = distance, onDistance = { onDistance(if (distance == it) null else it) },
                category = category, onCategory = { onCategory(if (category == it) null else it) },
                age = age,           onAge = { onAge(if (age == it) null else it) },
                gender = gender,     onGender = { onGender(if (gender == it) null else it) },
                maxPeople = maxPeople, onMaxPeople = { onMaxPeople(if (maxPeople == it) null else it) },
                time = time,         onTime = { onTime(if (time == it) null else it) },
                onClear = onClear
            )
        }

        item { SectionHeader("Near you", state.nearby.size) }
        if (state.nearby.isEmpty()) {
            item { EmptyHint("No events match your filters.") }
        } else {
            items(state.nearby, key = { "nearby_${it.id}" }) { ev ->
                EventCard(ev, "Join", true, CardKind.AVAILABLE, 
                    onAction = { onJoin(ev.id) },
                    onClick = { onEventClick(ev.id) }
                )
            }
        }

        item { SectionHeader("Joined events", state.joined.size) }
        if (state.joined.isEmpty()) {
            item { EmptyHint("You haven't joined any events yet.") }
        } else {
            items(state.joined, key = { "joined_${it.id}" }) { ev ->
                EventCard(ev, "Joined", false, CardKind.JOINED,
                    onClick = { onEventClick(ev.id) }
                )
            }
        }

        item { SectionHeader("Recently joined", state.recentlyJoined.size) }
        if (state.recentlyJoined.isEmpty()) {
            item { EmptyHint("Nothing here yet — tap Join on an event.") }
        } else {
            items(state.recentlyJoined, key = { "recently_${it.id}" }) { ev ->
                EventCard(ev, "Joined", false, CardKind.JOINED,
                    onClick = { onEventClick(ev.id) }
                )
            }
        }

        item { SectionHeader("My created events", state.owned.size) }
        if (state.owned.isEmpty()) {
            item { EmptyHint("You haven't created any events yet.") }
        } else {
            items(state.owned, key = { "owned_${it.id}" }) { ev ->
                EventCard(ev, "Owner", false, CardKind.OWNER,
                    onClick = { onEventClick(ev.id) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

/* ---------- LazyListScope.items wrapper for cleaner call sites ---------- */
private inline fun androidx.compose.foundation.lazy.LazyListScope.items(
    items: List<EventUi>,
    noinline key: (EventUi) -> Any,
    crossinline itemContent: @Composable (EventUi) -> Unit
) = items(count = items.size, key = { key(items[it]) }) { itemContent(items[it]) }

/* ---------- The rest of the file is the team's design, intact ---------- */

@Composable
private fun FiltersCard(
    distance: String?, onDistance: (String) -> Unit,
    category: String?, onCategory: (String) -> Unit,
    age: String?, onAge: (String) -> Unit,
    gender: String?, onGender: (String) -> Unit,
    maxPeople: String?, onMaxPeople: (String) -> Unit,
    time: String?, onTime: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filters", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText, modifier = Modifier.weight(1f))
                Text(
                    "Clear all",
                    color = HoneyText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(ChipBg, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .clickableNoRipple { onClear() }
                )
            }
            Spacer(Modifier.height(6.dp))
            FilterRow("Distance",   DistanceOptions,  distance,  onDistance)
            FilterRow("Category",   CategoryOptions,  category,  onCategory)
            FilterRow("Age range",  AgeOptions,       age,       onAge)
            FilterRow("Gender",     GenderOptions,    gender,    onGender)
            FilterRow("Max people", MaxPeopleOptions, maxPeople, onMaxPeople)
            FilterRow("Time",       TimeOptions,      time,      onTime)
        }
    }
}

@Composable
private fun FilterRow(label: String, options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = SageText)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { opt ->
                FilterChipPill(opt, selected == opt) { onSelect(opt) }
            }
        }
    }
}

@Composable
fun FilterChipPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) SageDark else ChipBg
    val fg = if (selected) Color.White else HoneyText
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        modifier = Modifier.clickableNoRipple(onClick)
    ) {
        Text(
            text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}


@Composable()
fun EventInfo(icon: Int, text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Icon(painter = painterResource(icon),
            tint = Color.LightGray,
            contentDescription = null,
            modifier = Modifier.padding(end = 7.dp, bottom = 2.dp)
        )
        Text(text = text, fontWeight = FontWeight.Normal, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(50), color = ChipBg) {
            Text(count.toString(), fontSize = 12.sp, color = HoneyText, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Text(text, color = MutedText, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventCard(
    event: EventUi,
    actionLabel: String,
    actionEnabled: Boolean,
    cardKind: CardKind,
    onAction: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val (buttonColor, buttonContent) = when (cardKind) {
        CardKind.AVAILABLE -> JoinBrown to Color.White
        CardKind.JOINED    -> JoinedGreen to Color.White
        CardKind.OWNER     -> OwnerOrange to Color.White
    }
    val cardBorder = if (cardKind == CardKind.JOINED) BorderStroke(2.dp, Sage) else null

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(event.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText, modifier = Modifier.weight(1f))
            Spacer(Modifier.height(8.dp))
            InfoLine(R.drawable.schedule, "${event.date} • ${event.time}")
            Spacer(Modifier.height(4.dp))
            InfoLine(R.drawable.location_on, "${event.location.ifBlank { "TBD" }}  •  ${event.distanceKm} km")
            Spacer(Modifier.height(4.dp))
            InfoLine(R.drawable.group, "${event.joined}/${if (event.maxPeople == 0) "-" else event.maxPeople} joined")

            if (event.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 3 // Limit to show truncation behavior
                ) {
                    event.tags.forEachIndexed { index, tag ->
                        if (index < 3) {
                            MiniTag(tag, Sand, BodyText)
                        } else if (index == 3) {
                            Text("...", color = MutedText, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAction,
                enabled = actionEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = buttonContent,
                    disabledContainerColor = buttonColor,
                    disabledContentColor = buttonContent
                )
            ) { Text(actionLabel) }
        }
    }
}

@Composable
fun InfoLine(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(icon), tint = MutedText,
            contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, color = BodyText)
    }
}

@Composable
fun MiniTag(text: String, bg: Color, fg: Color) {
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(text, fontSize = 11.sp, color = fg, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

fun categoryColor(category: String): Color = when (category) {
    "Sport"  -> Sage
    "Study"  -> Sand
    "Social" -> Honey
    "Art"    -> Color(0xFFE8C5C5)
    "Food"   -> Peach
    "Music"  -> Color(0xFFD4C5E8)
    else     -> ChipBg
}

fun categoryTextColor(category: String): Color = when (category) {
    "Sport"  -> Color(0xFF3D4A2A)
    "Study"  -> Color(0xFF5C3522)
    "Social" -> Color(0xFF6B4A0E)
    "Art"    -> Color(0xFF7A2828)
    "Food"   -> Color(0xFF5C3522)
    "Music"  -> Color(0xFF3D2F5C)
    else     -> Color(0xFF6B4A0E)
}

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
)

/* ---------- Keep these for backward-compat with NewScreen / EventDetailScreen ---------- */

@Composable
fun EventInfo(icon: Int, text: String) {
    Row {
        Icon(painter = painterResource(icon), tint = Color.LightGray,
            contentDescription = null, modifier = Modifier.padding(end = 7.dp, bottom = 2.dp))
        Text(text, fontWeight = FontWeight.Normal, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun EventInfoButton(event: UiEvent) {   // ← was Event (broken type) — now UiEvent (the legacy UI model)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth(0.9f).height(140.dp).padding(bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(event.name, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp))
                EventInfo(R.drawable.schedule, "${event.date}, ${event.timeStart}-${event.timeEnd}")
                EventInfo(R.drawable.location_on, event.location)
                EventInfo(R.drawable.group, "${event.participants.count()}/${event.limitPeople} joined")
            }
        }
    }
}