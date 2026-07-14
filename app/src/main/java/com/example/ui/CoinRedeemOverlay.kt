package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LudoAudioEngine
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun CoinRedeemOverlay(
    amount: Int,
    onDismiss: () -> Unit
) {
    if (amount <= 0) return

    // Trigger victory fanfare on launch
    LaunchedEffect(Unit) {
        LudoAudioEngine.playVictory()
        // Automatically dismiss after 4 seconds
        delay(4000)
        onDismiss()
    }

    // Interactive scale animation for the center card
    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    val cardScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Generate 35 coin particle animations with different launch parameters
    val particles = remember {
        List(35) {
            val angle = Random.nextFloat() * 2 * Math.PI
            val speed = Random.nextFloat() * 350f + 150f
            val delay = Random.nextInt(0, 500)
            val coinSize = Random.nextFloat() * 14f + 14f
            val rotSpeed = Random.nextFloat() * 360f + 180f
            CoinAnimData(
                angle = angle,
                speed = speed,
                delayMs = delay,
                size = coinSize,
                rotSpeed = rotSpeed
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = true) { /* Consume clicks to prevent background actions */ },
        contentAlignment = Alignment.Center
    ) {
        // Draw the streaming coin particles!
        particles.forEach { p ->
            var progress by remember { mutableStateOf(0f) }
            LaunchedEffect(Unit) {
                delay(p.delayMs.toLong())
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 2200,
                        easing = LinearOutSlowInEasing
                    )
                ) { value, _ ->
                    progress = value
                }
            }

            if (progress > 0f) {
                // Calculate position based on progress & launch physics
                val distance = p.speed * progress
                val xOffset = (Math.cos(p.angle) * distance).toFloat()
                // Defy gravity: they fly outwards and then float up!
                val yOffset = (Math.sin(p.angle) * distance - (progress * 150f)).toFloat()
                val scale = if (progress > 0.8f) (1f - (progress - 0.8f) * 5f) else 1f
                val rotation = progress * p.rotSpeed

                if (scale > 0f) {
                    Box(
                        modifier = Modifier
                            .offset(x = xOffset.dp, y = yOffset.dp)
                            .scale(scale)
                            .size(p.size.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Spinning gold coin gradient
                            val gradColors = listOf(Color(0xFFFFF176), Color(0xFFF57F17), Color(0xFFFFB300))
                            drawCircle(
                                brush = Brush.linearGradient(
                                    colors = gradColors,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                ),
                                radius = size.width / 2
                            )
                            // Inner embossed border
                            drawCircle(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFEE58), Color(0xFFE65100)),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, 0f)
                                ),
                                radius = size.width / 3.2f
                            )
                        }
                    }
                }
            }
        }

        // Center card with beautiful high-visibility details
        Card(
            modifier = Modifier
                .width(320.dp)
                .scale(cardScale)
                .padding(16.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Rich deep indigo/navy
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Color(0xFFFFD700)) // Golden border
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Gold Coin Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFFF176), Color(0xFFE65100))
                            )
                        )
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 44.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = "REDEEM SUCCESSFUL!",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "सिक्के सफलतापूर्वक रिडीम हो गए हैं!",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "+$amount Coins",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your free rewards have been added instantly to your wallet. Watch more ads or win games to earn more!",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Color(0xFF34D399), RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = "AWESOME! 👍",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private data class CoinAnimData(
    val angle: Double,
    val speed: Float,
    val delayMs: Int,
    val size: Float,
    val rotSpeed: Float
)
