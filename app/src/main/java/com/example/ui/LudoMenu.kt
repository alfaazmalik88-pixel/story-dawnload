package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LudoLanguage
import com.example.model.LudoTranslations
import com.example.model.GamePhase
import com.example.model.LudoColor
import com.example.model.LudoTheme
import com.example.model.LudoTokenStyle
import com.example.model.LudoDiceStyle
import com.example.model.LudoGameMode
import com.example.model.LudoViewModel
import com.example.model.PlayerType
import com.example.model.OnlineSubMode
import com.example.model.AdType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoMenu(
    viewModel: LudoViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Claim button pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_claim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val claimScale = if (uiState.isDailyRewardAvailable) pulseScale else 1.0f

    var showRulesDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNoInternetDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Luxury Deep Royal Navy Blue & Charcoal background gradient
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Deep dark slate/navy blue
            Color(0xFF0A0F24), // Luxury Deep Royal Navy Blue
            Color(0xFF030712)  // Deepest Charcoal Black
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .systemBarsPadding()
    ) {
        // 1. Draw subtle premium luxury golden geometric grid
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.04f)) {
            val stepX = 50.dp.toPx()
            for (i in -20..35) {
                drawLine(
                    color = Color(0xFFFFD700),
                    start = androidx.compose.ui.geometry.Offset(i * stepX, 0f),
                    end = androidx.compose.ui.geometry.Offset((i + 15) * stepX, size.height),
                    strokeWidth = 0.8f.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFFFD700),
                    start = androidx.compose.ui.geometry.Offset(i * stepX, 0f),
                    end = androidx.compose.ui.geometry.Offset((i - 15) * stepX, size.height),
                    strokeWidth = 0.8f.dp.toPx()
                )
            }
        }

        // 2. Draw cybernetic circuitry traces in electric cyan/blue matching the uploaded image's background
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.18f)) {
            val width = size.width
            val height = size.height

            // Trace 1: Top-Left circuit
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height * 0.15f)
                    lineTo(width * 0.2f, height * 0.15f)
                    lineTo(width * 0.32f, height * 0.24f)
                    lineTo(width * 0.32f, height * 0.32f)
                },
                color = Color(0xFF00D2FF),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
            )
            drawCircle(Color(0xFF00D2FF), radius = 3.5f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * 0.32f, height * 0.32f))

            // Trace 2: Top-Right circuit
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(width, height * 0.12f)
                    lineTo(width * 0.82f, height * 0.12f)
                    lineTo(width * 0.68f, height * 0.22f)
                    lineTo(width * 0.68f, height * 0.28f)
                },
                color = Color(0xFF00D2FF),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
            )
            drawCircle(Color(0xFF00D2FF), radius = 3.5f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * 0.68f, height * 0.28f))

            // Trace 3: Bottom-Left circuit
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height * 0.72f)
                    lineTo(width * 0.22f, height * 0.72f)
                    lineTo(width * 0.36f, height * 0.62f)
                },
                color = Color(0xFF00D2FF),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
            )
            drawCircle(Color(0xFF00D2FF), radius = 3.5f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * 0.36f, height * 0.62f))

            // Trace 4: Bottom-Right circuit
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(width, height * 0.78f)
                    lineTo(width * 0.72f, height * 0.78f)
                    lineTo(width * 0.58f, height * 0.68f)
                },
                color = Color(0xFF00D2FF),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
            )
            drawCircle(Color(0xFF00D2FF), radius = 3.5f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(width * 0.58f, height * 0.68f))
        }

        // 3. Elegant, subtle 3D Perspective Ludo Championship Board (Centered at the bottom, matching the background of the image)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.BottomCenter)
                .alpha(0.25f) // Keeps it as an elegant watermark background so interactive UI is perfectly readable
                .graphicsLayer {
                    rotationX = 55f
                    rotationZ = -22f
                    cameraDistance = 16f
                }
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.Center)
                    .shadow(32.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFFFD700))
                    .background(Color(0xFF0F172A))
                    .border(
                        width = 4.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFDF73), // Shiny Gold Highlight
                                Color(0xFFD4AF37), // Metallic Gold
                                Color(0xFF8A640F), // Antique Gold
                                Color(0xFFFFDF73)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                // Classic Ludo bases representation
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEF4444).copy(alpha = 0.85f))) // Red Base
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF10B981).copy(alpha = 0.85f))) // Green Base
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF3B82F6).copy(alpha = 0.85f))) // Blue Base
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF59E0B).copy(alpha = 0.85f))) // Yellow Base
                    }
                }
                
                // Outer 3D gold highlight rim
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.92f)
                        .align(Alignment.Center)
                        .border(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                )

                // Central Home Star/Emblem with glowing gold crown dice pocket
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .align(Alignment.Center)
                        .shadow(12.dp, CircleShape, spotColor = Color(0xFFFFD700))
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF2A66FF), Color(0xFF0F172A))
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFDF73), Color(0xFFD4AF37))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👑",
                        fontSize = 26.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row - Balanced Symmetrical Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Volume and Language
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x22FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (uiState.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Sound",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x22FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Select Language",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Right: Rules and Settings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showRulesDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x22FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Rules & Guide",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x22FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- EXTREMELY COMPACT USER PROFILE & COIN HUB (ABOVE TITLE) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Avatar, Username, Coins Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Circle (Small & Sleek)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                ),
                                shape = CircleShape
                            )
                            .border(1.dp, Color(0xFFFFD700), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.username.isNotEmpty()) uiState.username.first().uppercase() else "L",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    // Username & Coins badge
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable {
                                newNameInput = uiState.username
                                showRenameDialog = true
                            }
                        ) {
                            Text(
                                text = uiState.username,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = LudoTranslations.getTranslation("edit_username", uiState.selectedLanguage),
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🪙", fontSize = 11.sp)
                            Text(
                                text = "${uiState.coins}",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Right: Mini Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Daily Check-in Button (Mini)
                    val dailyBtnBg = if (uiState.isDailyRewardAvailable) {
                        Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF334155)))
                    }
                    
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = claimScale
                                scaleY = claimScale
                            }
                            .clip(RoundedCornerShape(6.dp))
                            .background(dailyBtnBg)
                            .clickable(enabled = uiState.isDailyRewardAvailable) {
                                viewModel.claimDailyReward()
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.isDailyRewardAvailable) {
                                "🎁 Claim"
                            } else {
                                "Claimed ✓"
                            },
                            color = if (uiState.isDailyRewardAvailable) Color.White else Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    // Watch Ad Button (Mini)
                    val isWatchAdOnCooldown = uiState.watchAdCooldownRemaining > 0
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (isWatchAdOnCooldown) {
                                    Modifier.background(Color.Gray.copy(alpha = 0.5f))
                                } else {
                                    Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))))
                                }
                            )
                            .clickable(enabled = !isWatchAdOnCooldown) {
                                viewModel.triggerAd(AdType.WATCH_AD)
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (isWatchAdOnCooldown) {
                                val mins = (uiState.watchAdCooldownRemaining / 60).toString().padStart(2, '0')
                                val secs = (uiState.watchAdCooldownRemaining % 60).toString().padStart(2, '0')
                                Text(
                                    text = "⏳ $mins:$secs",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "+500 🪙",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Shop Button (Mini)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C))))
                            .clickable {
                                showShopDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = LudoTranslations.getTranslation("shop_title", uiState.selectedLanguage),
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "🛒 Shop",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pulsing animation for the crown logo
            val crownPulseTransition = rememberInfiniteTransition(label = "crown_pulse")
            val crownScale by crownPulseTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "crown_scale"
            )
            val crownGlow by crownPulseTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "crown_glow"
            )

            if (uiState.gamePhase == GamePhase.MODE_SELECT) {
                // Centered Premium Luxury Crown Ludo Logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .graphicsLayer {
                                scaleX = crownScale
                                scaleY = crownScale
                            }
                            .shadow(20.dp * crownGlow, CircleShape, spotColor = Color(0xFFFFD700))
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2A66FF), // Glowing Royal Blue Center
                                        Color(0xFF0F172A)  // Deep Midnight Blue Edge
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 3.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFDF73), // Shiny Gold Highlight
                                        Color(0xFFD4AF37), // Metallic Gold
                                        Color(0xFF8A640F), // Antique Gold
                                        Color(0xFFFFDF73)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👑",
                            fontSize = 38.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CROWN LUDO",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            color = Color(0xFFFFD700),
                            fontSize = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "LUXURY CHAMPIONSHIP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (uiState.gamePhase == GamePhase.SETUP) {
                // Compact setup screen title row (Extremely elegant, zero scrolling needed!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👑",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = LudoTranslations.getTranslation("title", uiState.selectedLanguage).uppercase() + " - SETUP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700),
                            letterSpacing = 2.sp,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            // Rename Username Dialog
            if (showRenameDialog) {
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = {
                        Text(
                            text = LudoTranslations.getTranslation("enter_name_title", uiState.selectedLanguage),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = newNameInput,
                            onValueChange = { if (it.length <= 15) newNameInput = it },
                            placeholder = {
                                Text(
                                    text = LudoTranslations.getTranslation("enter_name_placeholder", uiState.selectedLanguage),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (newNameInput.trim().isNotEmpty()) {
                                        viewModel.updateUsername(newNameInput)
                                        showRenameDialog = false
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newNameInput.trim().isNotEmpty()) {
                                    viewModel.updateUsername(newNameInput)
                                    showRenameDialog = false
                                }
                            }
                        ) {
                            Text(
                                text = LudoTranslations.getTranslation("save", uiState.selectedLanguage),
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameDialog = false }) {
                            Text(
                                text = LudoTranslations.getTranslation("cancel", uiState.selectedLanguage),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    containerColor = Color(0xFF1E1B4B),
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }

            // Theme & Token Shop Dialog
            if (showShopDialog) {
                var shopError by remember { mutableStateOf<String?>(null) }
                var activeShopTab by remember { mutableStateOf(0) } // 0: Themes, 1: Tokens, 2: Dice

                AlertDialog(
                    onDismissRequest = { 
                        showShopDialog = false 
                        shopError = null
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🛍️", fontSize = 20.sp)
                            Text(
                                text = LudoTranslations.getTranslation("shop_title", uiState.selectedLanguage),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // User's current coins badge
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Balance:",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🪙", fontSize = 14.sp)
                                    Text(
                                        text = "${uiState.coins}",
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Custom Interactive Tab Selector for Shop Sections
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0FFFFFFF), RoundedCornerShape(10.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val tabs = listOf("🎨 Themes", "📍 Tokens", "🎲 Dice")
                                tabs.forEachIndexed { index, title ->
                                    val isSelected = activeShopTab == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF3B82F6) else Color.Transparent)
                                            .clickable { 
                                                activeShopTab = index 
                                                shopError = null
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            if (shopError != null) {
                                Text(
                                    text = shopError!!,
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Scrollable list container for shop items based on the active tab
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (activeShopTab) {
                                    0 -> {
                                        // Tab 0: Themes (8 items)
                                        LudoTheme.values().forEach { theme ->
                                            val isUnlocked = uiState.unlockedThemes.contains(theme)
                                            val isSelected = uiState.selectedTheme == theme
                                            val cost = viewModel.getThemeCost(theme)

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Left info
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val emoji = when (theme) {
                                                                LudoTheme.CLASSIC -> "🪵"
                                                                LudoTheme.COSMIC -> "🌌"
                                                                LudoTheme.ROYAL -> "👑"
                                                                LudoTheme.FOREST -> "🌲"
                                                                LudoTheme.CANDY -> "🍬"
                                                                LudoTheme.OCEAN -> "🌊"
                                                                LudoTheme.CYBERPUNK -> "⚡"
                                                                LudoTheme.EGYPT -> "🏺"
                                                            }
                                                            Text(emoji, fontSize = 16.sp)
                                                            Text(
                                                                text = theme.displayName,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Token: ${theme.pawnName}",
                                                            color = Color.White.copy(alpha = 0.6f),
                                                            fontSize = 10.sp
                                                        )
                                                        Text(
                                                            text = "Dice: ${theme.diceName}",
                                                            color = Color.White.copy(alpha = 0.6f),
                                                            fontSize = 10.sp
                                                        )
                                                    }

                                                    // Right button / status
                                                    if (isUnlocked) {
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(Color(0x2210B981))
                                                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Active ✓",
                                                                    color = Color(0xFF10B981),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = { 
                                                                    viewModel.selectTheme(theme)
                                                                    shopError = null
                                                                },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = Color(0xFF3B82F6)
                                                                ),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                                modifier = Modifier.height(28.dp),
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = LudoTranslations.getTranslation("use_btn", uiState.selectedLanguage),
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = {
                                                                if (uiState.coins >= cost) {
                                                                    viewModel.unlockTheme(theme)
                                                                    shopError = null
                                                                } else {
                                                                    shopError = "Not enough coins! Watch sponsor ads to earn more."
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFFF59E0B)
                                                            ),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(28.dp),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "${cost} 🪙",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        // Tab 1: Token Styles (6 items)
                                        LudoTokenStyle.values().forEach { tokenStyle ->
                                            val isUnlocked = uiState.unlockedTokenStyles.contains(tokenStyle)
                                            val isSelected = uiState.selectedTokenStyle == tokenStyle
                                            val cost = tokenStyle.cost

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(tokenStyle.emoji, fontSize = 22.sp)
                                                        Column {
                                                            Text(
                                                                text = tokenStyle.displayName,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
                                                            )
                                                            Text(
                                                                text = "Custom game piece style",
                                                                color = Color.White.copy(alpha = 0.6f),
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                    }

                                                    if (isUnlocked) {
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(Color(0x2210B981))
                                                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Active ✓",
                                                                    color = Color(0xFF10B981),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = { 
                                                                    viewModel.selectTokenStyle(tokenStyle)
                                                                    shopError = null
                                                                },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = Color(0xFF3B82F6)
                                                                ),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                                modifier = Modifier.height(28.dp),
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = LudoTranslations.getTranslation("use_btn", uiState.selectedLanguage),
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = {
                                                                if (uiState.coins >= cost) {
                                                                    viewModel.unlockTokenStyle(tokenStyle)
                                                                    shopError = null
                                                                } else {
                                                                    shopError = "Not enough coins! Watch sponsor ads to earn more."
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFFF59E0B)
                                                            ),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(28.dp),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "${cost} 🪙",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        // Tab 2: Dice Styles (6 items)
                                        LudoDiceStyle.values().forEach { diceStyle ->
                                            val isUnlocked = uiState.unlockedDiceStyles.contains(diceStyle)
                                            val isSelected = uiState.selectedDiceStyle == diceStyle
                                            val cost = diceStyle.cost

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(diceStyle.emoji, fontSize = 22.sp)
                                                        Column {
                                                            Text(
                                                                text = diceStyle.displayName,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
                                                            )
                                                            Text(
                                                                text = "Custom dice style",
                                                                color = Color.White.copy(alpha = 0.6f),
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                    }

                                                    if (isUnlocked) {
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(Color(0x2210B981))
                                                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Active ✓",
                                                                    color = Color(0xFF10B981),
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = { 
                                                                    viewModel.selectDiceStyle(diceStyle)
                                                                    shopError = null
                                                                },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = Color(0xFF3B82F6)
                                                                ),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                                modifier = Modifier.height(28.dp),
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = LudoTranslations.getTranslation("use_btn", uiState.selectedLanguage),
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = {
                                                                if (uiState.coins >= cost) {
                                                                    viewModel.unlockDiceStyle(diceStyle)
                                                                    shopError = null
                                                                } else {
                                                                    shopError = "Not enough coins! Watch sponsor ads to earn more."
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFFF59E0B)
                                                            ),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(28.dp),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "${cost} 🪙",
                                                                color = Color.White,
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
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { 
                                showShopDialog = false 
                                shopError = null
                            }
                        ) {
                            Text(
                                text = "Close",
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    containerColor = Color(0xFF1E1B4B),
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (uiState.gamePhase == GamePhase.MODE_SELECT) {
                // Subtle Mode select tag - clean and extremely small to avoid scrolling
                Text(
                    text = LudoTranslations.getTranslation("title", uiState.selectedLanguage).uppercase() + " - MODE",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Beautiful 2x2 Grid of game modes with Online Game featured on top!
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Featured Online Game at the very top (above all others!)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GameModeCard(
                            title = LudoTranslations.getTranslation("hybrid_online_title", uiState.selectedLanguage) + " 📶",
                            subtitle = LudoTranslations.getTranslation("hybrid_online_desc", uiState.selectedLanguage),
                            icon = Icons.Default.Language,
                            gradientColors = listOf(Color(0xFFEF4444), Color(0xFFF97316)), // Vibrant Fire Red/Orange
                            testTag = "mode_hybrid_online",
                            onClick = {
                                if (isInternetAvailable(context)) {
                                    viewModel.selectGameMode(LudoGameMode.HYBRID_ONLINE)
                                } else {
                                    showNoInternetDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 2x2 Grid for the other 4 options below
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 2. Classic Game
                        GameModeCard(
                            title = LudoTranslations.getTranslation("classic_title", uiState.selectedLanguage),
                            subtitle = LudoTranslations.getTranslation("classic_desc", uiState.selectedLanguage),
                            icon = Icons.Default.Casino,
                            gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)), // Emerald Green
                            testTag = "mode_classic",
                            onClick = { viewModel.selectGameMode(LudoGameMode.CLASSIC) },
                            modifier = Modifier.weight(1f)
                        )

                        // 3. 1v1 Game
                        GameModeCard(
                            title = LudoTranslations.getTranslation("one_vs_one_title", uiState.selectedLanguage),
                            subtitle = LudoTranslations.getTranslation("one_vs_one_desc", uiState.selectedLanguage),
                            icon = Icons.Default.Bolt,
                            gradientColors = listOf(Color(0xFFD946EF), Color(0xFF8B5CF6)), // Purple/Magenta
                            testTag = "mode_1v1",
                            onClick = {
                                if (isInternetAvailable(context)) {
                                    viewModel.selectGameMode(LudoGameMode.ONE_VS_ONE)
                                } else {
                                    showNoInternetDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 4. Computer Game
                        GameModeCard(
                            title = LudoTranslations.getTranslation("computer_title", uiState.selectedLanguage),
                            subtitle = LudoTranslations.getTranslation("computer_desc", uiState.selectedLanguage),
                            icon = Icons.Default.Android,
                            gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)), // Ocean Blue
                            testTag = "mode_computer",
                            onClick = {
                                viewModel.selectGameMode(LudoGameMode.VS_COMPUTER)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // 5. Team Up
                        GameModeCard(
                            title = LudoTranslations.getTranslation("team_up_title", uiState.selectedLanguage),
                            subtitle = LudoTranslations.getTranslation("team_up_desc", uiState.selectedLanguage),
                            icon = Icons.Default.Groups,
                            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)), // Orange/Gold
                            testTag = "mode_team_up",
                            onClick = { viewModel.selectGameMode(LudoGameMode.TEAM_UP) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else if (uiState.gamePhase == GamePhase.SETUP) {
                val selectedCount by viewModel.selectedPlayerCount.collectAsState()
                val selectedColor by viewModel.selectedUserColor.collectAsState()

                // Single unified glassmorphic console setup card containing all 3 selectors
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0x33B19FFB), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x99241F55))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Section 1: PLAYERS (CLASSIC, VS_COMPUTER, and HYBRID_ONLINE)
                        val isMultiplayerMode = uiState.gameMode == LudoGameMode.CLASSIC || 
                                                uiState.gameMode == LudoGameMode.VS_COMPUTER || 
                                                uiState.gameMode == LudoGameMode.HYBRID_ONLINE
                        if (isMultiplayerMode) {
                            Text(
                                text = LudoTranslations.getTranslation("players_count", uiState.selectedLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700),
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val counts = listOf(2, 3, 4)
                                counts.forEachIndexed { index, count ->
                                    CompactPlayerCountOption(
                                        count = count,
                                        selected = selectedCount == count,
                                        onClick = { viewModel.selectedPlayerCount.value = count }
                                    )
                                    if (index < counts.size - 1) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                        }

                        // Section 1B: ONLINE SUB-MODE SELECTOR (Only for HYBRID_ONLINE mode!)
                        if (uiState.gameMode == LudoGameMode.HYBRID_ONLINE) {
                            Text(
                                text = "⚡ SELECT ONLINE MODE / ऑनलाइन मोड",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700),
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val selectedSubMode by viewModel.selectedOnlineSubMode.collectAsState()
                                listOf(OnlineSubMode.CLASSIC, OnlineSubMode.QUICK_PLAY).forEachIndexed { idx, subMode ->
                                    val isSelected = selectedSubMode == subMode
                                    val title = if (subMode == OnlineSubMode.CLASSIC) "Classic 🏆" else "Quick Play ⚡"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFFEF4444) else Color(0x1AFFFFFF))
                                            .clickable { viewModel.selectOnlineSubMode(subMode) }
                                            .border(1.dp, if (isSelected) Color(0xFFFF9F9F) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (idx == 0) Spacer(modifier = Modifier.width(16.dp))
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                        }

                        // Section 2: YOUR COLOR
                        Text(
                            text = LudoTranslations.getTranslation("your_color", uiState.selectedLanguage),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        )
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(LudoColor.BLUE, LudoColor.GREEN, LudoColor.RED, LudoColor.YELLOW).forEachIndexed { index, color ->
                                CompactColorOption(
                                    color = color,
                                    selected = selectedColor == color,
                                    onClick = { viewModel.selectedUserColor.value = color }
                                )
                                if (index < 3) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                            }
                        }


                        // Section 3: Wager Selector (Only for 1v1 and Online modes!)
                        val isWagerSelectorVisible = uiState.gameMode == LudoGameMode.ONE_VS_ONE || 
                                                    uiState.gameMode == LudoGameMode.HYBRID_ONLINE
                        if (isWagerSelectorVisible) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                            Text(
                                text = "🪙 COIN ENTRY FEE / WAGER",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700),
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val wagers = listOf(500, 1000, 5000, 10000, 50000)
                                wagers.forEach { wager ->
                                    val isSelected = uiState.selectedWagerAmount == wager
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFFF59E0B) else Color(0x1AFFFFFF))
                                            .clickable { viewModel.selectWagerAmount(wager) }
                                            .border(1.dp, if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${wager}",
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Dynamic high-visibility inline warning banner if user has insufficient coins for the selected wager!
                            val hasInsufficientCoins = uiState.coins < uiState.selectedWagerAmount
                            if (hasInsufficientCoins) {
                                val errorMsg = LudoTranslations.getTranslation("not_enough_coins", uiState.selectedLanguage)
                                    .replace("%d", uiState.selectedWagerAmount.toString())
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF7F1D1D).copy(alpha = 0.85f))
                                        .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = "⚠️", fontSize = 16.sp)
                                        Column {
                                            Text(
                                                text = errorMsg,
                                                color = Color(0xFFFECDD3),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = if (uiState.selectedLanguage.code.contains("hi")) "🎁 नीचे से क्लेम करें या +500 कॉइन के लिए एड देखें!" else "🎁 Claim daily rewards or watch Ad to get +500 coins!",
                                                color = Color(0xFFFFD700),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Controls (Back button & PLAY button) side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button (Green, square with rounded corners)
                    IconButton(
                        onClick = { viewModel.resetToSetup() },
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .background(Color(0xFF4CAF50), RoundedCornerShape(16.dp)) // Bright lime green
                            .border(1.5.dp, Color(0xFFC5E1A5), RoundedCornerShape(16.dp)),
                        content = {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )

                    // PLAY button (Green, pill-shaped, glowing)
                    Button(
                        onClick = {
                            if (uiState.gameMode == LudoGameMode.ONE_VS_ONE && !isInternetAvailable(context)) {
                                showNoInternetDialog = true
                            } else {
                                viewModel.startGame()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(8.dp, RoundedCornerShape(25.dp))
                            .border(1.5.dp, Color(0xFFC5E1A5), RoundedCornerShape(25.dp))
                            .testTag("play_game_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50), // Vibrant green
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = LudoTranslations.getTranslation("start_game", uiState.selectedLanguage).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error or Status Warning Messages
            AnimatedVisibility(
                visible = uiState.statusMessage.startsWith("⚠️") || uiState.statusMessage.startsWith("❌"),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = uiState.statusMessage,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "👑 Developed by Kamar Pathan",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color(0xFFFFD700).copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Rules Guide Dialog
        if (showRulesDialog) {
            AlertDialog(
                onDismissRequest = { showRulesDialog = false },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Help, contentDescription = "Rules", tint = Color(0xFFFBC02D))
                        Text(LudoTranslations.getTranslation("rules_title", uiState.selectedLanguage), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RuleRow("1", LudoTranslations.getTranslation("rules_1", uiState.selectedLanguage))
                        RuleRow("2", LudoTranslations.getTranslation("rules_2", uiState.selectedLanguage))
                        RuleRow("3", LudoTranslations.getTranslation("rules_3", uiState.selectedLanguage))
                        RuleRow("4", LudoTranslations.getTranslation("rules_4", uiState.selectedLanguage))
                        RuleRow("5", LudoTranslations.getTranslation("rules_5", uiState.selectedLanguage))
                        RuleRow("6", LudoTranslations.getTranslation("rules_6", uiState.selectedLanguage))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRulesDialog = false }) {
                        Text(LudoTranslations.getTranslation("got_it", uiState.selectedLanguage).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFFFBC02D))
                    }
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Language Selection Dialog
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Language", tint = Color(0xFFFBC02D))
                        Text(LudoTranslations.getTranslation("choose_language", uiState.selectedLanguage), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LudoLanguage.values().forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (uiState.selectedLanguage == lang) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        width = if (uiState.selectedLanguage == lang) 2.dp else 1.dp,
                                        color = if (uiState.selectedLanguage == lang) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.selectLanguage(lang)
                                        showLanguageDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(lang.flag, fontSize = 24.sp)
                                    Column {
                                        Text(
                                            text = lang.countryName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = lang.label,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                if (uiState.selectedLanguage == lang) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(LudoTranslations.getTranslation("got_it", uiState.selectedLanguage).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFFFBC02D))
                    }
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                selectedLanguage = uiState.selectedLanguage,
                onDismissRequest = { showSettingsDialog = false }
            )
        }

        if (showNoInternetDialog) {
            AlertDialog(
                onDismissRequest = { showNoInternetDialog = false },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = "Offline", tint = Color(0xFFEF4444))
                        Text(
                            text = LudoTranslations.getTranslation("internet_required_title", uiState.selectedLanguage),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                text = {
                    Text(
                        text = LudoTranslations.getTranslation("internet_required_desc", uiState.selectedLanguage),
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showNoInternetDialog = false }) {
                        Text("OK", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                    }
                },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Interactive sponsor ad player overlay!
        if (uiState.adType != null) {
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
                        Text(
                            text = LudoTranslations.getTranslation("watching_ad", uiState.selectedLanguage),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = when (uiState.adType) {
                                AdType.GUARANTEED_SIX -> LudoTranslations.getTranslation("ad_guaranteed_six", uiState.selectedLanguage)
                                AdType.EXTEND_TIME -> LudoTranslations.getTranslation("ad_extend_time", uiState.selectedLanguage)
                                AdType.GAME_FINISH -> LudoTranslations.getTranslation("ad_game_finish", uiState.selectedLanguage)
                                AdType.RESET -> LudoTranslations.getTranslation("ad_reset", uiState.selectedLanguage)
                                AdType.WATCH_AD -> LudoTranslations.getTranslation("ad_watch_ad", uiState.selectedLanguage)
                                else -> LudoTranslations.getTranslation("ad_watching", uiState.selectedLanguage)
                            },
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = LudoTranslations.getTranslation("reward_claims", uiState.selectedLanguage).replace("%d", uiState.adSecondsLeft.toString()),
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color(0xFFFFD700)
                        )

                        LinearProgressIndicator(
                            progress = { (5f - uiState.adSecondsLeft) / 5f },
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

        // Pinned Banner Ad at the bottom!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            BannerAd()
        }
    }
}

@Composable
fun GameModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = testTag == "mode_hybrid_online"
    
    val infiniteTransition = rememberInfiniteTransition(label = "mode_card_anim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "earth_rotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Staggered floating and scaling animation based on the card's title hash
    // so they bounce elegantly out-of-sync
    val cardHash = title.hashCode().coerceAtLeast(0)
    val floatDuration = 1800 + (cardHash % 4) * 200
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = floatDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = floatDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )
    val iconSway by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = floatDuration + 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_sway"
    )

    val borderModifier = if (isOnline) {
        Modifier.border(
            width = (2 * glowPulse).dp,
            color = Color(0xFFFFD700).copy(alpha = glowPulse), // Glowing Gold!
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        Modifier.border(
            width = (1.0 + 0.3 * glowPulse).dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFDF73).copy(alpha = 0.7f + 0.3f * glowPulse), // Shiny Gold Highlight
                    Color(0xFFD4AF37).copy(alpha = 0.7f + 0.3f * glowPulse), // Pure Metallic Gold
                    Color(0xFF8A640F).copy(alpha = 0.7f + 0.3f * glowPulse), // Deep Antique Gold
                    Color(0xFFFFDF73).copy(alpha = 0.7f + 0.3f * glowPulse)
                )
            ),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Card(
        modifier = modifier
            .height(100.dp)
            .graphicsLayer {
                if (!isOnline) {
                    translationY = floatOffset.dp.toPx()
                    scaleX = scalePulse
                    scaleY = scalePulse
                }
            }
            .shadow(
                elevation = if (isOnline) 16.dp else (6 * glowPulse).dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFFFFD700)
            )
            .then(borderModifier)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        val bgBrush = if (isOnline) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFF7AD), // Premium shiny gold highlight
                    Color(0xFFD4AF37), // Royal metallic gold
                    Color(0xFF996515), // Elegant deep gold/bronze
                    Color(0xFFD4AF37)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = 0.9f), // Dark luxury steel
                    Color(0xFF0F172A).copy(alpha = 0.98f)  // Sinking deep navy slate
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isOnline) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Phone Left (Elegant Dark Luxury Accent)
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "Phone Left",
                            tint = Color(0xFF1E1B4B).copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = -15f }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Rotating Earth / Globe (Contrast Navy)
                        Icon(
                            imageVector = icon,
                            contentDescription = "Rotating Earth",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer { rotationZ = rotationAngle }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Phone Right (Elegant Dark Luxury Accent)
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "Phone Right",
                            tint = Color(0xFF1E1B4B).copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = 15f }
                        )
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color(0xFFFFD700), // Gold icons for offline modes
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = iconSway
                            }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isOnline) Color(0xFF0F172A) else Color(0xFFFFD700), // Gold vs Dark
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isOnline) Color(0xFF1E293B).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PlayerTypeButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    tooltip: String
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color else Color(0x13FFFFFF))
            .clickable(onClick = onClick)
            .border(
                width = 1.5.dp,
                color = if (selected) Color.White else Color(0x22FFFFFF),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.4f), // Outlined color
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun RuleRow(num: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0x33FFD700), CircleShape)
                .border(1.dp, Color(0xFFFFD700), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(num, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFFFFD700))
        }
        Text(text, color = Color(0xFFE2E8F0), fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun CompactPlayerCountOption(
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFFFD700) else Color(0x13FFFFFF))
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${count} Players",
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = if (selected) Color(0xFF1E1B4B) else Color.White
        )
    }
}

@Composable
fun CompactColorOption(
    color: LudoColor,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color.value)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color == LudoColor.YELLOW) Color.Blue else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CompactThemeOption(
    theme: LudoTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val themeBgColor = when (theme) {
        LudoTheme.CLASSIC -> Color(0xFFD3A370)
        LudoTheme.COSMIC -> Color(0xFF1E1B4B)
        LudoTheme.ROYAL -> Color(0xFFFFD700)
        LudoTheme.FOREST -> Color(0xFF1B4332)
        LudoTheme.CANDY -> Color(0xFFEC4899)
        LudoTheme.OCEAN -> Color(0xFF06B6D4)
        LudoTheme.CYBERPUNK -> Color(0xFF6366F1)
        LudoTheme.EGYPT -> Color(0xFFD97706)
    }
    val themeTextColor = if (theme == LudoTheme.ROYAL) Color.Black else Color.White
    val emoji = when (theme) {
        LudoTheme.CLASSIC -> "🪵"
        LudoTheme.COSMIC -> "🌌"
        LudoTheme.ROYAL -> "👑"
        LudoTheme.FOREST -> "🌲"
        LudoTheme.CANDY -> "🍬"
        LudoTheme.OCEAN -> "🌊"
        LudoTheme.CYBERPUNK -> "⚡"
        LudoTheme.EGYPT -> "🏺"
    }

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(themeBgColor)
            .border(
                width = if (selected) 2.dp else 0.5.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 13.sp)
            Text(
                text = theme.displayName.split(" ").first(),
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                color = themeTextColor
            )
        }
    }
}

@Composable
fun CompactLanguageOption(
    language: LudoLanguage,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF4CAF50) else Color(0x33FFFFFF))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(language.flag, fontSize = 15.sp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = language.countryName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
                Text(
                    text = language.label,
                    fontWeight = FontWeight.Normal,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

fun isInternetAvailable(context: android.content.Context): Boolean {
    return try {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) {
        true
    }
}

