package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DskCobalt
import com.example.ui.theme.DskCyan

@Composable
fun SplashOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(durationMillis = 360))
    ) {
        var started by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { started = true }

        val logoScale by animateFloatAsState(
            targetValue = if (started) 1f else 0.72f,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 240f),
            label = "splash_logo_scale"
        )
        val textAlpha by animateFloatAsState(
            targetValue = if (started) 1f else 0f,
            animationSpec = tween(durationMillis = 550),
            label = "splash_text_alpha"
        )
        val pulse = rememberInfiniteTransition(label = "splash_pulse")
        val glowScale by pulse.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "splash_glow"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("splash_overlay")
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0E1D3B),
                            Color(0xFF070B14)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .scale(glowScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(DskCyan.copy(alpha = 0.28f), Color.Transparent)
                                )
                            )
                    )
                    Image(
                        painter = painterResource(id = R.drawable.img_dsk_brand_emblem),
                        contentDescription = "DSK CHROME Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(112.dp)
                            .scale(logoScale)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(DskCyan, DskCobalt)),
                                shape = CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = "DSK CHROME",
                    style = MaterialTheme.typography.displayMedium.copy(
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.alpha(textAlpha)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "BY DSK OFFICIAL",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 3.sp
                    ),
                    color = DskCyan.copy(alpha = 0.88f),
                    modifier = Modifier.alpha(textAlpha)
                )
            }
        }
    }
}
