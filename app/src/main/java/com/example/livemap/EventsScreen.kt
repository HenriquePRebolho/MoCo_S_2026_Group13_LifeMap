package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livemap.ui.theme.LiveMapTheme

@Composable
fun EventsScreen(vm: CounterViewModel) {

    val event = vm.event
    val events = listOf(event, event, event)

    Column() { // TODO: fix scroll
        Row(modifier = Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 10.dp, start = 10.dp, end = 10.dp)) {
            Button(
                onClick = { vm.visibilityOwned = !vm.visibilityOwned },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Owned Events", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Icon(
                    painter = painterResource(if (vm.visibilityOwned) R.drawable.stat_minus else R.drawable.stat),
                    tint = Color.White,
                    contentDescription = null
                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            items(events.size) { index -> // TODO: show by date-time
                if (vm.visibilityOwned) {
                    EventInfoButton(event)
                }
            }
        }


        Row(modifier = Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 10.dp, start = 10.dp, end = 10.dp)) {
            Button(
                onClick = { vm.visibilityEvent = !vm.visibilityEvent },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Joined Events", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Icon(
                    painter = painterResource(if (vm.visibilityOwned) R.drawable.stat_minus else R.drawable.stat),
                    tint = Color.White,
                    contentDescription = null
                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(events.size) { index ->
                if (vm.visibilityEvent) {
                    EventInfoButton(event)
                }
            }
        }
    }
}



@Composable()
fun EventInfo(icon: Int, text: String) {
    Row() {
        Icon(painter = painterResource(icon),
            tint = Color.LightGray,
            contentDescription = null,
            modifier = Modifier.padding(end = 7.dp, bottom = 2.dp)
        )
        Text(text = text, fontWeight = FontWeight.Normal, modifier = Modifier.fillMaxWidth())
    }
}


@Composable
fun EventInfoButton(event: UiEvent) { // TODO: change event variable
    Surface(shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth(0.9f).height(140.dp).padding(bottom = 10.dp),
        // onClick = actionStartActivity<EventDetailScreen>(vm = CounterViewModel, id = event.id) // TODO: send to correct EventDetaiScreen
    ){
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.Center) {
                Text(text = event.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp))

                EventInfo(R.drawable.schedule, event.date + ", " + event.timeStart + "-" + event.timeEnd)

                EventInfo(R.drawable.location_on, event.location)

                EventInfo(R.drawable.group,  ""+event.participants.count() + "/" + event.limitPeople + " joined")
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