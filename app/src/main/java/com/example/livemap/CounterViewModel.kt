package com.example.livemap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class User(
    val name: String = "Nico",
    val birthday: String = "14/01/1998",
    val description: String = "I like being outside and meeting new people!",
    val languages: List<String> = listOf("English", "Mongolian"),
    val sex: String = "Male",
    val hobbies: List<String> = listOf("Reading", "Football"),
    val contactInfo: List<String> = listOf("+00 234-5678", "@nico.nico"),
    val location: String = "Kaiserstraße 46, 72764 Reutlingen",
    val email: String = "nico.nico@gmail.com",
    val password: String = "password"
)

class CounterViewModel : ViewModel() {

    var profile by mutableStateOf(User())
        private set

    fun updateProfile(user: User) {
        profile = user
    }

}
