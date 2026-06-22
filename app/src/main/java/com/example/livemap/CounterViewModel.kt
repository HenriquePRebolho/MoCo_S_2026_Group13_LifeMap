package com.example.livemap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class UiUser(
    val id: Int = 1,
    val name: String = "Nico1234567", // 33 max characters
    val birthday: String = "14/01/1998",
    val description: String = "I like being outside and meeting new people!",
    val languages: List<String> = listOf("English", "Mongolian"),
    val sex: String = "Male",
    val hobbies: List<String> = listOf("Reading", "Football", "Ski", "Coding", "Traveling", "Movies", "Hiking"),
    val contactInfo: List<String> = listOf("+00 234-5678", "@nico.nico"),
    val location: String = "Kaiserstraße 46, 72764 Reutlingen, Deutschland",
    val email: String = "nico.nico@gmail.com",
    val password: String = "password",
    val friends: List<String> = listOf("User1", "User2"),
    val blocked: List<String> = listOf("User3", "User4"),
)

data class UiEvent(
    val id: Int = 1,
    var name: String = "Event name",
    var date: String = "14/01/2023",
    var timeStart: String = "14:00",
    val timeEnd: String = "15:00",
    var location: String = "Kaiserstraße 46, 72764 Reutlingen",
    var description: String = "Event description",
    val participants: List<String> = listOf("User1", "User2"),
    val owner: String = "User1",
    val tags: List<String> = listOf("Tag1", "Tag2"),
    val restrictions: List<String> = listOf("Restriction1", "Restriction2"),
    val languages: List<String> =  listOf("English", "Mongolian", "Cantonese", "Bosnian", "Yapper"),
    var limitPeople: Int = 12,
    val public: Boolean = true,
    val bring: List<String> = listOf("Bring1", "Bring2"),
    val contactInfo: List<String> = listOf("+00 234-5678", "@nico.nico"),
    val locationLat: Double = 0.0,
    val locationLng: Double = 0.0,
){
    // True if this event has real GPS coordinates (not the default 0,0)
    val hasLocation: Boolean
        get() = locationLat != 0.0 && locationLng != 0.0
}


class CounterViewModel : ViewModel() {
    var profile by mutableStateOf(UiUser())
        private set

    var event by mutableStateOf(UiEvent())
        private set

    var visibilityOwned by mutableStateOf(true)
    var visibilityEvent by mutableStateOf(true)

    // Sample events with real coordinates
    // TODO: replace with events stored in the backend
    val events: List<UiEvent> = listOf(
        UiEvent(
            id = 1,
            name = "Mercedes-Benz Museum Visit",
            date = "14/06/2025",
            timeStart = "10:00",
            timeEnd = "14:00",
            location = "Mercedes-Benz Museum, Stuttgart",
            description = "Explore the iconic Mercedes-Benz Museum spanning 9 floors of automotive history.",
            owner = "LifeMap",
            tags = listOf("Museum", "Culture", "Cars"),
            languages = listOf("English", "German"),
            limitPeople = 20,
            public = true,
            locationLat = 48.788244,
            locationLng = 9.234186
        ),
        UiEvent(
            id = 2,
            name = "Porsche Museum Visit",
            date = "21/06/2025",
            timeStart = "10:00",
            timeEnd = "13:00",
            location = "Porsche Museum, Stuttgart-Zuffenhausen",
            description = "Discover the history of Porsche with over 80 iconic vehicles on display.",
            owner = "LifeMap",
            tags = listOf("Museum", "Culture", "Cars"),
            languages = listOf("English", "German"),
            limitPeople = 15,
            public = true,
            locationLat = 48.8344,
            locationLng = 9.1520
        ),
        UiEvent(
            id = 3,
            name = "Reutlingen Stadium",
            date = "28/06/2025",
            timeStart = "17:00",
            timeEnd = "19:00",
            location = "Kreuzeichenstadion, Reutlingen",
            description = "Join us for an evening football match at Reutlingen's local stadium.",
            owner = "LifeMap",
            tags = listOf("Sport", "Football", "Outdoor"),
            languages = listOf("English", "German"),
            limitPeople = 30,
            public = true,
            locationLat = 48.478490,
            locationLng = 9.189913
        )
    )

}
