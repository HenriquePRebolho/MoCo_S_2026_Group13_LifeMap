package com.example.livemap.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.livemap.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    label: String,
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: List<String>,
    modifier: Modifier = Modifier,
    onFriendClicked: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = textFieldState.text.toString(),
            onValueChange = { text ->
                textFieldState.edit { replace(0, length, text) }
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { expanded = it.isFocused },
            label = { Text(label) },
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Icon(painterResource(R.drawable.search), contentDescription = null)
            },
            singleLine = true
        )
        if (expanded) {
            val filteredResults = searchResults.filter {
                it.contains(textFieldState.text.toString(), ignoreCase = true)
            }
            if (filteredResults.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredResults) { result ->
                            ListItem(
                                headlineContent = { Text(result) },
                                modifier = Modifier.clickable {
                                    textFieldState.edit { replace(0, length, "") }
                                    onFriendClicked(result)
                                    expanded = false
                                    onSearch(result)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SearchResultField(
    valor: String,
    onRemoveFriendClicked: (String) -> Unit,
    isEditing: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valor.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = valor,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            if (isEditing) {
                IconButton(onClick = { onRemoveFriendClicked(valor) }) {
                    Icon(
                        painterResource(R.drawable.cancel),
                        contentDescription = "Remove friend",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}