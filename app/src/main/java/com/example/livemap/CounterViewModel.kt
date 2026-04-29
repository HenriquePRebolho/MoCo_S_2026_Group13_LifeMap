package com.example.livemap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class User(
    val name: String = "Nico", // 33 max characters
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

data class Event(
    val id: Int = 1,
    val name: String = "Event name",
    val date: String = "14/01/2023",
    val time_start: String = "14:00",
    val time_end: String = "15:00",
    val location: String = "Kaiserstraße 46, 72764 Reutlingen",
    val description: String = "Event description",
    val participants: List<String> = listOf("User1", "User2"),
    val owner: String = "User1",
    val tags: List<String> = listOf("Tag1", "Tag2"),
    val restrictions: List<String> = listOf("Restriction1", "Restriction2"),
    val languages: List<String> =  listOf("English", "Mongolian", "Cantonese", "Bosnian", "Yapper"),
    val limitPeople: Int = 12,
    val public: Boolean = true,
    val bring: List<String> = listOf("Bring1", "Bring2"),
    val contactInfo: List<String> = listOf("+00 234-5678", "@nico.nico")
)

class CounterViewModel : ViewModel() {

    var profile by mutableStateOf(User())
        private set

    var event by mutableStateOf(Event())
        private set

    var visibility_owned by mutableStateOf(true)

    var visibility_joined by mutableStateOf(true)

    var users_list by mutableStateOf(listOf(profile))

    var events_list by mutableStateOf(listOf(event))


    fun updateProfile(user: User) {
        profile = user
    }

    fun updateEvent(event: Event) {
        this.event = event
    }

}
