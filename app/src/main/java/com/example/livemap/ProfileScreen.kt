package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.livemap.ui.theme.LiveMapTheme

@Composable
fun text(text: String) {
    Text(text = text, fontSize = 10.sp, modifier = Modifier.fillMaxSize())
}

@Composable
fun ProfileScreen(vm: CounterViewModel) {
    val user = vm.profile

    val hobbies = user.hobbies.toString().substring(1, user.hobbies.toString().length-1)
    val languages = user.languages.toString().substring(1, user.languages.toString().length-1)

    val mod_text = Modifier.fillMaxSize();

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(30.dp)
    )
    {
        item {
            Column(horizontalAlignment = CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(R.drawable.nico),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
                Text("Nico")
                Text(text = user.description, fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
            }

        }
        item {
            // DatePickerModalInput() { }
            OutlinedTextField(state = rememberTextFieldState(initialText = user.birthday), label = {Text("Birthday")}, leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.icons_calendar),
                    contentDescription = null
                )
            })
        }
        item {
            // Dropdown: sex
            OutlinedTextField(state = rememberTextFieldState(initialText = user.sex), label = {Text("Sex")})
            //Text(text = user.sex)
        }
        item {
            // Languages
            OutlinedTextField(state = rememberTextFieldState(initialText = languages), label = {Text("Languages")})
            //Text(text = user.languages.map { l -> l }.toString())
        }
        item {
            // Dropdown and custom hobbies
            OutlinedTextField(state = rememberTextFieldState(initialText = hobbies), label = {Text("Hobbies")})
            //Text(text = user.hobbies.toString())
        }
        item {
            // Other contact information (phone number, email, instagram...)
            LazyColumn(modifier = Modifier
                .height(130.dp)) {
                item {
                    // Dropdown contact category
                    OutlinedTextField(state = rememberTextFieldState(initialText = user.contactInfo[0]), label = {Text("Phone number")})
                    //Text(text = "Phone number: ${user.contactInfo[0]}")
                }
                item {
                    // Dropdown contact category
                    OutlinedTextField(state = rememberTextFieldState(initialText = user.contactInfo[1]), label = {Text("Instagram")})
                    //Text(text = "Instagram: ${user.contactInfo[1]}")
                }
            }
        }
        item {
            // User location (optional, automatically open map focused on it)
            OutlinedTextField(state = rememberTextFieldState(initialText = user.location), label = {Text("Location")})
            // Text(text = user.location)
        }
        item {
            // User account email
            OutlinedTextField(state = rememberTextFieldState(initialText = user.email), label = {Text("Email")})
            // Text(text = user.email)
        }
        item {
            // Password: will be **** and a button to show
            OutlinedTextField(state = rememberTextFieldState(initialText = user.password), label = {Text("Password")})
            //Text(text = user.password)
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
        LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProfileScreen(vm = CounterViewModel())
        }
    }
}