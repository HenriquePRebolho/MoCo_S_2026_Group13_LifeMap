package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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


@SuppressLint("UnrememberedMutableState")
@Composable
fun FriendsScreen(vm: CounterViewModel) {

    val user = vm.profile
    val event = vm.event
    val events = listOf(event, event, event)
    var searching_new_friends by remember { mutableStateOf(false) }

    Column() {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 10.dp, start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.search),
                tint = Color.Black,
                contentDescription = null
            )
            OutlinedTextField(state = rememberTextFieldState(initialText = ""), leadingIcon = {R.drawable.search}, label = {Text("Search new friend")})
            if (searching_new_friends) {
                Icon(
                    painter = painterResource(R.drawable.cancel),
                    tint = Color.Black,
                    contentDescription = null
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            items(events.size) { index ->
                if (!searching_new_friends) {
                    FriendInfoButton(user)
                } else {
                    NewFriendInfoButton(user)
                }
            }
        }
    }
}



@Composable
fun FriendInfoButton(user: User) {
    Surface(shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth(0.9f).height(90.dp).padding(bottom = 10.dp)
    ){
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.nico),
                tint = Color.Unspecified,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .size(60.dp)
                    .clip(CircleShape)
            )
            Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}



@Composable
fun NewFriendInfoButton(user: User) {
    Surface(shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth(0.9f).height(90.dp).padding(bottom = 10.dp)
    ){
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.nico),
                tint = Color.Unspecified,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .size(60.dp)
                    .clip(CircleShape)
            )
            Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(0.7f))
            Button(onClick = { /*TODO*/ }) {
                Text(text = "Add")
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