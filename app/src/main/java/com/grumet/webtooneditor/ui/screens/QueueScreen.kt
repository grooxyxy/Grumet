package com.grumet.webtooneditor.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grumet.webtooneditor.domain.ApiSettings
import com.grumet.webtooneditor.domain.ProcessStatus
import com.grumet.webtooneditor.domain.WebtoonPage
import com.grumet.webtooneditor.ml.ProcessingPipeline
import kotlinx.coroutines.launch

@Composable
fun QueueScreen(
    pages: List<WebtoonPage>,
    onPagesUpdated: (List<WebtoonPage>) -> Unit,
    apiSettings: ApiSettings,
    onOpenEditor: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pipeline = remember { ProcessingPipeline(context) }
    var isProcessing by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val newPages = uris.mapNotNull { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    WebtoonPage(originalBitmap = bitmap)
                }
            } catch (e: Exception) {
                null
            }
        }
        if (newPages.isNotEmpty()) {
            onPagesUpdated(pages + newPages)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Webtoon Queue & Auto Process",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Images")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Images")
            }

            Button(
                onClick = {
                    if (pages.isNotEmpty() && !isProcessing) {
                        isProcessing = true
                        coroutineScope.launch {
                            pipeline.setPages(pages)
                            pipeline.processAllPages(apiSettings)
                            onPagesUpdated(pipeline.pagesFlow.value)
                            isProcessing = false
                            if (pages.isNotEmpty()) {
                                onOpenEditor(0)
                            }
                        }
                    }
                },
                enabled = pages.isNotEmpty() && !isProcessing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Process All")
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isProcessing) "Processing..." else "Process All")
            }
        }

        if (pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No webtoon pages added yet.\nClick 'Add Images' to begin batch processing.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(pages) { index, page ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEditor(index) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = page.originalBitmap.asImageBitmap(),
                                contentDescription = "Webtoon Page ${index + 1}",
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Page ${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bubbles detected: ${page.bubbles.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                when (page.status) {
                                    ProcessStatus.QUEUED -> Text("Status: Queued", color = MaterialTheme.colorScheme.secondary)
                                    ProcessStatus.PROCESSING -> {
                                        LinearProgressIndicator(
                                            progress = { page.progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        )
                                    }
                                    ProcessStatus.COMPLETED -> Text("Status: Done ✓", color = MaterialTheme.colorScheme.primary)
                                    ProcessStatus.FAILED -> Text("Status: Failed (${page.errorMessage})", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            IconButton(
                                onClick = {
                                    val updated = pages.toMutableList().apply { removeAt(index) }
                                    onPagesUpdated(updated)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
