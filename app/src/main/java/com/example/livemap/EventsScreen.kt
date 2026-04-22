package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.livemap.ui.theme.LiveMapTheme

@Composable
fun EventsScreen(vm: CounterViewModel) {

    val event = vm.event
    val events = listOf(event, event, event)

    var visibility_owned : Boolean by remember { mutableStateOf(true) }
    var visibility_joined : Boolean by remember { mutableStateOf(true) }

    Column() {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(

                onClick = { visibility_owned = !visibility_owned }
            ) { Text("Owned Events")
                //Icon(imageVector = R.drawable.icons_calendar, contentDescription = "Owned Events button")
            }
        }
        LazyColumn(modifier = Modifier.onVisibilityChanged { visible -> visibility_owned = visible },
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(events.size) { index ->
                val num_joined = events[index].participants.size
                val ev = events[index]
                if (visibility_owned) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.drawBehind() {
                            val indicatorWidth = null
                            val strokeWidth = 50 * density
                            val y = size.height - strokeWidth / 2

                            drawLine(
                                Color.LightGray,
                                Offset(0f, y),
                                Offset(size.width, y),
                                strokeWidth
                            )
                        }) {
                        Text(ev.name)
                        Text(ev.date)
                        Text("${num_joined}/${ev.limitPeople}")
                        TextButton(
                            onClick = { } // send to event detail
                        ) { Text("Details", color = Color.Green)
                            //Icon(imageVector = R.drawable.icons_calendar, contentDescription = "Owned Events button")
                        }
                    }
                }
            }
        }
        Row() {
            TextButton(

                onClick = { visibility_joined = !visibility_joined }
            ) { Text("Joined Events") }
        }
        LazyColumn(modifier = Modifier.onVisibilityChanged { visible -> visibility_joined = visible },
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(events.size) { index ->
                val num_joined = events[index].participants.size
                val ev = events[index]
                if (visibility_joined) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.drawBehind() {
                            val indicatorWidth = null
                            val strokeWidth = 50 * density
                            val y = size.height - strokeWidth / 2

                            drawLine(
                                Color.LightGray,
                                Offset(0f, y),
                                Offset(size.width, y),
                                strokeWidth
                            )
                        }) {
                        Text(ev.name)
                        Text(ev.date)
                        Text("${num_joined}/${ev.limitPeople}")
                        TextButton(
                            onClick = { } // send to event detail
                        ) { Text("Details", color = Color.Green)
                            //Icon(imageVector = R.drawable.icons_calendar, contentDescription = "Owned Events button")
                        }
                    }
                }
            }
        }
    }



}

private fun RowScope.Icon(imageVector: Int, contentDescription: String) {
        TODO("Not yet implemented")
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