package com.example.livemap.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.livemap.R

@Composable
fun EventInfoField(
    modifier: Modifier = Modifier,
    valor: String,
    onChange: (String) -> Unit,
    isEditing: Boolean = false,
    font: Font = Font(R.font.lexend_light, FontWeight.Normal),
    fontSize: TextUnit = 16.sp,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIconPainter: Painter? = null,
    width: Float = 1.0f,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    isPasswordVisible: Boolean = true,
) {

    val borderColor = if (!isEditing) Color.Transparent else Color.Black
    val value = if (!isPasswordVisible) "••••••" else valor

    BasicTextField(
        value = value,
        onValueChange = onChange,
        readOnly = !isEditing,
        modifier = modifier.fillMaxWidth(fraction = width).padding(vertical = 4.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily(font),
            fontSize = fontSize,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
        ),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = valor,
                innerTextField = {
                    // Manually place icon + field side by side
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = horizontalArrangement,
                        modifier = Modifier.padding(
                            if (!isEditing) PaddingValues(0.dp)
                            else PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        )
                    ) {
                        if (leadingIconPainter != null) {
                            Icon(
                                painter = leadingIconPainter,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        innerTextField()
                    }
                },
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = remember { MutableInteractionSource() },
                // No leadingIcon here — handled manually above
                leadingIcon = null,
                contentPadding = PaddingValues(0.dp), // zero since we handle it in the Row
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = remember { MutableInteractionSource() },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = borderColor,
                            disabledBorderColor = borderColor,
                            errorBorderColor = borderColor,
                        ),
                        shape = RoundedCornerShape(size = 12.dp),
                    )
                }
            )
        }
    )
}