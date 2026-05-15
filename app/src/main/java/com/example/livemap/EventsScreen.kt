package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livemap.ui.theme.LiveMapTheme

/* ---------- Peach + Sage + Honey palette (Events screen) ---------- */
private val Peach        = Color(0xFFFFD4B8)  // durazno (chip Food)
private val Sage         = Color(0xFFC8D5B0)  // salvia clara (chip Sport)
private val Honey        = Color(0xFFF0DDB0)  // miel (chip Social)
private val Sand         = Color(0xFFE8D0C5)  // arena (chip Age)
private val SageDark     = Color(0xFF8FA968)  // verde salvia oscuro (chips filtros activos)
private val JoinedGreen  = Color(0xFF6B9444)  // verde llamativo "Joined"
private val OwnerOrange  = Color(0xFFE8A57A)  // naranja suave "Owner"
private val JoinBrown    = Color(0xFFB07A4D)  // marrón cálido "Join"

private val ScreenBg     = Color(0xFFFBF6EE)  // fondo crema cálido
private val ChipBg       = Color(0xFFEAE5D6)  // fondo de chips/filtros
private val DarkText     = Color(0xFF3D4A2A)  // texto verde oscuro (titulos)
private val BodyText     = Color(0xFF5C3522)  // texto cuerpo (marrón oscuro)
private val MutedText    = Color(0xFF8B5E47)  // texto secundario marrón
private val HoneyText    = Color(0xFF6B4A0E)  // texto sobre chips miel/arena
private val SageText     = Color(0xFF6B7855)  // texto verde apagado

/* ---------- UI-only data class ---------- */
data class EventUi(
    val id: Int,
    val name: String,
    val category: String,
    val date: String,
    val time: String,
    val location: String,
    val distanceKm: Double,
    val ageRange: String,
    val genderPref: String,
    val joined: Int,
    val maxPeople: Int,
    val ownedByMe: Boolean = false,
    val timeBucket: String = "Today"
)

/* ---------- Filter option lists ---------- */
private val DistanceOptions  = listOf("1 km", "5 km", "10 km", "25 km")
private val CategoryOptions  = listOf("Sport", "Study", "Social", "Art", "Food", "Music")
private val AgeOptions       = listOf("18-25", "26-35", "36-45", "Any")
private val GenderOptions    = listOf("Any", "Male", "Female", "Mixed")
private val MaxPeopleOptions = listOf("5", "10", "20", "Any")
private val TimeOptions      = listOf("Today", "Tomorrow", "This week", "Any")

/* ---------- Sample events (placeholder — replace with Firebase later) ---------- */
private val sampleEvents = listOf(
    EventUi(1, "Sunset Run",          "Sport",  "Today",       "19:00", "Tübingen Neckar river", 0.8,  "18-25", "Any",   3, 10, false, "Today"),
    EventUi(2, "Algorithms Study",    "Study",  "Today",       "16:00", "Uni Library",           1.2,  "18-25", "Mixed", 4, 5,  false, "Today"),
    EventUi(3, "Coffee & Chat",       "Social", "Tomorrow",    "10:00", "Café Lieblich",         2.4,  "Any",   "Any",   5, 8,  false, "Tomorrow"),
    EventUi(4, "Acrylic Painting",    "Art",    "Sat 18 May",  "14:00", "Atelier Süd",           4.7,  "26-35", "Female",2, 6,  false, "This week"),
    EventUi(5, "Ramen Night",         "Food",   "Tomorrow",    "20:00", "Ramen Yama",            3.1,  "26-35", "Mixed", 6, 10, false, "Tomorrow"),
    EventUi(6, "Indie Live Session",  "Music",  "Sat 18 May",  "21:00", "Hirschau Bar",          6.5,  "18-25", "Any",   12, 20, false, "This week"),
    EventUi(7, "Board Games Evening", "Social", "Today",       "18:30", "Nico's place",          0.4,  "Any",   "Mixed", 4, 8,  true,  "Today"),
    EventUi(8, "Weekend Hike",        "Sport",  "Sat 18 May",  "09:00", "Schönbuch forest",      11.2, "Any",   "Any",   2, 12, false, "This week"),
    EventUi(9, "Korean Cooking",      "Food",   "This Friday", "18:00", "Sharing Kitchen",       18.0, "26-35", "Any",   3, 5,  false, "This week")
)

/* ---------- Screen ---------- */
@Composable
fun EventsScreen(vm: CounterViewModel) {
    var distance  by remember { mutableStateOf<String?>(null) }
    var category  by remember { mutableStateOf<String?>(null) }
    var age       by remember { mutableStateOf<String?>(null) }
    var gender    by remember { mutableStateOf<String?>(null) }
    var maxPeople by remember { mutableStateOf<String?>(null) }
    var time      by remember { mutableStateOf<String?>(null) }

    val joinedIds      = remember { mutableStateListOf<Int>() }
    val recentlyJoined = remember { mutableStateListOf<Int>() }

    val filtered = sampleEvents.filter { ev ->
        val byDistance = when (distance) {
            "1 km"  -> ev.distanceKm <= 1
            "5 km"  -> ev.distanceKm <= 5
            "10 km" -> ev.distanceKm <= 10
            "25 km" -> ev.distanceKm <= 25
            else    -> true
        }
        val byCategory  = category  == null || ev.category  == category
        val byAge       = age       == null || age == "Any" || ev.ageRange == age
        val byGender    = gender    == null || gender == "Any" || ev.genderPref == gender || ev.genderPref == "Any"
        val byTime      = time      == null || time == "Any" || ev.timeBucket == time
        val byMaxPeople = when (maxPeople) {
            "5"   -> ev.maxPeople <= 5
            "10"  -> ev.maxPeople <= 10
            "20"  -> ev.maxPeople <= 20
            else  -> true
        }
        byDistance && byCategory && byAge && byGender && byTime && byMaxPeople
    }

    val available  = filtered.filter { !it.ownedByMe && !joinedIds.contains(it.id) }
    val joinedList = sampleEvents.filter { joinedIds.contains(it.id) }
    val recentList = recentlyJoined.mapNotNull { id -> sampleEvents.find { it.id == id } }
    val myCreated  = sampleEvents.filter { it.ownedByMe }

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
                    text = "Events",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = DarkText
                )
            }

            item {
                FiltersCard(
                    distance = distance, onDistance = { distance = if (distance == it) null else it },
                    category = category, onCategory = { category = if (category == it) null else it },
                    age      = age,      onAge      = { age = if (age == it) null else it },
                    gender   = gender,   onGender   = { gender = if (gender == it) null else it },
                    maxPeople= maxPeople,onMaxPeople= { maxPeople = if (maxPeople == it) null else it },
                    time     = time,     onTime     = { time = if (time == it) null else it },
                    onClear  = {
                        distance = null; category = null; age = null
                        gender = null; maxPeople = null; time = null
                    }
                )
            }

            item { SectionHeader("Near you", available.size) }
            if (available.isEmpty()) {
                item { EmptyHint("No events match your filters.") }
            } else {
                items(available) { ev ->
                    EventCard(
                        event = ev,
                        actionLabel = "Join",
                        actionEnabled = true,
                        cardKind = CardKind.AVAILABLE,
                        onAction = {
                            joinedIds.add(ev.id)
                            recentlyJoined.remove(ev.id)
                            recentlyJoined.add(0, ev.id)
                        }
                    )
                }
            }

            item { SectionHeader("Joined events", joinedList.size) }
            if (joinedList.isEmpty()) {
                item { EmptyHint("You haven't joined any events yet.") }
            } else {
                items(joinedList) { ev ->
                    EventCard(
                        event = ev,
                        actionLabel = "Joined",
                        actionEnabled = false,
                        cardKind = CardKind.JOINED
                    )
                }
            }

            item { SectionHeader("Recently joined", recentList.size) }
            if (recentList.isEmpty()) {
                item { EmptyHint("Nothing here yet — tap Join on an event.") }
            } else {
                items(recentList) { ev ->
                    EventCard(
                        event = ev,
                        actionLabel = "Joined",
                        actionEnabled = false,
                        cardKind = CardKind.JOINED
                    )
                }
            }

            item { SectionHeader("My created events", myCreated.size) }
            if (myCreated.isEmpty()) {
                item { EmptyHint("You haven't created any events yet.") }
            } else {
                items(myCreated) { ev ->
                    EventCard(
                        event = ev,
                        actionLabel = "Owner",
                        actionEnabled = false,
                        cardKind = CardKind.OWNER
                    )
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

/* ---------- Helpers and small composables ---------- */

private enum class CardKind { AVAILABLE, JOINED, OWNER }

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
                Text(
                    text = "Filters",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkText,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Clear all",
                    color = HoneyText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(ChipBg, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .clickableNoRipple { onClear() }
                )
            }
            Spacer(Modifier.height(6.dp))

            FilterRow("Distance",    DistanceOptions,  distance,  onDistance)
            FilterRow("Category",    CategoryOptions,  category,  onCategory)
            FilterRow("Age range",   AgeOptions,       age,       onAge)
            FilterRow("Gender",      GenderOptions,    gender,    onGender)
            FilterRow("Max people",  MaxPeopleOptions, maxPeople, onMaxPeople)
            FilterRow("Time",        TimeOptions,      time,      onTime)
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = SageText)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { opt ->
                FilterChipPill(
                    text = opt,
                    selected = selected == opt,
                    onClick = { onSelect(opt) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) SageDark else ChipBg
    val fg = if (selected) Color.White else HoneyText
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        modifier = Modifier.clickableNoRipple(onClick)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
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
                color = HoneyText,
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
private fun EventCard(
    event: EventUi,
    actionLabel: String,
    actionEnabled: Boolean,
    cardKind: CardKind,
    onAction: () -> Unit = {}
) {
    // Colores del botón según el tipo de card
    val (buttonColor, buttonContent) = when (cardKind) {
        CardKind.AVAILABLE -> JoinBrown to Color.White
        CardKind.JOINED    -> JoinedGreen to Color.White
        CardKind.OWNER     -> OwnerOrange to Color.White
    }

    // Las cards "Joined" tienen un borde verde suave para destacar
    val cardBorder = if (cardKind == CardKind.JOINED) BorderStroke(2.dp, Sage) else null

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = cardBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DarkText,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(50), color = categoryColor(event.category)) {
                    Text(
                        text = event.category,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = categoryTextColor(event.category),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            InfoLine(R.drawable.schedule, "${event.date} • ${event.time}")
            Spacer(Modifier.height(4.dp))
            InfoLine(R.drawable.location_on, "${event.location}  •  ${event.distanceKm} km")
            Spacer(Modifier.height(4.dp))
            InfoLine(R.drawable.group, "${event.joined}/${event.maxPeople} joined")

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTag("Age: ${event.ageRange}", Sand, BodyText)
                if (event.genderPref != "Any") {
                    MiniTag(event.genderPref, Honey, HoneyText)
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
                    // Cuando el botón está disabled (Joined/Owner) mantenemos el color de fondo,
                    // así no se ve "apagado" sino intencionalmente coloreado.
                    disabledContainerColor = buttonColor,
                    disabledContentColor = buttonContent
                )
            ) { Text(actionLabel) }
        }
    }
}

@Composable
private fun InfoLine(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            tint = MutedText,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = text, fontSize = 13.sp, color = BodyText)
    }
}

@Composable
private fun MiniTag(text: String, bg: Color, fg: Color) {
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/* Color del chip de categoría dentro de cada card */
private fun categoryColor(category: String): Color = when (category) {
    "Sport"  -> Sage
    "Study"  -> Sand
    "Social" -> Honey
    "Art"    -> Color(0xFFE8C5C5)  // rosa terroso
    "Food"   -> Peach
    "Music"  -> Color(0xFFD4C5E8)  // lila terroso
    else     -> ChipBg
}

private fun categoryTextColor(category: String): Color = when (category) {
    "Sport"  -> Color(0xFF3D4A2A)
    "Study"  -> Color(0xFF5C3522)
    "Social" -> Color(0xFF6B4A0E)
    "Art"    -> Color(0xFF7A2828)
    "Food"   -> Color(0xFF5C3522)
    "Music"  -> Color(0xFF3D2F5C)
    else     -> Color(0xFF6B4A0E)
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
)

/* ---------- KEEP these for the rest of the project ----------
   NewScreen.kt calls EventInfoButton(...) and EventDetailScreen.kt calls EventInfo(...).
   They were originally defined in the old EventsScreen.kt, so we keep them here
   to avoid breaking those files. They are NOT used by the new EventsScreen above. */

@Composable
fun EventInfo(icon: Int, text: String) {
    Row {
        Icon(
            painter = painterResource(icon),
            tint = Color.LightGray,
            contentDescription = null,
            modifier = Modifier.padding(end = 7.dp, bottom = 2.dp)
        )
        Text(text = text, fontWeight = FontWeight.Normal, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun EventInfoButton(event: Event) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(140.dp)
            .padding(bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = event.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)
                )
                EventInfo(R.drawable.schedule, event.date + ", " + event.timeStart + "-" + event.timeEnd)
                EventInfo(R.drawable.location_on, event.location)
                EventInfo(R.drawable.group, "" + event.participants.count() + "/" + event.limitPeople + " joined")
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PreviewEventsScreen() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EventsScreen(vm = CounterViewModel())
        }
    }
}
