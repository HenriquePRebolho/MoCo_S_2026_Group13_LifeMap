package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livemap.ui.theme.LiveMapTheme

@Composable()
fun EventDetailScreen(vm: CounterViewModel, event_id : Int) {
    val event = vm.event
    val user = vm.profile

    val inEvent : Boolean by remember { mutableStateOf(event.participants.contains(user.name)) }

    Column() {
        Image(
            painter = painterResource(R.drawable.park),
            contentDescription = "Event image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black)
        )

        Text(text = event.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth().padding(10.dp))
        Text(text = event.description, modifier = Modifier.padding(bottom = 10.dp, start = 5.dp, end = 5.dp))

        EventInfo(R.drawable.schedule, event.date + ", " + event.time_start + "-" + event.time_end)
        EventInfo(R.drawable.location_on, event.location)
        EventInfo(R.drawable.group,  ""+event.participants.count() + "/" + event.limitPeople + " joined")

        Text(text = "Created by: " + event.owner, modifier = Modifier.padding(start = 5.dp))
        Text(text = event.contactInfo.toString(), modifier = Modifier.padding(start = 5.dp))

        Listing("Tags", event.tags)
        Listing("Restrictions", event.restrictions)
        Listing("Languages", event.languages)
        Listing("Bring", event.bring)


        Button(onClick = {/*TODO*/}, modifier = Modifier.background(if (inEvent) Color.Red else Color.Green).fillMaxWidth()) {
            if (inEvent) {
                Text(text = "Leave")
            } else {
                Text(text = "Join")
            }
        }
    }
}



@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PreviewEventDetailScreen() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            EventDetailScreen(vm = CounterViewModel(), 0)
        }
    }
}