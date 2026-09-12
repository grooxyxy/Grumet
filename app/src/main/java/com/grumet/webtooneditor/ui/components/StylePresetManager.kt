package com.grumet.webtooneditor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grumet.webtooneditor.domain.TextStyleConfig

@Composable
fun StylePresetManager(
    currentStyle: TextStyleConfig,
    onApplyPreset: (TextStyleConfig) -> Unit
) {
    var presets by remember {
        mutableStateOf(
            listOf(
                TextStyleConfig(name = "Webtoon Standard", fontSizeSp = 16f, textColorHex = "#000000", isBold = false),
                TextStyleConfig(name = "Shout Bold", fontSizeSp = 22f, textColorHex = "#FF0000", isBold = true, shadowBlurPx = 8f, shadowColorHex = "#000000"),
                TextStyleConfig(name = "Whisper Italic", fontSizeSp = 14f, textColorHex = "#4A4A4A", isItalic = true),
                TextStyleConfig(name = "Dark Fantasy", fontSizeSp = 18f, textColorHex = "#FFFFFF", isBold = true, shadowBlurPx = 10f, shadowColorHex = "#800080")
            )
        )
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Style Presets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = { showSaveDialog = true }) {
                Text("+ Save Current")
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            items(presets) { preset ->
                OutlinedButton(
                    onClick = { onApplyPreset(preset.copy(id = currentStyle.id)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(preset.name)
                }
            }
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Style Preset") },
                text = {
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        label = { Text("Preset Name") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (presetNameInput.isNotBlank()) {
                                presets = presets + currentStyle.copy(name = presetNameInput)
                                presetNameInput = ""
                                showSaveDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
