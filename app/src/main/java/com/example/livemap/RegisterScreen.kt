package com.example.livemap

// =====================================================
// IMPORTACIONES
// =====================================================

// Detectar clicks
import androidx.compose.foundation.clickable

// Layouts y estructuras visuales
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

// Formas redondeadas
import androidx.compose.foundation.shape.RoundedCornerShape

// Columna con scroll
import androidx.compose.foundation.lazy.LazyColumn

// =====================================================
// COMPONENTES MATERIAL3
// =====================================================

import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState

// =====================================================
// ESTADOS DINÁMICOS
// =====================================================

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// =====================================================
// PASSWORD
// =====================================================

// Oculta contraseña
import androidx.compose.ui.text.input.PasswordVisualTransformation

// Permite mostrar contraseña normal
import androidx.compose.ui.text.input.VisualTransformation

// =====================================================
// FECHA
// =====================================================

// Convierte fecha a formato legible
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Tema app
import com.example.livemap.ui.theme.LiveMapTheme



// =====================================================
// REGISTER SCREEN
// =====================================================

@Composable
fun RegisterScreen() {

    // =====================================================
    // VARIABLES
    // =====================================================

    // Nombre
    var name by remember {
        mutableStateOf("")
    }

    // Descripción
    var description by remember {
        mutableStateOf("")
    }

    // Sexo seleccionado
    // Empieza vacío
    var sex by remember {
        mutableStateOf("")
    }

    // =====================================================
    // HOBBIES Y LANGUAGES
    // =====================================================

    // Texto que escribe usuario
    var hobbiesText by remember {
        mutableStateOf("")
    }

    var languagesText by remember {
        mutableStateOf("")
    }

    // =====================================================
    // CONVERSIÓN AUTOMÁTICA A LISTAS
    // =====================================================

    // El usuario escribe:
    // football, music, games
    //
    // Se convierte automáticamente en:
    // ["football", "music", "games"]

    val hobbies = hobbiesText
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val languages = languagesText
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    // Location
    var location by remember {
        mutableStateOf("")
    }

    // Birthday
    var birthday by remember {
        mutableStateOf("")
    }

    // Instagram
    var instagram by remember {
        mutableStateOf("")
    }

    // Phone
    var phone by remember {
        mutableStateOf("")
    }

    // Email
    var email by remember {
        mutableStateOf("")
    }

    // Password
    var password by remember {
        mutableStateOf("")
    }

    // =====================================================
    // PASSWORD VISIBLE
    // =====================================================

    // true -> visible
    // false -> oculta
    var passwordVisible by remember {
        mutableStateOf(false)
    }

    // =====================================================
    // DROPDOWN SEX
    // =====================================================

    // Controla si menú está abierto
    var expanded by remember {
        mutableStateOf(false)
    }

    // Opciones disponibles
    val sexOptions = listOf(
        "Male",
        "Female",
        "Prefer not to say"
    )

    // =====================================================
    // DATE PICKER
    // =====================================================

    // Controla si calendario está abierto
    var showDatePicker by remember {
        mutableStateOf(false)
    }



    // =====================================================
    // CONTENEDOR PRINCIPAL
    // =====================================================

    LazyColumn(

        modifier = Modifier.fillMaxSize(),

        horizontalAlignment = Alignment.CenterHorizontally,

        contentPadding = PaddingValues(25.dp),

        verticalArrangement = Arrangement.Top

    ) {

        // =====================================================
        // TITLE
        // =====================================================

        item {

            Text(

                text = "Register",

                style = MaterialTheme.typography.headlineMedium,

                modifier = Modifier.padding(bottom = 20.dp)
            )
        }



        // =====================================================
        // NAME
        // =====================================================

        item {

            CampoTexto(

                label = "Name",

                valor = name,

                onChange = {
                    name = it
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // DESCRIPTION
        // =====================================================

        item {

            CampoTexto(

                label = "Description",

                valor = description,

                onChange = {
                    description = it
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // SEX DESPLEGABLE
        // =====================================================

        item {

            // =================================================
            // BOX PARA DETECTAR CLICK
            // =================================================

            Box(

                modifier = Modifier.fillMaxWidth()

            ) {

                // =================================================
                // CAMPO SEX
                // =================================================

                OutlinedTextField(

                    value = sex,

                    onValueChange = {},

                    readOnly = true,

                    // IMPORTANTE:
                    // Desactiva captura interna
                    enabled = false,

                    label = {
                        Text("Sex")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(12.dp)
                )

                // =================================================
                // CAPA INVISIBLE PARA DETECTAR CLICK
                // =================================================

                Box(

                    modifier = Modifier

                        .matchParentSize()

                        .clickable {

                            expanded = true
                        }
                )

                // =================================================
                // MENÚ DESPLEGABLE
                // =================================================

                DropdownMenu(

                    expanded = expanded,

                    onDismissRequest = {

                        expanded = false
                    }

                ) {

                    sexOptions.forEach { option ->

                        DropdownMenuItem(

                            text = {
                                Text(option)
                            },

                            onClick = {

                                // Guarda opción
                                sex = option

                                // Cierra menú
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // HOBBIES
        // =====================================================

        item {

            CampoTexto(

                label = "Hobbies",

                valor = hobbiesText,

                onChange = {

                    hobbiesText = it
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // LANGUAGES
        // =====================================================

        item {

            CampoTexto(

                label = "Languages",

                valor = languagesText,

                onChange = {

                    languagesText = it
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // LOCATION
        // =====================================================

        item {

            CampoTexto(

                label = "Location",

                valor = location,

                onChange = {
                    location = it
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // BIRTHDAY
        // =====================================================

        item {

            // =================================================
            // BOX PARA DETECTAR CLICK
            // =================================================

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    // =================================================
                    // AL PULSAR:
                    // ABRE CALENDARIO
                    // =================================================
                    .clickable {

                        showDatePicker = true
                    }

            ) {

                // =================================================
                // CAMPO FECHA
                // =================================================

                OutlinedTextField(

                    value = birthday,

                    onValueChange = {},

                    readOnly = true,

                    enabled = false,

                    label = {
                        Text("Birthday")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // INSTAGRAM
        // =====================================================

        item {

            CampoTexto(

                label = "Instagram",

                valor = instagram,

                onChange = {
                    instagram = it
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // PHONE
        // =====================================================

        item {

            CampoTexto(

                label = "Phone Number",

                valor = phone,

                onChange = {
                    phone = it
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // EMAIL
        // =====================================================

        item {

            CampoTexto(

                label = "Email",

                valor = email,

                onChange = {
                    email = it
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }



        // =====================================================
        // PASSWORD
        // =====================================================

        item {

            OutlinedTextField(

                value = password,

                onValueChange = {

                    password = it
                },

                label = {
                    Text("Password")
                },

                // =================================================
                // SI passwordVisible:
                // muestra contraseña
                //
                // SI NO:
                // oculta contraseña
                // =================================================
                visualTransformation =

                    if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(12.dp),

                // =================================================
                // BOTÓN SHOW / HIDE
                // =================================================
                trailingIcon = {

                    TextButton(

                        onClick = {

                            passwordVisible =
                                !passwordVisible
                        }

                    ) {

                        if (passwordVisible)
                            Text("Hide")
                        else
                            Text("Show")
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }



        // =====================================================
        // BOTÓN FINAL
        // =====================================================

        item {

            Button(

                onClick = {

                    // hobbies -> List<String>
                    // languages -> List<String>
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),

                shape = RoundedCornerShape(12.dp)

            ) {

                Text("Create Account")
            }
        }
    }



    // =====================================================
    // DATE PICKER DIALOG
    // =====================================================

    if (showDatePicker) {

        // Estado interno calendario
        val datePickerState =
            rememberDatePickerState()

        DatePickerDialog(

            onDismissRequest = {

                showDatePicker = false
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        // Fecha seleccionada
                        val selectedDate =
                            datePickerState
                                .selectedDateMillis

                        if (selectedDate != null) {

                            // Formato fecha
                            val formatter =
                                SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    Locale.getDefault()
                                )

                            // Convierte fecha
                            birthday =
                                formatter.format(
                                    Date(selectedDate)
                                )
                        }

                        // Cierra calendario
                        showDatePicker = false
                    }

                ) {

                    Text("OK")
                }
            }

        ) {

            // Calendario visual
            DatePicker(
                state = datePickerState
            )
        }
    }
}



// =====================================================
// COMPONENTE REUTILIZABLE
// =====================================================

@Composable
fun CampoTexto(

    label: String,

    valor: String,

    onChange: (String) -> Unit

) {

    OutlinedTextField(

        value = valor,

        onValueChange = onChange,

        label = {
            Text(label)
        },

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(12.dp)
    )
}



// =====================================================
// PREVIEW
// =====================================================

@Preview(showBackground = true)
@Composable
fun PreviewRegisterScreen() {

    LiveMapTheme(dynamicColor = false) {

        Surface(

            color =
                MaterialTheme.colorScheme.background

        ) {

            RegisterScreen()
        }
    }
}