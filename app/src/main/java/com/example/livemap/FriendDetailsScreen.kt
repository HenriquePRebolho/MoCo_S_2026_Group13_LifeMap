package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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


@Composable
fun FriendDetailScreen(vm: CounterViewModel) {
    val user = vm.profile

    val sex = user.sex;
    val hobbies = user.hobbies;
    val languages = user.languages;
    val isFriend = user.friends.contains(user.name)

    fun addOrRemoveFriend() {
        if (isFriend) {
            //user.friends.get(user.name);
        } else {
            //val newFriends = listOf(user.friends);
            //user.friends
            //user.friends.add(user.name);
        }
    }

    LazyColumn(
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(30.dp)
    )
    {
        item {
            Column(
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.nico),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )
                Row() {
                    Text(
                        text = user.name,
                        modifier = Modifier.padding(0.dp, 0.dp, 5.dp),
                        fontSize = 18.sp,
                    )
                    if (sex == "Male") {
                        Icon(
                            painter = painterResource(R.drawable.male),
                            tint = Color.Unspecified,
                            contentDescription = "Gender",
                        )
                    } else if (sex == "Female") {
                        Icon(
                            painter = painterResource(R.drawable.male),
                            tint = Color.Unspecified,
                            contentDescription = "Gender",
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.male),
                            tint = Color.Unspecified,
                            contentDescription = "Gender",
                        )
                    }

                }
                Text(
                    text = user.description,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { addOrRemoveFriend() }, modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
                    if (user.friends.contains(user.name)) {
                        Text("Add friend")
                    } else {
                        Text("Remove friend")
                    }
                }
            }
        }
        item {
            Listing("Hobbies", hobbies)
        }
        item {
            Listing("Languages", languages)
        }
        item {
            ProfileInfoButton(
                field = "Location",
                value = user.location,
                R.drawable.home,
                Color.Green
            )
        }
        item {
            ProfileInfoButton(
                field = "Birthday",
                value = user.birthday,
                R.drawable.calendar,
                Color.Blue
            )
        }
        item {
            ProfileInfoButton(
                field = "Instagram",
                value = user.contactInfo[1],
                R.drawable.language,
                Color.Cyan
            )
        }
        item {
            ProfileInfoButton(
                field = "Phone number",
                value = user.contactInfo[0],
                R.drawable.phone,
                Color.Red
            )
        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable()
fun PreviewFriendDetailScreen() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            FriendDetailScreen(vm = CounterViewModel(),)
        }
    }
}
