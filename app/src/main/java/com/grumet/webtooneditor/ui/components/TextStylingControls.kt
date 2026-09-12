package com.grumet.webtooneditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grumet.webtooneditor.domain.TextStyleConfig

@Composable
fun TextStylingControls(
    style: TextStyleConfig,
    onStyleChanged: (TextStyleConfig) -> Unit
) {
    val colorOptions = listOf(
        "#000000", "#FFFFFF", "#FF0000", "#00FF00",
        "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Font Size: ${style.fontSizeSp.toInt()} sp", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = style.fontSizeSp,
            onValueChange = { onStyleChanged(style.copy(fontSizeSp = it)) },
            valueRange = 8f..48f
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = style.isBold,
                onClick = { onStyleChanged(style.copy(isBold = !style.isBold)) },
                label = { Text("B", fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = style.isItalic,
                onClick = { onStyleChanged(style.copy(isItalic = !style.isItalic)) },
                label = { Text("I") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Text Color", style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            colorOptions.forEach { hex ->
                val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (style.textColorHex.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                            color = if (style.textColorHex.equals(hex, ignoreCase = true)) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onStyleChanged(style.copy(textColorHex = hex)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Shadow Blur: ${style.shadowBlurPx.toInt()} px", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = style.shadowBlurPx,
            onValueChange = { onStyleChanged(style.copy(shadowBlurPx = it)) },
            valueRange = 0f..20f
        )
    }
}
