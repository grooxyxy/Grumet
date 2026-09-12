package com.grumet.webtooneditor.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grumet.webtooneditor.domain.TextBubble
import com.grumet.webtooneditor.domain.TextStyleConfig
import com.grumet.webtooneditor.domain.WebtoonPage
import com.grumet.webtooneditor.ui.components.StylePresetManager
import com.grumet.webtooneditor.ui.components.TextStylingControls

enum class ExportMode {
    TRANSLATED_IMAGE,
    CLEAN_IMAGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    pages: List<WebtoonPage>,
    currentPageIndex: Int,
    onPageIndexChanged: (Int) -> Unit,
    onPagesUpdated: (List<WebtoonPage>) -> Unit
) {
    if (pages.isEmpty() || currentPageIndex !in pages.indices) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No webtoon page selected. Add images in the Queue tab first.")
        }
        return
    }

    val page = pages[currentPageIndex]
    var exportMode by remember { mutableStateOf(ExportMode.TRANSLATED_IMAGE) }
    var selectedBubbleId by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val selectedBubble = page.bubbles.find { it.id == selectedBubbleId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page ${currentPageIndex + 1} of ${pages.size}") },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        FilterChip(
                            selected = exportMode == ExportMode.TRANSLATED_IMAGE,
                            onClick = { exportMode = ExportMode.TRANSLATED_IMAGE },
                            label = { Text("Translated") }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = exportMode == ExportMode.CLEAN_IMAGE,
                            onClick = { exportMode = ExportMode.CLEAN_IMAGE },
                            label = { Text("Clean Image") }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main Interactive Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF222222))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 4f)
                            offset += pan
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = if (exportMode == ExportMode.CLEAN_IMAGE && page.cleanedBitmap != null) {
                    page.cleanedBitmap!!
                } else {
                    page.originalBitmap
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Webtoon Page",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Draw Speech Bubbles overlay if translated mode is active
                    if (exportMode == ExportMode.TRANSLATED_IMAGE) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasW = size.width
                            val canvasH = size.height

                            for (bubble in page.bubbles) {
                                val bx = bubble.x * canvasW
                                val by = bubble.y * canvasH
                                val bw = bubble.width * canvasW
                                val bh = bubble.height * canvasH

                                val isSelected = bubble.id == selectedBubbleId

                                // Draw speech bubble bounding box
                                drawRoundRect(
                                    color = if (isSelected) Color(0xFF3B82F6) else Color(0x88FF5722),
                                    topLeft = Offset(bx, by),
                                    size = Size(bw, bh),
                                    cornerRadius = CornerRadius(8f, 8f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isSelected) 4f else 2f)
                                )
                            }
                        }

                        // Text overlays
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val maxW = maxWidth
                            val maxH = maxHeight

                            for (bubble in page.bubbles) {
                                val isSelected = bubble.id == selectedBubbleId

                                val bubbleStyle = bubble.style

                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = maxW * bubble.x,
                                            y = maxH * bubble.y
                                        )
                                        .size(
                                            width = maxW * bubble.width,
                                            height = maxH * bubble.height
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) Color.Blue else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedBubbleId = bubble.id }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = bubble.translatedText.ifEmpty { bubble.originalText },
                                        style = TextStyle(
                                            color = try { Color(AndroidColor.parseColor(bubbleStyle.textColorHex)) } catch (e: Exception) { Color.Black },
                                            fontSize = bubbleStyle.fontSizeSp.sp,
                                            fontWeight = if (bubbleStyle.isBold) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            shadow = if (bubbleStyle.shadowBlurPx > 0) {
                                                Shadow(
                                                    color = try { Color(AndroidColor.parseColor(bubbleStyle.shadowColorHex)) } catch (e: Exception) { Color.Black },
                                                    offset = Offset(bubbleStyle.shadowOffsetX, bubbleStyle.shadowOffsetY),
                                                    blurRadius = bubbleStyle.shadowBlurPx
                                                )
                                            } else null
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Text Editing Panel
            if (selectedBubble != null && exportMode == ExportMode.TRANSLATED_IMAGE) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Edit Text & Style",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = selectedBubble.translatedText,
                            onValueChange = { newText ->
                                selectedBubble.translatedText = newText
                                onPagesUpdated(pages.toList())
                            },
                            label = { Text("Translated Text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )

                        TextStylingControls(
                            style = selectedBubble.style,
                            onStyleChanged = { newStyle ->
                                selectedBubble.style = newStyle
                                onPagesUpdated(pages.toList())
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        StylePresetManager(
                            currentStyle = selectedBubble.style,
                            onApplyPreset = { presetStyle ->
                                selectedBubble.style = presetStyle
                                onPagesUpdated(pages.toList())
                            }
                        )
                    }
                }
            }
        }
    }
}
