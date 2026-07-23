package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.model.LudoColor
import com.example.model.LudoGameMode
import com.example.model.LudoState
import com.example.model.LudoViewModel
import com.example.model.PlayerType

@Composable
fun LudoMatchmakingScreen(
    state: LudoState,
    viewModel: LudoViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = state.isFindingOpponent) {
        viewModel.cancelMatchmaking()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lobby")
    
    // Beautiful glow and pulse animations
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val livePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livePulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Deep Slate dark background
                        Color(0xFF1E1B4B), // Cyber violet depth
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Top Title & Game Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "LUDO LIVE BATTLE LOBBY",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color(0xFFFFD700) // Gold
                    )
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val modeTitle = "Multiplayer Online 🌐"
                            Text(
                                text = modeTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF94A3B8))
                            )
                            Text(
                                text = "🪙 ${state.selectedWagerAmount} Wager",
                                color = Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = livePulseAlpha))
                            )
                            Text(
                                text = "1,452 PLAYERS LIVE ONLINE 🟢",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Main Versus Box (Symmetrical dynamic layout)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.5.dp, Brush.horizontalGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFFEC4899))
                        )),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827).copy(alpha = 0.85f))
            ) {
                val players = state.players
                if (players.size <= 2) {
                    // 1v1 side-by-side
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val p1 = players.firstOrNull { it.id == 3 } ?: players.getOrNull(0)
                        val p2 = players.firstOrNull { it.id != 3 } ?: players.getOrNull(1)

                        if (p1 != null) {
                            MatchmakingPlayerCard(player = p1, isMe = p1.id == 3, modifier = Modifier.weight(1f))
                        }

                        // VS Divider
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(pulseScale)
                                    .border(BorderStroke(1.5.dp, Color(0xFFFFD700)), shape = CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "VS",
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (p2 != null) {
                            MatchmakingPlayerCard(player = p2, isMe = p2.id == 3, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    // 3 or 4 players - 2x2 grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1: Player 1 and Player 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val p1 = players.getOrNull(0)
                            val p2 = players.getOrNull(1)
                            if (p1 != null) {
                                MatchmakingPlayerCard(player = p1, isMe = p1.id == 3, modifier = Modifier.weight(1f))
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            if (p2 != null) {
                                MatchmakingPlayerCard(player = p2, isMe = p2.id == 3, modifier = Modifier.weight(1f))
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        // Row 2: Player 3 and Player 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val p3 = players.getOrNull(2)
                            val p4 = players.getOrNull(3)
                            if (p3 != null) {
                                MatchmakingPlayerCard(player = p3, isMe = p3.id == 3, modifier = Modifier.weight(1f))
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            if (p4 != null) {
                                MatchmakingPlayerCard(player = p4, isMe = p4.id == 3, modifier = Modifier.weight(1f))
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Pulse Loading status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFEF4444),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )
                
                Text(
                    text = state.statusMessage,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // Cancel Button
            Button(
                onClick = {
                    viewModel.cancelMatchmaking()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444), // Crimson Red
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(180.dp)
                    .height(44.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                    Text(
                        text = "CANCEL MATCH",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MatchmakingPlayerCard(
    player: com.example.model.Player,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val isConnected = !player.name.contains("Searching") && !player.name.contains("Waiting")
    val playerColor = player.color
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        // Profile Circle
        Box(
            modifier = Modifier
                .size(68.dp)
                .border(
                    BorderStroke(
                        3.dp,
                        if (isConnected) playerColor.value else Color.White.copy(alpha = 0.2f)
                    ),
                    shape = CircleShape
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        if (isConnected) playerColor.value.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.05f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Person else Icons.Default.Search,
                    contentDescription = player.name,
                    tint = if (isConnected) playerColor.value else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Text(
            text = if (isMe) "${player.name} (You)" else player.name,
            color = if (isConnected) Color.White else Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFFBBF24).copy(alpha = 0.15f),
            border = BorderStroke(
                1.dp,
                if (isConnected) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFFBBF24).copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF10B981) else Color(0xFFFBBF24))
                )
                Text(
                    text = if (isConnected) "CONNECTED 🟢" else "SEARCHING... 🔍",
                    color = if (isConnected) Color(0xFF34D399) else Color(0xFFFBBF24),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
