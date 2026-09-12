package com.grumet.webtooneditor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.grumet.webtooneditor.domain.ApiSettings
import com.grumet.webtooneditor.domain.WebtoonPage
import com.grumet.webtooneditor.ui.screens.EditorScreen
import com.grumet.webtooneditor.ui.screens.QueueScreen
import com.grumet.webtooneditor.ui.screens.SettingsScreen

enum class AppScreen {
    QUEUE,
    EDITOR,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebtoonApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.QUEUE) }
    var pages by remember { mutableStateOf<List<WebtoonPage>>(emptyList()) }
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var apiSettings by remember { mutableStateOf(ApiSettings()) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.QUEUE,
                    onClick = { currentScreen = AppScreen.QUEUE },
                    icon = { Icon(Icons.Default.List, contentDescription = "Queue") },
                    label = { Text("Queue") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.EDITOR,
                    onClick = { currentScreen = AppScreen.EDITOR },
                    icon = { Icon(Icons.Default.Brush, contentDescription = "Editor") },
                    label = { Text("Editor") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = { currentScreen = AppScreen.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.QUEUE -> QueueScreen(
                    pages = pages,
                    onPagesUpdated = { pages = it },
                    apiSettings = apiSettings,
                    onOpenEditor = { pageIndex ->
                        selectedPageIndex = pageIndex
                        currentScreen = AppScreen.EDITOR
                    }
                )
                AppScreen.EDITOR -> EditorScreen(
                    pages = pages,
                    currentPageIndex = selectedPageIndex,
                    onPageIndexChanged = { selectedPageIndex = it },
                    onPagesUpdated = { pages = it }
                )
                AppScreen.SETTINGS -> SettingsScreen(
                    settings = apiSettings,
                    onSettingsChanged = { apiSettings = it }
                )
            }
        }
    }
}
