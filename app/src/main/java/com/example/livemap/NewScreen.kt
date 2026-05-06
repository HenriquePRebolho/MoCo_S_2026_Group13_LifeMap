package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun NewScreen(vm: CounterViewModel) {

    val event = vm.event
    val events = listOf(event, event, event)

    Column() {
        Button(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Text(text = "Create new event", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Icon(painter = painterResource(R.drawable.add_circle),
                tint = Color.White,
                contentDescription = null)
        }
        // TODO search bar
        OutlinedTextField(state = rememberTextFieldState(initialText = ""), label = {Text("Search bar")})

        LazyRow() {

        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(events.size) { index ->
                if (vm.visibilityOwned) {
                    EventInfoButton(event)
                }
            }
        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PreviewNewScreen() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NewScreen(vm = CounterViewModel())
        }
    }
}