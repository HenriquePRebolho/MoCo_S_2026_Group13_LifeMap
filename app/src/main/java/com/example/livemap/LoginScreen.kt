package com.example.livemap

// Importaciones necesarias de Jetpack Compose
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livemap.ui.theme.LiveMapTheme


// Pantalla principal de Login
@Composable
fun LoginScreen() {

    // Variable para guardar lo que escribe el usuario en Username
    var username by remember { mutableStateOf("") }

    // Variable para guardar lo que escribe en Password
    var password by remember { mutableStateOf("") }

    // LazyColumn = columna vertical con scroll
    LazyColumn(

        // Ocupa toda la pantalla
        modifier = Modifier.fillMaxSize(),

        // Centra horizontalmente todos los elementos
        horizontalAlignment = Alignment.CenterHorizontally,

        // Margen interior de 30dp
        contentPadding = PaddingValues(30.dp),

        // Coloca el contenido centrado verticalmente
        verticalArrangement = Arrangement.Center

    ) {

        // Título superior
        item {
            Text(
                text = "Welcome Back",
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 30.dp)
            )
        }

        // Caja de texto para Username
        item {
            OutlinedTextField(

                // Valor escrito actualmente
                value = username,

                // Cada vez que escribe cambia la variable
                onValueChange = { username = it },

                // Texto flotante dentro del campo
                label = { Text("Username") },

                // Ocupa todo el ancho disponible
                modifier = Modifier.fillMaxWidth(),

                // Bordes redondeados
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Espacio entre cajas
        item {
            Spacer(modifier = Modifier.height(15.dp))
        }

        // Caja de texto para Password
        item {
            OutlinedTextField(

                // Texto actual de contraseña
                value = password,

                // Guarda lo que escribe el usuario
                onValueChange = { password = it },

                // Texto del campo
                label = { Text("Password") },

                // Oculta la contraseña con puntos
                visualTransformation = PasswordVisualTransformation(),

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(12.dp)
            )
        }

        // Espacio antes de botones
        item {
            Spacer(modifier = Modifier.height(25.dp))
        }

        // Botón LOGIN
        item {
            Button(

                // Acción al pulsar (vacía por ahora)
                onClick = { },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),

                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log In")
            }
        }

        // Espacio entre botones
        item {
            Spacer(modifier = Modifier.height(15.dp))
        }

        // Botón REGISTER
        item {
            Button(

                // Acción al pulsar
                onClick = { },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),

                shape = RoundedCornerShape(12.dp),

                // Cambia color del botón
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Register")
            }
        }
    }
}


// Vista previa en Android Studio
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {

    // Aplica el tema de la app
    LiveMapTheme(dynamicColor = false) {

        // Fondo de la pantalla
        Surface(color = MaterialTheme.colorScheme.background) {

            // Muestra pantalla Login
            LoginScreen()
        }
    }
}

