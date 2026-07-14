package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LudoSplashScreen(
    modifier: Modifier = Modifier
) {
    // 8-second animated linear progress
    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 8000, easing = LinearEasing)
        )
    }

    // Dynamic loading text cycle to look super professional
    var loadingText by remember { mutableStateOf("PREPARING BOARD...") }
    LaunchedEffect(Unit) {
        delay(1500)
        loadingText = "ROLLING GOLDEN DICE..."
        delay(1500)
        loadingText = "CONNECTING TO LOBBY..."
        delay(1500)
        loadingText = "ALIGNING TOKENS..."
        delay(1500)
        loadingText = "READYING CHAMPIONSHIP..."
    }

    // Pulsing and scaling crown animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash_crown")
    val crownScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_scale"
    )
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Deep Midnight Slate
                        Color(0xFF0A0F24), // Royal Navy Blue
                        Color(0xFF030712)  // Deepest Charcoal Black
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Luxury Golden Geometric Line Grid
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.06f)) {
            val step = 60.dp.toPx()
            for (i in -10..20) {
                drawLine(
                    color = Color(0xFFFFD700),
                    start = androidx.compose.ui.geometry.Offset(i * step, 0f),
                    end = androidx.compose.ui.geometry.Offset((i + 10) * step, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFFFD700),
                    start = androidx.compose.ui.geometry.Offset(i * step, 0f),
                    end = androidx.compose.ui.geometry.Offset((i - 10) * step, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Main Splash Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Giant luxury circular board design representing the uploaded layout
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        scaleX = crownScale
                        scaleY = crownScale
                    }
                    .shadow(24.dp * glowIntensity, CircleShape, spotColor = Color(0xFFFFD700))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E3A8A).copy(alpha = 0.9f), // Glowing Royal Blue center
                                Color(0xFF0B132B).copy(alpha = 0.98f) // Sinking Deep Navy
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 4.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFDF73), // Shiny Gold Highlight
                                Color(0xFFD4AF37), // Pure Metallic Gold
                                Color(0xFF8A640F), // Deep Antique Gold
                                Color(0xFFFFDF73)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer 3D gold highlight aura rings
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.92f)
                        .border(1.2.dp, Color(0xFFFFD700).copy(alpha = 0.3f), CircleShape)
                )

                // Large central crown and title layout matching the uploaded logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Floating golden crown emoji
                    Text(
                        text = "👑",
                        fontSize = 72.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "CROWN",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 4.sp,
                            color = Color(0xFFFFD700),
                            fontSize = 32.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "LUDO",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 2.sp,
                            color = Color.White,
                            fontSize = 38.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Subtitle
            Text(
                text = "LUXURY CHAMPIONSHIP",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Polished loading animation & progress bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(200.dp)
            ) {
                Text(
                    text = loadingText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700).copy(alpha = 0.8f),
                        letterSpacing = 1.sp,
                        fontSize = 9.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )

                // Custom animated progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim.value)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA000)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Percentage Indicator
                Text(
                    text = "${(progressAnim.value * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
