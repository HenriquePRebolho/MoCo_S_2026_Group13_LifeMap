package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun text(text: String) {
    Text(text = text, fontSize = 10.sp, modifier = Modifier.fillMaxSize())
}

@Composable
fun ProfileScreen(vm: CounterViewModel) {

    val mod_text = Modifier.fillMaxSize();

    LazyColumn(
        modifier = Modifier.height(500.dp),
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(30.dp)
    )
    {
        item {
            Image(
                painter = painterResource(R.drawable.nico),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color = Color.Blue)
                    .padding(2.dp),
                alignment = AbsoluteAlignment.BottomRight
            )
        }
        item {
            text("Nico")
        }
        item {
            OutlinedTextField(state = rememberTextFieldState(initialText = "Nico"), label = {Text("Name")})
        }
        item {
            //DatePickerModalInput() { }
            Text(text = "14/01/1998", modifier = Modifier.fillMaxSize())
        }
        item {
            // Languages
            LazyColumn(modifier = Modifier
                .height(100.dp)) {
                item {
                    // Dropdown language 1
                    Text(text = "English")
                    // Level 1
                    Text(text = "Native")
                }
                item {
                    // Language 2
                    Text(text = "Mongolian")
                    // Level 2
                    Text(text = "Intermediate")
                }
            }
        }
        item {
            // Dropdown: sex
            Text(text = "Male")
        }
        item {
            // Description
            Text(text = "I like being outside and meeting new people! I like being outside and meeting new people! I like being outside and meeting new people!")
        }
        item {
            // Dropdown + custom hobbies
            LazyColumn (modifier = Modifier
                .height(100.dp)) {
                    item {
                        // Hobby1
                        Text(text = "Reading")
                    }
                    item {
                        Text(text = "Football")
                    }
                }
        }
        item {
            // Other contact information (phone number, email, instagram...)
            LazyColumn(modifier = Modifier
                .height(100.dp)) {
                item {
                    // Dropdown contact category
                    Text(text = "Phone number")
                    // Contact information
                    Text(text = "+00 234-5678")
                }
                item {
                    // Dropdown contact category
                    Text(text = "Instagram")
                    // Contact information
                    Text(text = "nico.nico")
                }
            }
        }
        item {
            // User location (optional, automatically open map focused on it)
            Text(text = "Kaiserstraße 46, 72764 Reutlingen")
        }
        item {
            // User account email
            Text(text = "nico.nico@gmail.com")
        }
        item {
            // Password
            Text(text = "password")
        }
    }
}


@Composable
fun DatePickerModalInput(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Input)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable()
fun PreviewProfileScreen() {
    MaterialTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfileScreen(vm = CounterViewModel())
        }
    }
}