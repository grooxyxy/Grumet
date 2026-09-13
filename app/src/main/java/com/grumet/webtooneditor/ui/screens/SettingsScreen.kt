package com.grumet.webtooneditor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grumet.webtooneditor.domain.ApiSettings
import com.grumet.webtooneditor.domain.Language
import com.grumet.webtooneditor.domain.TranslationProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: ApiSettings,
    onSettingsChanged: (ApiSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Translation & Model Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Translation Provider",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    RadioButton(
                        selected = settings.provider == TranslationProvider.ML_KIT,
                        onClick = { onSettingsChanged(settings.copy(provider = TranslationProvider.ML_KIT)) }
                    )
                    Text("ML Kit (On-device / Free)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    RadioButton(
                        selected = settings.provider == TranslationProvider.AI_API,
                        onClick = { onSettingsChanged(settings.copy(provider = TranslationProvider.AI_API)) }
                    )
                    Text("AI Translation API Key (Custom/OpenAI/Gemini)")
                }
            }
        }

        if (settings.provider == TranslationProvider.AI_API) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "API Key Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = settings.apiKey,
                        onValueChange = { onSettingsChanged(settings.copy(apiKey = it)) },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )

                    OutlinedTextField(
                        value = settings.customEndpoint,
                        onValueChange = { onSettingsChanged(settings.copy(customEndpoint = it)) },
                        label = { Text("Endpoint URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )

                    OutlinedTextField(
                        value = settings.modelName,
                        onValueChange = { onSettingsChanged(settings.copy(modelName = it)) },
                        label = { Text("Model Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Language Pair",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Source Language:", style = MaterialTheme.typography.bodyMedium)
                DropdownLanguageSelector(
                    selectedLanguage = settings.sourceLanguage,
                    onLanguageSelected = { onSettingsChanged(settings.copy(sourceLanguage = it)) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Target Language:", style = MaterialTheme.typography.bodyMedium)
                DropdownLanguageSelector(
                    selectedLanguage = settings.targetLanguage,
                    onLanguageSelected = { onSettingsChanged(settings.copy(targetLanguage = it)) }
                )
            }
        }
    }
}

@Composable
fun DropdownLanguageSelector(
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedLanguage.displayName)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Language.values().forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}
