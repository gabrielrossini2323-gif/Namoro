package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Configured vibrant dual-tone gradients for premium visual style
val GradientThemes = listOf(
    // 0: Dev/Neutral
    listOf(Color(0xFFFF5F6D), Color(0xFFFFC371)),
    // 1: Mariana (Designer/Pink-Purple)
    listOf(Color(0xFFEA80FC), Color(0xFF3D5AFE)),
    // 2: Rodrigo (Photographer/Teal-Indigo)
    listOf(Color(0xFF00B0FF), Color(0xFF651FFF)),
    // 3: Camila (Chef/Green-Teal)
    listOf(Color(0xFF00E676), Color(0xFF00B0FF)),
    // 4: Lucas (Musician/Orange-Yellow)
    listOf(Color(0xFFFF9100), Color(0xFFFF3D00)),
    // 5: Beatriz (Architect/Pink-Magenta)
    listOf(Color(0xFFFF1744), Color(0xFFF50057)),
    // 6: Gabriel (Trainer/Lime-Green)
    listOf(Color(0xFF76FF03), Color(0xFF00E5FF)),
    // 7: Juliana (Vet/Purple-Pink)
    listOf(Color(0xFFD500F9), Color(0xFFFF4081)),
    // 8: Felipe (Sommelier/Deep Grape-Burgundy)
    listOf(Color(0xFF4A148C), Color(0xFF880E4F))
)

val ThemeEmojis = listOf(
    "🧑‍💻", // 0: You (Dev)
    "👩‍🎨", // 1: Mariana (Designer)
    "📷", // 2: Rodrigo (Photographer)
    "👩‍🍳", // 3: Camila (Chef)
    "🎸", // 4: Lucas (Musician)
    "📐", // 5: Beatriz (Architect)
    "🏋️", // 6: Gabriel (Trainer)
    "🐈", // 7: Juliana (Vet)
    "🍷"  // 8: Felipe (Wine)
)

@Composable
fun UserAvatar(
    avatarIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    borderWidth: Dp = 3.dp,
    borderColor: Color = Color.White
) {
    val themeColors = GradientThemes.getOrElse(avatarIndex) { GradientThemes[0] }
    val emojiChar = ThemeEmojis.getOrElse(avatarIndex) { "💖" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = themeColors,
                    start = Offset(0f, 0f),
                    end = Offset(100f, 400f)
                )
            )
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Draw elegant, soft abstract geometric background vectors via Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size
            // Draw a subtle translucent inner overlay circle
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = canvasSize.minDimension / 2.5f,
                center = Offset(canvasSize.width * 0.2f, canvasSize.height * 0.2f)
            )
            // Draw a modern tech horizontal guideline
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, canvasSize.height * 0.7f),
                end = Offset(canvasSize.width, canvasSize.height * 0.7f),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Render themed character representation emojis with dynamic sizes
        Text(
            text = emojiChar,
            fontSize = (size.value * 0.45).sp,
            lineHeight = (size.value * 0.45).sp
        )
    }
}
