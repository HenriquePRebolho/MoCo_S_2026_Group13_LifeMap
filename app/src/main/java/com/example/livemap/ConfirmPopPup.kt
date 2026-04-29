package com.example.livemap

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.livemap.ui.theme.LiveMapTheme
import java.nio.file.WatchEvent

@Composable()
fun ConffirmPopPup(vm: CounterViewModel, text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 5.dp,
    ){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text)
            Row() {
                Button(onClick = {null}, modifier = Modifier.padding(start = 10.dp, end = 5.dp)) {Text("Confirm")}
                Button(onClick = {null}, modifier = Modifier.padding(end = 10.dp, start = 5.dp)) {Text("Cancel")}
            }
        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable()
fun PreviewConfirmPopPup() {
    LiveMapTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ConffirmPopPup(vm = CounterViewModel(), "Are you sure?")
        }
    }
}