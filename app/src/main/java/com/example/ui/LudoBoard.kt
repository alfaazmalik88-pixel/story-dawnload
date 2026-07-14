package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoBoard(
    viewModel: LudoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val currentPlayer = viewModel.getCurrentPlayer()

    var showBackConfirmation by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    // Intercept Android hardware or gesture back key during active match
    BackHandler(enabled = state.gamePhase == GamePhase.PLAYING) {
        showBackConfirmation = true
    }

    // Bouncing/glowing animation for active player tokens & dice
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Ludo Table", fontWeight = FontWeight.Black)
                        val isQuickPlay = state.gameMode == LudoGameMode.HYBRID_ONLINE && state.onlineSubMode == OnlineSubMode.QUICK_PLAY
                        val isTimerActive = state.gameMode == LudoGameMode.ONE_VS_ONE || isQuickPlay
                        if (isTimerActive && state.gamePhase == GamePhase.PLAYING) {
                            val minutes = state.timeLeftSeconds / 60
                            val seconds = state.timeLeftSeconds % 60
                            val timeStr = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = "⏳ $timeStr",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        

                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.gamePhase == GamePhase.PLAYING) {
                            showBackConfirmation = true
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSound() }) {
                        Icon(
                            imageVector = if (state.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Sound"
                        )
                    }
                    IconButton(onClick = {
                        if (state.gamePhase == GamePhase.PLAYING) {
                            showResetConfirmation = true
                        } else {
                            viewModel.resetToSetup()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Match")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1B4B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A),
        bottomBar = { BannerAd() }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                    )
                )
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            
            val padding = 8.dp
            // Calculate a responsive board size that fills maximum screen space
            val maxBoardByHeight = screenHeight - 200.dp
            val boardSize = minOf(screenWidth - padding * 2, maxBoardByHeight, 520.dp).coerceAtLeast(290.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Row: Red Player (Left) & Green Player (Right)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerCornerCard(
                        playerId = 0, // Red
                        viewModel = viewModel,
                        state = state,
                        pulseScale = pulseScale
                    )
                    PlayerCornerCard(
                        playerId = 1, // Green
                        viewModel = viewModel,
                        state = state,
                        pulseScale = pulseScale
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // The Ludo Board container (rendered in 1:1 square aspect ratio)
                Box(
                    modifier = Modifier
                        .size(boardSize)
                        .shadow(16.dp, RoundedCornerShape(12.dp))
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(4.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .testTag("ludo_game_board")
                ) {
                    val cellSize = boardSize / 15f
                    val density = LocalDensity.current
                    val cellSizePx = with(density) { cellSize.toPx() }

                    // 1. Draw Board Bases & Path cells
                    LudoBoardGrid(boardSize = boardSize, cellSize = cellSize, state = state)

                    // 2. Draw Tokens layered perfectly on top!
                    // Group tokens by their actual position coordinates to stack/offset nicely
                    val tokensGrouped = state.tokens.groupBy { token ->
                        LudoCoordinates.getTokenCoordinates(token.playerId, token.id, token.position)
                    }

                    tokensGrouped.forEach { (coord, tokenList) ->
                        val baseRow = coord.first
                        val baseCol = coord.second

                        tokenList.forEachIndexed { index, token ->
                            // Calculate dynamic stack offset if multiple tokens land on same spot
                            val stackOffsetRow = if (tokenList.size > 1) {
                                when (index) {
                                    0 -> -0.15f
                                    1 -> 0.15f
                                    2 -> -0.15f
                                    else -> 0.15f
                                }
                            } else 0f

                            val stackOffsetCol = if (tokenList.size > 1) {
                                when (index) {
                                    0 -> -0.15f
                                    1 -> -0.15f
                                    2 -> 0.15f
                                    else -> 0.15f
                                }
                            } else 0f

                            val r = baseRow + stackOffsetRow
                            val c = baseCol + stackOffsetCol

                            // Determine clickability
                            val isTokenClickable = state.gamePhase == GamePhase.PLAYING &&
                                    state.hasRolled &&
                                    !state.isRolling &&
                                    !state.isMovingToken &&
                                    token.playerId == currentPlayer?.id &&
                                    state.diceRoll != null &&
                                    state.players.firstOrNull { it.id == token.playerId }?.type == PlayerType.HUMAN &&
                                    token.position + state.diceRoll!! <= 57 &&
                                    (token.position > 0 || state.diceRoll == 6)

                            val modifierWithPulse = if (isTokenClickable) {
                                Modifier.scale(pulseScale)
                            } else Modifier

                            val radialGradient = when (token.playerId) {
                                0 -> Brush.radialGradient( // RED
                                    colors = listOf(Color(0xFFFF7676), Color(0xFFE53935), Color(0xFF7B1111))
                                )
                                1 -> Brush.radialGradient( // GREEN
                                    colors = listOf(Color(0xFF86FA8B), Color(0xFF2E7D32), Color(0xFF0D3710))
                                )
                                2 -> Brush.radialGradient( // YELLOW
                                    colors = listOf(Color(0xFFFFF176), Color(0xFFFBC02D), Color(0xFF8F5100))
                                )
                                else -> Brush.radialGradient( // BLUE
                                    colors = listOf(Color(0xFF64D8FF), Color(0xFF1565C0), Color(0xFF08254E))
                                )
                            }

                            val metallicBorder = if (isTokenClickable) {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFDF73), Color(0xFFD4AF37), Color(0xFFFFDF73), Color(0xFF8A640F), Color(0xFFFFDF73))
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFB0BEC5), Color(0xFFFFFFFF), Color(0xFF546E7A), Color(0xFFFFFFFF))
                                )
                            }

                            val baseModifier = Modifier
                                .size(if (tokenList.size > 1) cellSize * 0.75f else cellSize * 0.9f)
                                .offset(
                                    x = cellSize * (c + if (tokenList.size > 1) 0.12f else 0.05f),
                                    y = cellSize * (r + if (tokenList.size > 1) 0.12f else 0.05f)
                                )
                                .then(modifierWithPulse)

                            val tokenModifier = if (state.selectedTokenStyle == LudoTokenStyle.GLOSSY_3D) {
                                baseModifier
                                    .shadow(if (isTokenClickable) 8.dp else 4.dp, CircleShape, spotColor = Color.Black)
                                    .drawBehind {
                                        val radius = size.minDimension / 2
                                        val centerPt = Offset(size.width / 2, size.height / 2)

                                        // 1. Draw background radial gradient circle
                                        drawCircle(
                                            brush = radialGradient,
                                            radius = radius,
                                            center = centerPt
                                        )

                                        // 2. Draw metallic ring border
                                        drawCircle(
                                            brush = metallicBorder,
                                            radius = radius - 1.dp.toPx(),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = if (isTokenClickable) 3.dp.toPx() else 1.8.dp.toPx()
                                            ),
                                            center = centerPt
                                        )

                                        // 3. Inner border accent for elegant depth
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.25f),
                                            radius = radius - (if (isTokenClickable) 4.dp.toPx() else 3.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx()),
                                            center = centerPt
                                        )

                                        // 4. Glossy curved reflection at the top half (glass dome effect)
                                        val glassPath = Path().apply {
                                            addArc(
                                                oval = androidx.compose.ui.geometry.Rect(
                                                    left = centerPt.x - radius,
                                                    top = centerPt.y - radius,
                                                    right = centerPt.x + radius,
                                                    bottom = centerPt.y + radius
                                                ),
                                                startAngleDegrees = 180f,
                                                sweepAngleDegrees = 180f
                                            )
                                            // Curve back across the middle
                                            quadraticTo(
                                                x1 = centerPt.x,
                                                y1 = centerPt.y + radius * 0.15f,
                                                x2 = centerPt.x - radius,
                                                y2 = centerPt.y
                                            )
                                            close()
                                        }
                                        drawPath(
                                            path = glassPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.45f),
                                                    Color.White.copy(alpha = 0.0f)
                                                ),
                                                startY = centerPt.y - radius,
                                                endY = centerPt.y + radius * 0.1f
                                            )
                                        )

                                        // 5. Bright highlight pin-point / dot reflect (top-left)
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.85f),
                                            radius = 2.2.dp.toPx(),
                                            center = Offset(centerPt.x - radius * 0.42f, centerPt.y - radius * 0.42f)
                                        )
                                    }
                            } else {
                                baseModifier
                                    .shadow(if (isTokenClickable) 8.dp else 4.dp, CircleShape)
                                    .background(token.color.value, CircleShape)
                                    .border(
                                        width = if (isTokenClickable) 2.5.dp else 1.5.dp,
                                        color = if (isTokenClickable) Color(0xFFFFD700) else Color.White,
                                        shape = CircleShape
                                    )
                            }

                            Box(
                                modifier = tokenModifier
                                    .clickable(enabled = isTokenClickable) {
                                        viewModel.selectTokenToMove(token)
                                    }
                                    .testTag("token_${token.playerId}_${token.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Token adaptively renders the custom token style chosen by the user!
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (state.selectedTokenStyle == LudoTokenStyle.GLOSSY_3D) {
                                        Text(
                                            text = "${token.id + 1}",
                                            fontSize = if (tokenList.size > 1) 11.sp else 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            style = androidx.compose.ui.text.TextStyle(
                                                shadow = androidx.compose.ui.graphics.Shadow(
                                                    color = Color.Black.copy(alpha = 0.5f),
                                                    offset = Offset(0f, 2f),
                                                    blurRadius = 4f
                                                )
                                            )
                                        )
                                    } else {
                                        val emoji = when (state.selectedTokenStyle) {
                                            LudoTokenStyle.CLASSIC_PIN -> {
                                                // If classic pin, we adapt to the selected theme's default emoji style
                                                when (state.selectedTheme) {
                                                    LudoTheme.CLASSIC -> null
                                                    LudoTheme.COSMIC -> "🚀"
                                                    LudoTheme.ROYAL -> "👑"
                                                    LudoTheme.FOREST -> "🍃"
                                                    LudoTheme.CANDY -> "🍬"
                                                    LudoTheme.OCEAN -> "🐠"
                                                    LudoTheme.CYBERPUNK -> "🔌"
                                                    LudoTheme.EGYPT -> "🏺"
                                                }
                                            }
                                            else -> state.selectedTokenStyle.emoji
                                        }

                                        if (emoji != null) {
                                            Text(text = emoji, fontSize = if (tokenList.size > 1) 10.sp else 14.sp)
                                            Text(
                                                text = "${token.id + 1}",
                                                fontSize = if (tokenList.size > 1) 7.sp else 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (tokenList.size > 1) cellSize * 0.38f else cellSize * 0.48f)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC))
                                                        ),
                                                        shape = CircleShape
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = Color(0xFFFFD700).copy(alpha = 0.8f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${token.id + 1}",
                                                    fontSize = if (tokenList.size > 1) 9.sp else 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF1A1A1A)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Row: Blue Player (Left) & Yellow Player (Right)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerCornerCard(
                        playerId = 3, // Blue
                        viewModel = viewModel,
                        state = state,
                        pulseScale = pulseScale
                    )
                    PlayerCornerCard(
                        playerId = 2, // Yellow
                        viewModel = viewModel,
                        state = state,
                        pulseScale = pulseScale
                    )
                }

                // Simple minimalist "Guaranteed 6" ad button without any background or status text
                if (state.gameMode == LudoGameMode.VS_COMPUTER || state.gameMode == LudoGameMode.ONE_VS_ONE) {
                    val currentPlayer = state.players.firstOrNull { it.id == state.currentPlayerIdx }
                    val isHumanTurn = currentPlayer?.type == PlayerType.HUMAN && !state.hasRolled && !state.isRolling && !state.isMovingToken
                    val isSixOnCooldown = state.guaranteedSixCooldownRemaining > 0
                    val isEligibleForSix = isHumanTurn && !state.nextRollIsSix && !isSixOnCooldown

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .width(if (isSixOnCooldown) 90.dp else 120.dp)
                                .height(26.dp)
                                .shadow(if (isEligibleForSix || state.nextRollIsSix) 2.dp else 0.dp, RoundedCornerShape(13.dp))
                                .clickable(enabled = isEligibleForSix || state.nextRollIsSix) {
                                    viewModel.triggerAd(AdType.GUARANTEED_SIX)
                                },
                            shape = RoundedCornerShape(13.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.nextRollIsSix) Color(0xFF065F46) else if (isEligibleForSix) Color(0xFF1E1B4B) else Color(0xFF1E293B)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (state.nextRollIsSix) Color(0xFF34D399) else if (isEligibleForSix) Color(0xFFFFD700) else Color(0xFF475569)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSixOnCooldown) {
                                    val mins = (state.guaranteedSixCooldownRemaining / 60).toString().padStart(2, '0')
                                    val secs = (state.guaranteedSixCooldownRemaining % 60).toString().padStart(2, '0')
                                    Text(
                                        text = "⏳ $mins:$secs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                } else {
                                    DiceSixIcon(
                                        size = 12.dp,
                                        isEnabled = isEligibleForSix || state.nextRollIsSix,
                                        isNextRollSix = state.nextRollIsSix
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (state.nextRollIsSix) LudoTranslations.getTranslation("six_active", state.selectedLanguage) else LudoTranslations.getTranslation("get_six", state.selectedLanguage),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = if (isEligibleForSix || state.nextRollIsSix) Color.White else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Game Finished overlay
            if (state.gamePhase == GamePhase.FINISHED) {
                val winnerId = state.winnerPlayerId ?: 0
                val winnerPlayer = state.players.firstOrNull { it.id == winnerId }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(24.dp)
                            .shadow(24.dp, RoundedCornerShape(24.dp))
                            .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🏆 LUDO MATCH OVER 🏆",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700),
                                    letterSpacing = 2.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(winnerPlayer?.color?.value ?: Color.Yellow, CircleShape)
                                    .border(4.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Winner",
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp)
                                )
                            }

                            Text(
                                text = "${winnerPlayer?.name ?: "Winner"} Wins!",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )

                            Text(
                                text = "Ludo match successfully completed. What would you like to do next?",
                                textAlign = TextAlign.Center,
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.startGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("REPLAY MATCH", fontWeight = FontWeight.Black, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { viewModel.triggerAd(AdType.GAME_FINISH) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("RETURN TO SETUP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBackConfirmation) {
        AlertDialog(
            onDismissRequest = { showBackConfirmation = false },
            title = {
                Text(
                    text = LudoTranslations.getTranslation("back_confirm_title", state.selectedLanguage),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = LudoTranslations.getTranslation("back_confirm_desc", state.selectedLanguage),
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackConfirmation = false
                        viewModel.triggerAd(AdType.GAME_FINISH)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text(LudoTranslations.getTranslation("yes", state.selectedLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackConfirmation = false }
                ) {
                    Text(LudoTranslations.getTranslation("no", state.selectedLanguage), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1B4B),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = {
                Text(
                    text = LudoTranslations.getTranslation("reset_confirm_title", state.selectedLanguage),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = LudoTranslations.getTranslation("reset_confirm_desc", state.selectedLanguage),
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        viewModel.triggerAd(AdType.RESET)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text(LudoTranslations.getTranslation("yes", state.selectedLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmation = false }
                ) {
                    Text(LudoTranslations.getTranslation("no", state.selectedLanguage), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1B4B),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // Interactive Sponsor Video Ad Player Dialog
    if (state.adType != null) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFFFD700),
                        strokeWidth = 2.5.dp
                    )
                    Text(LudoTranslations.getTranslation("watching_ad", state.selectedLanguage), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = when (state.adType) {
                            AdType.GUARANTEED_SIX -> LudoTranslations.getTranslation("ad_guaranteed_six", state.selectedLanguage)
                            AdType.EXTEND_TIME -> LudoTranslations.getTranslation("ad_extend_time", state.selectedLanguage)
                            AdType.GAME_FINISH -> LudoTranslations.getTranslation("ad_game_finish", state.selectedLanguage)
                            AdType.RESET -> LudoTranslations.getTranslation("ad_reset", state.selectedLanguage)
                            else -> LudoTranslations.getTranslation("ad_watching", state.selectedLanguage)
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = LudoTranslations.getTranslation("reward_claims", state.selectedLanguage).replace("%d", state.adSecondsLeft.toString()),
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = Color(0xFFFFD700)
                    )

                    LinearProgressIndicator(
                        progress = { (5f - state.adSecondsLeft) / 5f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFD700),
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAd() }) {
                    Text("Skip Ad", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E1B4B),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // Time's Up Reward Dialog (Extend Time)
    if (state.isTimeUpDialogShowing) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("⏳ TIME IS UP! (समय समाप्त)", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "The 5-minute match timer has run out! Would you like to extend the game by 5 more minutes by watching a sponsor ad?",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { viewModel.triggerAd(AdType.EXTEND_TIME) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎲 Watch Ad (+5 Min)", fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTimeUpDialog() }) {
                    Text("End Match & See Winner", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1B4B),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // Time's Up Warning Dialog (Extend Time before it runs out)
    if (state.isTimeWarningDialogShowing) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTimeWarningDialog() },
            title = {
                Text("⚠️ TIME IS RUNNING OUT! (समय समाप्त होने वाला है)", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "केवल 30 सेकंड बचे हैं! क्या आप एक छोटा प्रायोजक विज्ञापन (Sponsor Ad) देखकर गेम का समय 5 मिनट और बढ़ाना चाहते हैं?\n\n(Only 30 seconds left! Would you like to extend the game by 5 more minutes right now by watching a short sponsor ad?)",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { viewModel.triggerAd(AdType.EXTEND_TIME) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎲 Watch Ad (+5 Min)", fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTimeWarningDialog() }) {
                    Text("Cancel (रद्द करें)", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1B4B),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }


}

@Composable
fun DiceSixIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    isEnabled: Boolean = true,
    isNextRollSix: Boolean = false
) {
    val dotSize = (size.value * 0.13f).dp
    val paddingSize = (size.value * 0.16f).dp
    val cornerRadius = (size.value * 0.22f).dp
    Box(
        modifier = modifier
            .size(size)
            .shadow(if (isEnabled) 3.dp else 0.dp, RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isNextRollSix) {
                        listOf(Color(0xFF10B981), Color(0xFF047857))
                    } else if (isEnabled) {
                        listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                    } else {
                        listOf(Color(0xFF475569), Color(0xFF334155))
                    }
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 1.dp,
                color = if (isNextRollSix) Color(0xFFA7F3D0) else if (isEnabled) Color(0xFFFFD700) else Color(0xFF64748B),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(paddingSize),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.size(dotSize).background(Color.White, androidx.compose.foundation.shape.CircleShape))
                Box(modifier = Modifier.size(dotSize).background(Color.White, androidx.compose.foundation.shape.CircleShape))
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.size(dotSize).background(Color.White, androidx.compose.foundation.shape.CircleShape))
                Box(modifier = Modifier.size(dotSize).background(Color.White, androidx.compose.foundation.shape.CircleShape))
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.size(dotSize).background(Color.White, androidx.compose.foundation.shape.CircleShape))
                Box(modifier = Modifier.size(dotSize).background(Color.White, androidx.compose.foundation.shape.CircleShape))
            }
        }
    }
}

@Composable
fun LudoBoardGrid(
    boardSize: androidx.compose.ui.unit.Dp,
    cellSize: androidx.compose.ui.unit.Dp,
    state: LudoState
) {
    val emptyCellColor = when (state.selectedTheme) {
        LudoTheme.CLASSIC -> Color(0xFFFFFBEB) // Wood/parchment warm cream
        LudoTheme.COSMIC -> Color(0xFF0F172A)  // Cosmic space dark slate
        LudoTheme.ROYAL -> Color(0xFFFFFDF5)   // Palace soft gold ivory
        LudoTheme.FOREST -> Color(0xFFEDF7ED)  // Jungle fresh light mint
        LudoTheme.CANDY -> Color(0xFFFFF0F5)   // Lavender blush pink
        LudoTheme.OCEAN -> Color(0xFFECFEFF)   // Sky cyan/blue
        LudoTheme.CYBERPUNK -> Color(0xFF1E1B4B) // Dark neon purple
        LudoTheme.EGYPT -> Color(0xFFFEF3C7)   // Golden desert sand
    }
    val starColor = when (state.selectedTheme) {
        LudoTheme.CLASSIC -> Color(0xFF94A3B8)
        LudoTheme.COSMIC -> Color(0xFF22D3EE)  // Neon cyan
        LudoTheme.ROYAL -> Color(0xFFFFD700)   // Bright gold
        LudoTheme.FOREST -> Color(0xFF4ADE80)  // Fresh mint green
        LudoTheme.CANDY -> Color(0xFFEC4899)   // Hot pink candy
        LudoTheme.OCEAN -> Color(0xFF06B6D4)   // Deep cyan ocean
        LudoTheme.CYBERPUNK -> Color(0xFFF43F5E) // Cyber neon rose
        LudoTheme.EGYPT -> Color(0xFFD97706)   // Amber gold
    }

    // Dynamic color mapping based on players currently assigned to each base index.
    // Index 0: Top Left, Index 1: Top Right, Index 2: Bottom Right, Index 3: Bottom Left.
    val color0 = state.baseColors.getOrNull(0) ?: LudoColor.RED
    val color1 = state.baseColors.getOrNull(1) ?: LudoColor.GREEN
    val color2 = state.baseColors.getOrNull(2) ?: LudoColor.YELLOW
    val color3 = state.baseColors.getOrNull(3) ?: LudoColor.BLUE

    Box(modifier = Modifier.size(boardSize)) {
        // 1. Render all 15x15 board cells
        for (row in 0..14) {
            for (col in 0..14) {
                // Determine if this cell falls under base yards or center home
                val isRedBase = row in 0..5 && col in 0..5
                val isGreenBase = row in 0..5 && col in 9..14
                val isYellowBase = row in 9..14 && col in 9..14
                val isBlueBase = row in 9..14 && col in 0..5
                val isCenterHome = row in 6..8 && col in 6..8

                if (!isRedBase && !isGreenBase && !isYellowBase && !isBlueBase && !isCenterHome) {
                    // Path cell rendering
                    val isSafeStar = LudoCoordinates.isCellSafe(row, col)

                    // Color coordinate-specific cells
                    val cellColor = when {
                        // Home stretch corridors (mapped to dynamic corner colors)
                        row == 7 && col in 1..5 -> color0.value
                        col == 7 && row in 1..5 -> color1.value
                        row == 7 && col in 9..13 -> color2.value
                        col == 7 && row in 9..13 -> color3.value
                        
                        // Starting arrow point cells
                        row == 6 && col == 1 -> color0.value
                        row == 1 && col == 8 -> color1.value
                        row == 8 && col == 13 -> color2.value
                        row == 13 && col == 6 -> color3.value

                        // Standard path backgrounds
                        else -> emptyCellColor
                    }

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .offset(x = cellSize * col, y = cellSize * row)
                            .background(cellColor)
                            .border(0.5.dp, Color(0xFFCBD5E1)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSafeStar) {
                            // Render a protective star on safe cells
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Safe Zone Star",
                                tint = if (cellColor == emptyCellColor) starColor else Color.White,
                                modifier = Modifier.size(cellSize * 0.7f)
                            )
                        }
                    }
                }
            }
        }

        val isTeamUp = state.gameMode == LudoGameMode.TEAM_UP

        // 2. Draw Top Left Base (dynamic color0)
        BaseArea(
            color = color0,
            isTeamUp = isTeamUp,
            teamLabel = "TEAM A",
            modifier = Modifier
                .size(cellSize * 6)
                .offset(x = 0.dp, y = 0.dp)
        )

        // 3. Draw Top Right Base (dynamic color1)
        BaseArea(
            color = color1,
            isTeamUp = isTeamUp,
            teamLabel = "TEAM B",
            modifier = Modifier
                .size(cellSize * 6)
                .offset(x = cellSize * 9, y = 0.dp)
        )

        // 4. Draw Bottom Right Base (dynamic color2)
        BaseArea(
            color = color2,
            isTeamUp = isTeamUp,
            teamLabel = "TEAM A",
            modifier = Modifier
                .size(cellSize * 6)
                .offset(x = cellSize * 9, y = cellSize * 9)
        )

        // 5. Draw Bottom Left Base (dynamic color3 - always matches the Human's chosen color!)
        BaseArea(
            color = color3,
            isTeamUp = isTeamUp,
            teamLabel = "TEAM B",
            modifier = Modifier
                .size(cellSize * 6)
                .offset(x = 0.dp, y = cellSize * 9)
        )

        // 6. Draw central home cross intersection meeting using clean Canvas
        Canvas(
            modifier = Modifier
                .size(cellSize * 3)
                .offset(x = cellSize * 6, y = cellSize * 6)
                .border(1.dp, Color(0xFFCBD5E1))
        ) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // Left Home Triangle (corresponds to dynamic color0)
            val redPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(center.x, center.y)
                lineTo(0f, h)
                close()
            }
            drawPath(redPath, color0.value)

            // Top Home Triangle (corresponds to dynamic color1)
            val greenPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(center.x, center.y)
                close()
            }
            drawPath(greenPath, color1.value)

            // Right Home Triangle (corresponds to dynamic color2)
            val yellowPath = Path().apply {
                moveTo(w, 0f)
                lineTo(w, h)
                lineTo(center.x, center.y)
                close()
            }
            drawPath(yellowPath, color2.value)

            // Bottom Home Triangle (corresponds to dynamic color3)
            val bluePath = Path().apply {
                moveTo(0f, h)
                lineTo(center.x, center.y)
                lineTo(w, h)
                close()
            }
            drawPath(bluePath, color3.value)
        }
    }
}

@Composable
fun BaseArea(
    color: LudoColor,
    isTeamUp: Boolean,
    teamLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.value)
            .border(1.dp, Color(0xFF94A3B8)),
        contentAlignment = Alignment.Center
    ) {
        // Safe inner board white pocket card
        Card(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .shadow(2.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isTeamUp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .background(color.value.copy(alpha = 0.08f))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teamLabel,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = color.value.copy(alpha = 0.85f),
                            letterSpacing = 1.sp
                        )
                    }
                }
                // Draw 4 yard pockets circles where pieces reside
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        PocketCircle(color = color.value)
                        PocketCircle(color = color.value)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        PocketCircle(color = color.value)
                        PocketCircle(color = color.value)
                    }
                }
            }
        }
    }
}

@Composable
fun PocketCircle(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color.copy(alpha = 0.15f), CircleShape)
            .border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun PlayerCornerCard(
    playerId: Int,
    viewModel: LudoViewModel,
    state: LudoState,
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    val player = state.players.firstOrNull { it.id == playerId }
    val isCurrentTurn = state.currentPlayerIdx == playerId && state.gamePhase == GamePhase.PLAYING

    val diceRotationTransition = rememberInfiniteTransition(label = "dice_rotation")
    val diceRotationAngle by diceRotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 180, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    val defaultColor = state.baseColors.getOrNull(playerId) ?: LudoColor.BLUE

    if (player != null) {
        val canRoll = isCurrentTurn && !state.hasRolled && !state.isRolling && !state.isMovingToken && player.type == PlayerType.HUMAN

        val cardBorderColor = if (isCurrentTurn) defaultColor.value else defaultColor.value.copy(alpha = 0.3f)
        val cardBgColor = if (isCurrentTurn) Color(0xCC241F55) else Color(0x991E293B)

        val diceModifier = if (canRoll) {
            Modifier.scale(pulseScale)
        } else Modifier

        val bubbleText = state.activePlayerBubbles[playerId]
        val isRightSide = playerId == 1 || playerId == 2

        val diceBg = when (state.selectedDiceStyle) {
            LudoDiceStyle.CLASSIC_DOTS -> if (isCurrentTurn) Color.White else Color(0xFFF1F5F9)
            LudoDiceStyle.NEON_LASER -> if (isCurrentTurn) Color(0xFFECFDF5) else Color(0xFFE6F4EA)
            LudoDiceStyle.ROYAL_SCEPTRE -> if (isCurrentTurn) Color(0xFFFEF3C7) else Color(0xFFFEF3C7).copy(alpha = 0.5f)
            LudoDiceStyle.OCEAN_SHELL -> if (isCurrentTurn) Color(0xFFECFEFF) else Color(0xFFE0F2FE)
            LudoDiceStyle.ANCIENT_HIEROGLYPH -> if (isCurrentTurn) Color(0xFFFFFBEB) else Color(0xFFFEF3C7).copy(alpha = 0.3f)
            LudoDiceStyle.COSMIC_SINGULARITY -> if (isCurrentTurn) Color(0xFFF5F3FF) else Color(0xFFEDE9FE)
        }
        val diceBorderColor = when (state.selectedDiceStyle) {
            LudoDiceStyle.CLASSIC_DOTS -> if (isCurrentTurn) Color(0xFFFFD700) else Color(0xFF94A3B8)
            LudoDiceStyle.NEON_LASER -> if (isCurrentTurn) Color(0xFF10B981) else Color(0xFF34D399)
            LudoDiceStyle.ROYAL_SCEPTRE -> if (isCurrentTurn) Color(0xFFFFD700) else Color(0xFFF59E0B)
            LudoDiceStyle.OCEAN_SHELL -> if (isCurrentTurn) Color(0xFF06B6D4) else Color(0xFF38BDF8)
            LudoDiceStyle.ANCIENT_HIEROGLYPH -> if (isCurrentTurn) Color(0xFFD97706) else Color(0xFFB45309)
            LudoDiceStyle.COSMIC_SINGULARITY -> if (isCurrentTurn) Color(0xFF8B5CF6) else Color(0xFFA78BFA)
        }

        val isBottomPlayer = playerId == 2 || playerId == 3

        Box(modifier = modifier.width(105.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val displayName = if (player.id == 3) {
                    when (state.selectedLanguage) {
                        LudoLanguage.IN -> "आप"
                        LudoLanguage.SA, LudoLanguage.AE, LudoLanguage.KW, LudoLanguage.QA, LudoLanguage.OM, LudoLanguage.BH -> "أنت"
                        LudoLanguage.MX, LudoLanguage.ES -> "Tú"
                        LudoLanguage.BR -> "Você"
                        LudoLanguage.ID -> "Anda"
                        LudoLanguage.TR -> "Siz"
                        LudoLanguage.RU -> "Вы"
                        else -> "You"
                    }
                } else {
                    player.name
                }

                if (!isBottomPlayer) {
                    Text(
                        text = displayName,
                        color = if (isCurrentTurn) Color(0xFFFFD700) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    if (state.gameMode == LudoGameMode.HYBRID_ONLINE) {
                        val statusText = if (player.id == 3) "📶 32 ms" 
                                        else if (state.disconnectedPlayers.contains(player.id)) "⚠️ Off / AI" 
                                        else "📶 ${state.onlinePlayerPings[player.id] ?: 54} ms"
                        val statusColor = if (player.id == 3 || !state.disconnectedPlayers.contains(player.id)) Color(0xFF10B981) else Color(0xFFEF4444)
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(bottom = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(if (isCurrentTurn) 12.dp else 2.dp, RoundedCornerShape(14.dp))
                        .border(
                            width = if (isCurrentTurn) 2.5.dp else 1.dp,
                            color = cardBorderColor,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRightSide) {
                            // Right-side players: Dice / Pansa first (inner edge), then Team label, then User / Bot profile icon (outer edge)
                            // 1. Beautiful interactive Dice / Pansa
                            Box(
                                modifier = Modifier
                                    .then(diceModifier)
                                    .size(40.dp)
                                    .shadow(if (isCurrentTurn) 6.dp else 2.dp, RoundedCornerShape(8.dp))
                                    .background(
                                        diceBg,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isCurrentTurn) 2.5.dp else 1.dp,
                                        color = diceBorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = canRoll) {
                                        viewModel.rollDice()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val rollValue = if (isCurrentTurn) (state.diceRoll ?: 1) else 1
                                val rotation = if (isCurrentTurn && state.isRolling) diceRotationAngle else 0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .rotate(rotation),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DiceFace(value = rollValue, tint = player.color.value, style = state.selectedDiceStyle, modifier = Modifier.fillMaxSize())
                                }
                            }

                            // 2. Compact Color dot indicating player color & type (Human vs Bot)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(player.color.value, CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (player.type == PlayerType.BOT) Icons.Default.Android else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        } else {
                            // Left-side players: User / Bot profile icon first (outer edge), then Team label, then Dice / Pansa (inner edge)
                            // 1. Compact Color dot indicating player color & type (Human vs Bot)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(player.color.value, CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (player.type == PlayerType.BOT) Icons.Default.Android else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            // 2. Beautiful interactive Dice / Pansa
                            Box(
                                modifier = Modifier
                                    .then(diceModifier)
                                    .size(40.dp)
                                    .shadow(if (isCurrentTurn) 6.dp else 2.dp, RoundedCornerShape(8.dp))
                                    .background(
                                        diceBg,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isCurrentTurn) 2.5.dp else 1.dp,
                                        color = diceBorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = canRoll) {
                                        viewModel.rollDice()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val rollValue = if (isCurrentTurn) (state.diceRoll ?: 1) else 1
                                val rotation = if (isCurrentTurn && state.isRolling) diceRotationAngle else 0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .rotate(rotation),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DiceFace(value = rollValue, tint = player.color.value, style = state.selectedDiceStyle, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }

                if (isBottomPlayer) {
                    if (state.gameMode == LudoGameMode.HYBRID_ONLINE) {
                        val statusText = if (player.id == 3) "📶 32 ms" 
                                        else if (state.disconnectedPlayers.contains(player.id)) "⚠️ Off / AI" 
                                        else "📶 ${state.onlinePlayerPings[player.id] ?: 54} ms"
                        val statusColor = if (player.id == 3 || !state.disconnectedPlayers.contains(player.id)) Color(0xFF10B981) else Color(0xFFEF4444)
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = displayName,
                        color = if (isCurrentTurn) Color(0xFFFFD700) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        // Subtle minimal placeholder dot for absent slot
        Box(
            modifier = modifier
                .size(36.dp)
                .background(Color(0x221E293B), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(defaultColor.value.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
}

@Composable
fun DiceFace(value: Int, tint: Color, style: LudoDiceStyle, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            LudoDiceStyle.CLASSIC_DOTS -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (value) {
                        1 -> Box(modifier = Modifier.size(6.dp).background(tint, CircleShape))
                        2 -> {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Box(modifier = Modifier.size(5.dp).background(tint, CircleShape).align(Alignment.Start))
                                Box(modifier = Modifier.size(5.dp).background(tint, CircleShape).align(Alignment.End))
                            }
                        }
                        3 -> {
                            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Box(modifier = Modifier.size(4.dp).background(tint, CircleShape).align(Alignment.TopStart))
                                Box(modifier = Modifier.size(4.dp).background(tint, CircleShape).align(Alignment.Center))
                                Box(modifier = Modifier.size(4.dp).background(tint, CircleShape).align(Alignment.BottomEnd))
                            }
                        }
                        4 -> {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                }
                            }
                        }
                        5 -> {
                            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                }
                                Box(modifier = Modifier.size(4.dp).background(tint, CircleShape).align(Alignment.Center))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                }
                            }
                        }
                        6 -> {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                    Box(modifier = Modifier.size(4.dp).background(tint, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = style.emoji, fontSize = 11.sp)
                    Text(
                        text = "$value",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = tint
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: String,
    isTopRow: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.widthIn(max = 130.dp)
    ) {
        if (!isTopRow) {
            // Above card: bubble body on top, arrow at the bottom pointing down
            Box(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1B4B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = (-4).dp)
                    .rotate(45f)
                    .background(Color(0xFF1E1B4B))
                    .border(1.dp, Color(0xFF818CF8))
            )
        } else {
            // Below card: arrow at the top pointing up, bubble body at the bottom
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = 4.dp)
                    .rotate(45f)
                    .background(Color(0xFF1E1B4B))
                    .border(1.dp, Color(0xFF818CF8))
            )
            Box(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1B4B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
