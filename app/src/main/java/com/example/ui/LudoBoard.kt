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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import kotlinx.coroutines.delay

private data class TokenGroupKey(
    val row: Float,
    val col: Float,
    val groupId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoBoard(
    viewModel: LudoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val currentPlayer = state.players.firstOrNull { it.id == state.currentPlayerIdx }

    var showBackConfirmation by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }

    val isWagerMode = state.gameMode == LudoGameMode.ONE_VS_ONE || state.gameMode == LudoGameMode.HYBRID_ONLINE

    BackHandler(enabled = true) {
        if (state.gamePhase == GamePhase.PLAYING) {
            showBackConfirmation = true
        } else {
            viewModel.triggerAd(AdType.GAME_FINISH)
        }
    }

    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.enableMic(context)
        } else {
            android.widget.Toast.makeText(context, "Microphone permission required for voice chat", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Bouncing/glowing animation for active player tokens & dice
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.20f,
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
                            viewModel.triggerAd(AdType.GAME_FINISH)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Chat Button
                    IconButton(onClick = { showChatDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Quick Chat",
                            tint = Color(0xFFFFD700)
                        )
                    }

                    // Speaker/Voice Chat Button
                    IconButton(onClick = { viewModel.toggleVoice() }) {
                        Icon(
                            imageVector = if (state.isVoiceEnabled) Icons.Default.Hearing else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Voice Speaker",
                            tint = if (state.isVoiceEnabled) Color(0xFF10B981) else Color.LightGray
                        )
                    }

                    // Microphone/Mic Button with live amplitude glow
                    IconButton(onClick = {
                        if (state.isMicEnabled) {
                            viewModel.disableMic()
                        } else {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                viewModel.enableMic(context)
                            } else {
                                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }) {
                        Box(contentAlignment = Alignment.Center) {
                            if (state.isMicEnabled) {
                                val glowAlpha = (state.micAmplitude * 0.8f + 0.3f).coerceIn(0.3f, 1f)
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = glowAlpha)
                                ) {}
                            }
                            Icon(
                                imageVector = if (state.isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Toggle Microphone",
                                tint = if (state.isMicEnabled) Color.White else Color.LightGray
                            )
                        }
                    }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .zIndex(2f),
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
                        .zIndex(1f)
                        .testTag("ludo_game_board")
                ) {
                    val cellSize = boardSize / 15f
                    val density = LocalDensity.current
                    val cellSizePx = with(density) { cellSize.toPx() }

                    // 1. Draw Board Bases & Path cells
                    LudoBoardGrid(boardSize = boardSize, cellSize = cellSize, state = state)

                    // 2. Draw Tokens layered perfectly on top!
                    // Group tokens by their actual position coordinates to stack/offset nicely.
                    // If a token is actively moving, we treat it as its own separate group so it hops
                    // cleanly over cells without disrupting the size/offset of stationary tokens!
                    val tokensGrouped = state.tokens.groupBy { token ->
                        val coord = LudoCoordinates.getTokenCoordinates(token.playerId, token.id, token.position)
                        val isMovingThisToken = state.isMovingToken && 
                                state.movingTokenId == token.id && 
                                state.currentPlayerIdx == token.playerId
                        
                        val groupId = if (isMovingThisToken) {
                            "moving_${token.playerId}_${token.id}"
                        } else {
                            "cell_${coord.first}_${coord.second}"
                        }
                        
                        TokenGroupKey(coord.first, coord.second, groupId)
                    }

                    tokensGrouped.forEach { (key, tokenList) ->
                        val baseRow = key.row
                        val baseCol = key.col

                        tokenList.forEachIndexed { index, token ->
                            val isAtHome = token.position == 57

                            // Precise sub-grid offsets within 1x1 cell to prevent clipping at edges/corners
                            val (cellOffsetRelX, cellOffsetRelY, tokenSizeFactor) = when {
                                isAtHome -> Triple(0.20f, 0.20f, 0.60f)
                                tokenList.size == 1 -> Triple(0.10f, 0.10f, 0.80f)
                                tokenList.size == 2 -> when (index) {
                                    0 -> Triple(0.04f, 0.04f, 0.48f)
                                    else -> Triple(0.48f, 0.48f, 0.48f)
                                }
                                tokenList.size == 3 -> when (index) {
                                    0 -> Triple(0.04f, 0.04f, 0.46f)
                                    1 -> Triple(0.50f, 0.04f, 0.46f)
                                    else -> Triple(0.27f, 0.50f, 0.46f)
                                }
                                else -> when (index) {
                                    0 -> Triple(0.04f, 0.04f, 0.45f)
                                    1 -> Triple(0.51f, 0.04f, 0.45f)
                                    2 -> Triple(0.04f, 0.51f, 0.45f)
                                    else -> Triple(0.51f, 0.51f, 0.45f)
                                }
                            }

                            val rawCellX = baseCol + cellOffsetRelX
                            val rawCellY = baseRow + cellOffsetRelY

                            val clampedCellX = rawCellX.coerceIn(0.02f, 14.98f - tokenSizeFactor)
                            val clampedCellY = rawCellY.coerceIn(0.02f, 14.98f - tokenSizeFactor)

                            // Determine clickability
                            val isMyTurnToClick = if (state.gameMode == LudoGameMode.HYBRID_ONLINE) {
                                token.playerId == viewModel.myFirebasePlayerSlot && state.currentPlayerIdx == viewModel.myFirebasePlayerSlot
                            } else {
                                token.playerId == currentPlayer?.id && currentPlayer?.type == PlayerType.HUMAN
                            }

                            val isTokenClickable = state.gamePhase == GamePhase.PLAYING &&
                                    state.hasRolled &&
                                    !state.isRolling &&
                                    !state.isMovingToken &&
                                    isMyTurnToClick &&
                                    state.diceRoll != null &&
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
                                2 -> Brush.radialGradient( // YELLOW - High contrast warm golden amber with rich dark border
                                    colors = listOf(Color(0xFFFFEA00), Color(0xFFEAB308), Color(0xFF78350F))
                                )
                                else -> Brush.radialGradient( // BLUE
                                    colors = listOf(Color(0xFF64D8FF), Color(0xFF1565C0), Color(0xFF08254E))
                                )
                            }

                            val metallicBorder = if (isTokenClickable) {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFF5A0), Color(0xFFFFD700), Color(0xFFFFF5A0), Color(0xFFB45309), Color(0xFFFFD700))
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFB0BEC5), Color(0xFFFFFFFF), Color(0xFF546E7A), Color(0xFFFFFFFF))
                                )
                            }

                            val normalSize = cellSize * tokenSizeFactor

                            val pawnWidth = normalSize * 0.72f
                            val pawnHeight = pawnWidth * 1.25f

                            val baseModifier = Modifier
                                .size(normalSize)
                                .offset(
                                    x = cellSize * clampedCellX,
                                    y = cellSize * clampedCellY
                                )
                                .then(modifierWithPulse)

                            val tokenModifier = if (state.selectedTokenStyle == LudoTokenStyle.GLOSSY_3D) {
                                baseModifier
                                    .shadow(if (isTokenClickable) 10.dp else 3.dp, CircleShape, spotColor = Color.Black)
                                    .drawBehind {
                                        val radius = size.minDimension / 2
                                        val centerPt = Offset(size.width / 2, size.height / 2)

                                        // 0. Golden Glowing Aura Ring for playable tokens ("Saini" highlight)
                                        if (isTokenClickable) {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(Color(0xFFFFD700).copy(alpha = 0.85f), Color(0xFFFFD700).copy(alpha = 0.0f))
                                                ),
                                                radius = radius * 1.55f,
                                                center = centerPt
                                            )
                                        }

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
                                                width = if (isTokenClickable) 3.5.dp.toPx() else 1.8.dp.toPx()
                                            ),
                                            center = centerPt
                                        )

                                        // 3. Inner border accent for elegant depth
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.35f),
                                            radius = radius - (if (isTokenClickable) 4.5.dp.toPx() else 3.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.9.dp.toPx()),
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
                                                    Color.White.copy(alpha = 0.50f),
                                                    Color.White.copy(alpha = 0.0f)
                                                ),
                                                startY = centerPt.y - radius,
                                                endY = centerPt.y + radius * 0.1f
                                            )
                                        )

                                        // 5. Bright highlight pin-point / dot reflect (top-left)
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.90f),
                                            radius = 2.2.dp.toPx(),
                                            center = Offset(centerPt.x - radius * 0.42f, centerPt.y - radius * 0.42f)
                                        )
                                    }
                            } else if (state.selectedTokenStyle == LudoTokenStyle.CLASSIC_PAWN) {
                                val standardX = cellSize * clampedCellX
                                val standardY = cellSize * clampedCellY
                                
                                val rawAdjustedX = standardX + (normalSize - pawnWidth) / 2
                                val rawAdjustedY = standardY + normalSize - pawnHeight

                                val adjustedX = rawAdjustedX.coerceIn(0.dp, cellSize * 15f - pawnWidth)
                                val adjustedY = rawAdjustedY.coerceIn(0.dp, cellSize * 15f - pawnHeight)

                                Modifier
                                    .width(pawnWidth)
                                    .height(pawnHeight)
                                    .offset(x = adjustedX, y = adjustedY)
                                    .then(modifierWithPulse)
                                    .drawBehind {
                                        val w = size.width
                                        val h = size.height

                                        // Get base player colors and stroke color from SVG gradient values
                                        val (lightColor, mainColor, darkColor, strokeColor) = when (token.playerId) {
                                            0 -> listOf(Color(0xFFFF7A68), Color(0xFFE0362A), Color(0xFF7A1710), Color(0xFF6B1109)) // RED
                                            1 -> listOf(Color(0xFF6DE79A), Color(0xFF22A85C), Color(0xFF0C4D29), Color(0xFF0A3A1E)) // GREEN
                                            2 -> listOf(Color(0xFFFFF066), Color(0xFFEAB308), Color(0xFFB45309), Color(0xFF451A03)) // YELLOW (High Contrast)
                                            else -> listOf(Color(0xFF7CC4FF), Color(0xFF2470C4), Color(0xFF0F3255), Color(0xFF0D2745)) // BLUE
                                        }

                                        val strokeWidth = if (isTokenClickable) 2.5.dp.toPx() else 1.2.dp.toPx()
                                        val finalStrokeColor = if (isTokenClickable) Color(0xFFFFD700) else strokeColor

                                        // 0. Glowing aura behind pawn if playable
                                        if (isTokenClickable) {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(Color(0xFFFFD700).copy(alpha = 0.80f), Color(0xFFFFD700).copy(alpha = 0.0f))
                                                ),
                                                radius = w * 0.95f,
                                                center = Offset(w * 0.5f, h * 0.5f)
                                            )
                                        }

                                        // 1. Draw soft radial shadow at the bottom
                                        val shadowCx = w * 0.5f
                                        val shadowCy = h * 0.96f
                                        val shadowRx = w * 0.45f
                                        val shadowRy = h * 0.06f
                                        
                                        drawOval(
                                            brush = Brush.radialGradient(
                                                colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
                                                center = Offset(shadowCx, shadowCy),
                                                radius = shadowRx
                                            ),
                                            topLeft = Offset(shadowCx - shadowRx, shadowCy - shadowRy),
                                            size = androidx.compose.ui.geometry.Size(shadowRx * 2f, shadowRy * 2f)
                                        )

                                        // 2. Draw pawn body path (mapped to a classic, elegantly curved pawn shape)
                                        val bodyPath = Path().apply {
                                            moveTo(w * 0.15f, h * 0.95f)
                                            quadraticTo(w * 0.15f, h * 0.65f, w * 0.35f, h * 0.5f) // waist
                                            quadraticTo(w * 0.28f, h * 0.42f, w * 0.32f, h * 0.35f) // collar
                                            quadraticTo(w * 0.32f, h * 0.28f, w * 0.5f, h * 0.28f) // neck top center
                                            quadraticTo(w * 0.68f, h * 0.28f, w * 0.68f, h * 0.35f)
                                            quadraticTo(w * 0.72f, h * 0.42f, w * 0.65f, h * 0.5f)
                                            quadraticTo(w * 0.85f, h * 0.65f, w * 0.85f, h * 0.95f)
                                            close()
                                        }

                                        // Fill body with beautiful 3D linear gradient (light to dark)
                                        val bodyFillBrush = Brush.linearGradient(
                                            colors = listOf(lightColor, mainColor, darkColor),
                                            start = Offset(w * 0.15f, h * 0.28f),
                                            end = Offset(w * 0.85f, h * 0.95f)
                                        )
                                        drawPath(path = bodyPath, brush = bodyFillBrush)
                                        
                                        // Body stroke outline
                                        drawPath(
                                            path = bodyPath,
                                            color = finalStrokeColor,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                        )

                                        // 3. Draw head circle
                                        val headRadius = w * 0.25f
                                        val headCenter = Offset(w * 0.5f, h * 0.26f)
                                        
                                        val headFillBrush = Brush.linearGradient(
                                            colors = listOf(lightColor, mainColor, darkColor),
                                            start = Offset(headCenter.x - headRadius, headCenter.y - headRadius),
                                            end = Offset(headCenter.x + headRadius, headCenter.y + headRadius)
                                        )
                                        drawCircle(
                                            brush = headFillBrush,
                                            radius = headRadius,
                                            center = headCenter
                                        )
                                        drawCircle(
                                            color = finalStrokeColor,
                                            radius = headRadius,
                                            center = headCenter,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                        )

                                        // 4. Draw pinnacle/inner accent circle
                                        val pinRadius = w * 0.06f
                                        val pinCenter = Offset(w * 0.5f, h * 0.06f)
                                        drawCircle(
                                            color = strokeColor,
                                            radius = pinRadius,
                                            center = pinCenter
                                        )

                                        // 5. Draw body highlight (radialGradient id=highlight)
                                        val bodyHighlightBrush = Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.White.copy(alpha = 0.8f),
                                                0.5f to Color.White.copy(alpha = 0.15f),
                                                1.0f to Color.Transparent
                                            ),
                                            center = Offset(w * 0.45f, h * 0.55f),
                                            radius = w * 0.45f
                                        )
                                        drawPath(path = bodyPath, brush = bodyHighlightBrush)

                                        // 6. Draw head highlight (radialGradient id=highlight)
                                        val headHighlightBrush = Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.White.copy(alpha = 0.85f),
                                                0.45f to Color.White.copy(alpha = 0.18f),
                                                1.0f to Color.Transparent
                                            ),
                                            center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.45f),
                                            radius = headRadius * 1.3f
                                        )
                                        drawCircle(
                                            brush = headHighlightBrush,
                                            radius = headRadius,
                                            center = headCenter
                                        )
                                    }
                            } else {
                                val normalBorderColor = if (token.playerId == 2) Color(0xFF78350F) else if (tokenList.size > 1) Color(0xFF111827) else Color.White
                                baseModifier
                                    .shadow(if (isTokenClickable) 10.dp else 4.dp, CircleShape)
                                    .background(token.color.value, CircleShape)
                                    .border(
                                        width = if (isTokenClickable) 3.dp else if (tokenList.size > 1) 1.8.dp else 1.5.dp,
                                        color = if (isTokenClickable) Color(0xFFFFD700) else normalBorderColor,
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
                                    verticalArrangement = Arrangement.Center,
                                    modifier = if (state.selectedTokenStyle == LudoTokenStyle.CLASSIC_PAWN) {
                                        Modifier.offset(y = pawnHeight * -0.24f)
                                    } else Modifier
                                ) {
                                if (state.selectedTokenStyle == LudoTokenStyle.GLOSSY_3D || state.selectedTokenStyle == LudoTokenStyle.CLASSIC_PAWN) {
                                    // Smooth 3D Glossy and Classic Pawns are clean and smooth without any overlays!
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
                                        Text(
                                            text = emoji,
                                            fontSize = if (isAtHome) 9.sp else if (tokenList.size > 1) 12.sp else 16.sp,
                                            style = androidx.compose.ui.text.TextStyle(
                                                shadow = androidx.compose.ui.graphics.Shadow(
                                                    color = Color.Black.copy(alpha = 0.4f),
                                                    offset = Offset(0f, 1f),
                                                    blurRadius = 2f
                                                )
                                            )
                                        )
                                    } else {
                                        // Standard Ludo coin nested inner concentric ring
                                        Box(
                                            modifier = Modifier
                                                .size(if (isAtHome) cellSize * 0.25f else if (tokenList.size > 1) cellSize * 0.20f else cellSize * 0.42f)
                                                .background(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.2f))
                                                    ),
                                                    shape = CircleShape
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    shape = CircleShape
                                                )
                                        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .zIndex(2f),
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
                                    if (isInternetAvailable(context)) {
                                        viewModel.triggerAd(AdType.GUARANTEED_SIX)
                                    } else {
                                        val title = LudoTranslations.getTranslation("internet_required_title", state.selectedLanguage)
                                        android.widget.Toast.makeText(
                                            context,
                                            title,
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
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

                            if (state.gameMode == LudoGameMode.HYBRID_ONLINE) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (state.selectedLanguage.code.contains("hi")) "🤝 ऑनलाइन खिलाड़ियों को दोस्त बनाएं:" else "🤝 Add Online Players as Friends:",
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                
                                val otherPlayers = state.players.filter { it.id != 3 }
                                if (otherPlayers.isNotEmpty()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                    ) {
                                        otherPlayers.forEach { player ->
                                            val isAdded = state.addedFriends.contains(player.id)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF2E2A72), RoundedCornerShape(12.dp))
                                                    .border(1.dp, Color(0xFF4F46E5), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .background(player.color.value, CircleShape)
                                                    )
                                                    Text(
                                                        text = player.name,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        if (!isAdded) {
                                                            viewModel.addFriend(player.name, player.id)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isAdded) Color(0xFF10B981) else Color(0xFFFFD700),
                                                        contentColor = if (isAdded) Color.White else Color(0xFF0F172A)
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (isAdded) {
                                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Text(
                                                                text = if (state.selectedLanguage.code.contains("hi")) "दोस्त बन गए" else "Friends",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        } else {
                                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Text(
                                                                text = if (state.selectedLanguage.code.contains("hi")) "दोस्त बनाएं" else "Add Friend",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

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
        val descKey = if (isWagerMode) "back_confirm_desc" else "back_confirm_desc_no_wager"
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
                    text = LudoTranslations.getTranslation(descKey, state.selectedLanguage),
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

    if (showChatDialog) {
        AlertDialog(
            onDismissRequest = { showChatDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFFFFD700))
                    Text(
                        text = if (state.selectedLanguage.code.contains("hi")) "त्वरित चैट / Quick Chat" else "Quick Chat",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            containerColor = Color(0xFF1E1B4B),
            textContentColor = Color.White,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (state.selectedLanguage.code.contains("hi")) "एक संदेश चुनें:" else "Select a message:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val quickMessages = listOf(
                        "🦁 Lion Roar! 💪" to "🦁 शेर की दहाड़! 💪",
                        "🐆 Black Panther! ⚡" to "🐆 ब्लैक पैंथर चाल! ⚡",
                        "👸 Love Queen! 💖" to "👸 लव क्वीन लव! 💖",
                        "👧 Girl Power! 🎀" to "👧 गर्ल पावर! 🎀",
                        "💖 Love You All! 💕" to "💖 प्यार भरा खेल! 💕",
                        "Hello! 👋" to "हेलो! 👋",
                        "Play fast! ⏳" to "जल्दी खेलो भाई! ⏳",
                        "Oh no! 🤦‍♂️" to "अरे यार! 🤦‍♂️",
                        "Congratulations! 🏆" to "बधाई हो! 🏆",
                        "Wow! 😮" to "क्या बात है! 😮",
                        "Amazing game! 🤩" to "मजा आ गया! 🤩",
                        "Your turn! 🎲" to "चल भाई चाल चल! 🎲",
                        "Good luck! 👍" to "शुभकामनाएं! 👍"
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(quickMessages) { (eng, hin) ->
                            val msgText = if (state.selectedLanguage.code.contains("hi")) hin else eng
                            Surface(
                                onClick = {
                                    viewModel.sendUserChatMessage(msgText)
                                    showChatDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF2E2A72),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF4F46E5), RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msgText,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    var customMsg by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customMsg,
                            onValueChange = { customMsg = it },
                            placeholder = { Text("Type here...", color = Color.Gray, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF4F46E5),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (customMsg.isNotBlank()) {
                                    viewModel.sendUserChatMessage(customMsg.trim())
                                    customMsg = ""
                                    showChatDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700),
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Send", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChatDialog = false }) {
                    Text(
                        text = if (state.selectedLanguage.code.contains("hi")) "बंद करें" else "Close",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Interactive Fullscreen Interstitial Sponsor Ad Player Overlay
    if (state.adType != null) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
            ) {
                // Background Glow Effects
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF312E81), Color(0xFF0F172A)),
                                radius = 900f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: Interstitial Tag & Countdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E1B4B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "🎬 SPONSORED AD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Close / Skip Button or Timer Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFD700),
                            modifier = Modifier.clickable { viewModel.dismissAd() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (state.adSecondsLeft > 0) "Skip in ${state.adSecondsLeft}s ✕" else "Close ✕",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    // Center Content: Interstitial Video/Sponsor Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF3730A3), Color(0xFF1E1B4B))
                                    )
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = Color(0xFFFFD700)
                                )

                                Text(
                                    text = when (state.adType) {
                                        AdType.GUARANTEED_SIX -> LudoTranslations.getTranslation("ad_guaranteed_six", state.selectedLanguage)
                                        AdType.EXTEND_TIME -> LudoTranslations.getTranslation("ad_extend_time", state.selectedLanguage)
                                        AdType.GAME_FINISH -> LudoTranslations.getTranslation("ad_game_finish", state.selectedLanguage)
                                        AdType.RESET -> LudoTranslations.getTranslation("ad_reset", state.selectedLanguage)
                                        else -> LudoTranslations.getTranslation("ad_watching", state.selectedLanguage)
                                    },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = LudoTranslations.getTranslation("reward_claims", state.selectedLanguage).replace("%d", state.adSecondsLeft.toString()),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD700),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                CircularProgressIndicator(
                                    color = Color(0xFFFFD700),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // Bottom Bar Progress Indicator
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { (5f - state.adSecondsLeft) / 5f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFFD700),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )

                        Text(
                            text = "Sponsored Interstitial Video Ad",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // Time's Up Reward Dialog (Extend Time)
    if (state.isTimeUpDialogShowing) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = if (state.selectedLanguage.code.contains("hi")) "⏳ समय समाप्त! (TIME IS UP)" else "⏳ TIME IS UP! (समय समाप्त)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (state.selectedLanguage.code.contains("hi")) {
                            "मैच का समय समाप्त हो गया है! क्या आप एक प्रायोजक विज्ञापन देखकर खेल को 5 मिनट और बढ़ाना चाहते हैं या गेम बंद करना चाहते हैं?"
                        } else {
                            "The match timer has run out! Would you like to extend the game by 5 more minutes by watching a sponsor ad, or quit the game?"
                        },
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
                            Text(
                                text = if (state.selectedLanguage.code.contains("hi")) "🎲 विज्ञापन देखें (+5 मिनट)" else "🎲 Watch Ad (+5 Min)",
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTimeUpDialog() }) {
                    Text(
                        text = if (state.selectedLanguage.code.contains("hi")) "गेम बंद करें (Exit Game)" else "Exit Game (गेम बंद करें)",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
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
                            .padding(bottom = 1.dp),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Lvl ${player.level}",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(bottom = 2.dp),
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

                            // 2. Beautiful styled Avatar Sticker representing the player
                            val avatar = ludoAvatars.firstOrNull { it.id == player.avatarId } ?: ludoAvatars[0]
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        brush = Brush.linearGradient(colors = avatar.gradient),
                                        shape = CircleShape
                                    )
                                    .border(1.5.dp, avatar.frameColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = avatar.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else {
                            // Left-side players: User / Bot profile icon first (outer edge), then Team label, then Dice / Pansa (inner edge)
                            // 1. Beautiful styled Avatar Sticker representing the player
                            val avatar = ludoAvatars.firstOrNull { it.id == player.avatarId } ?: ludoAvatars[0]
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        brush = Brush.linearGradient(colors = avatar.gradient),
                                        shape = CircleShape
                                    )
                                    .border(1.5.dp, avatar.frameColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = avatar.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
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
                        text = "Lvl ${player.level}",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 2.dp),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = displayName,
                        color = if (isCurrentTurn) Color(0xFFFFD700) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Beautiful Floating Chat Bubble overlay with smooth enter/exit animations to prevent jitter
            androidx.compose.animation.AnimatedVisibility(
                visible = !bubbleText.isNullOrEmpty(),
                enter = androidx.compose.animation.fadeIn(animationSpec = tween(300)) + 
                        androidx.compose.animation.scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
                exit = androidx.compose.animation.fadeOut(animationSpec = tween(250)) + 
                       androidx.compose.animation.scaleOut(targetScale = 0.8f, animationSpec = tween(250)),
                modifier = Modifier
                    .align(if (isBottomPlayer) Alignment.TopCenter else Alignment.BottomCenter)
                    .offset(y = if (isBottomPlayer) (-65).dp else 65.dp)
                    .zIndex(10f)
            ) {
                if (bubbleText != null) {
                    ChatBubble(
                        message = bubbleText,
                        isTopRow = !isBottomPlayer
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
    val isYellow = tint == LudoColor.YELLOW.value || (tint.red > 0.8f && tint.green > 0.65f && tint.blue < 0.4f)
    val dotColor = if (isYellow) Color(0xFF78350F) else tint
    val dotBorder = if (isYellow) Color(0xFFEAB308) else Color.White.copy(alpha = 0.4f)

    @Composable
    fun Dot(sizeDp: androidx.compose.ui.unit.Dp) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .background(dotColor, CircleShape)
                .border(0.6.dp, dotBorder, CircleShape)
        )
    }

    Box(
        modifier = modifier
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            LudoDiceStyle.CLASSIC_DOTS -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (value) {
                        1 -> Dot(6.dp)
                        2 -> {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Box(modifier = Modifier.align(Alignment.Start)) { Dot(5.dp) }
                                Box(modifier = Modifier.align(Alignment.End)) { Dot(5.dp) }
                            }
                        }
                        3 -> {
                            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Box(modifier = Modifier.align(Alignment.TopStart)) { Dot(4.dp) }
                                Box(modifier = Modifier.align(Alignment.Center)) { Dot(4.dp) }
                                Box(modifier = Modifier.align(Alignment.BottomEnd)) { Dot(4.dp) }
                            }
                        }
                        4 -> {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Dot(4.dp)
                                    Dot(4.dp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Dot(4.dp)
                                    Dot(4.dp)
                                }
                            }
                        }
                        5 -> {
                            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                                    Dot(4.dp)
                                    Dot(4.dp)
                                }
                                Box(modifier = Modifier.align(Alignment.Center)) { Dot(4.dp) }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                                    Dot(4.dp)
                                    Dot(4.dp)
                                }
                            }
                        }
                        6 -> {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Dot(4.dp)
                                    Dot(4.dp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Dot(4.dp)
                                    Dot(4.dp)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Dot(4.dp)
                                    Dot(4.dp)
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
                        color = if (isYellow) Color(0xFFB45309) else tint
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
