package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livemap.ui.theme.LiveMapTheme

// TODO: set fields to not be editable
// TODO: create button that allow changes, then converts into confirm and cancel button

@Composable
fun ProfileScreen(vm: CounterViewModel) {
    val user = vm.profile

    val sex = user.sex;
    val hobbies = user.hobbies;
    val languages = user.languages;

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(30.dp)
    )
    {
        item {
            Column(horizontalAlignment = CenterHorizontally,
                modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(R.drawable.nico), //TODO: add profile picture to user
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
                Row() {
                    Text(text = user.name, modifier = Modifier.padding (0.dp, 0.dp, 5.dp), fontSize = 18.sp,)
                    Icon(
                        painter = painterResource(if (sex == "Male") R.drawable.male else if (sex == "Female") R.drawable.female else R.drawable.agender),
                        tint = Color.Unspecified,
                        contentDescription = "Gender",
                    )
                }
                Text(text = user.description, fontWeight = FontWeight.Light, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 20.dp))
            }
        }
        item {
            Listing("Hobbies", hobbies)
        }
        item {
            Listing("Languages", languages)
        }
        item {
            ProfileInfoButton(field = "Location", value = user.location, R.drawable.home, Color.Green)
        }
        item {
            ProfileInfoButton(field = "Birthday", value = user.birthday, R.drawable.calendar, Color.Blue)
        }
        item {
            ProfileInfoButton(field = "Instagram", value = user.contactInfo[1], R.drawable.language, Color.Cyan)
        }
        item {
            ProfileInfoButton(field = "Phone number", value = user.contactInfo[0], R.drawable.phone, Color.Red)
        }
        item {
            ProfileInfoButton(field = "Email", value = user.email, R.drawable.mail, Color.Yellow)
        }
        item {
            ProfileInfoButton(field = "Password", value = "•••••••••••••••", R.drawable.password, Color.Black)
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


@Composable
fun ProfileInfoButton(field: String, value: String, svg: Int, color: Color) {
    Surface(shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 10.dp))
    {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, modifier = Modifier.padding(start = 16.dp)) {
                Icon(
                    painter = painterResource(svg),
                    tint = Color.White,
                    contentDescription = null,
                    modifier = Modifier.background(color).padding(5.dp)
                )
            }
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.Center) {
                Text(text = field, fontWeight = FontWeight.Light, modifier = Modifier.fillMaxWidth())
                Text(text = value, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}



@Composable()
fun Listing(info: String, list: List<String>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = info,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // FlowRow is the magic component here
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            list.forEach { listitem ->
                Surface(
                    shape = RoundedCornerShape(50), // Pill shape
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = listitem.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
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