package com.elocho.snooker.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.theme.Burgundy
import com.elocho.snooker.ui.theme.BurgundyDark
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.PureWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(600)
        )
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500)
        )
        delay(1200)
        onSplashComplete()
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(400))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Burgundy, BurgundyDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(
                height = 80.dp,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SNOOKER LOUNGE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 8.sp,
                color = Gold.copy(alpha = 0.85f),
                modifier = Modifier.alpha(subtitleAlpha.value),
                textAlign = TextAlign.Center
            )
        }
    }
}
