package com.elocho.snooker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun DiagonalStripeBackground(
    modifier: Modifier = Modifier,
    color1: Color = Color(0xFF882546),
    color2: Color = Color(0xFF561230)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        color1,
                        color1.copy(alpha = 0.92f),
                        color2.copy(alpha = 0.95f),
                        color2
                    )
                )
            )
    )
}
