package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LudoLanguage
import com.example.model.LudoTranslations

data class LudoAvatar(
    val id: Int,
    val nameEn: String,
    val nameHi: String,
    val gradient: List<Color>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val frameColor: Color
)

val ludoAvatars = listOf(
    LudoAvatar(0, "Classic", "क्लासिक", listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6)), Icons.Default.Face, Color(0xFFFFD700)),
    LudoAvatar(1, "Pro Gamer", "प्रो गेमर", listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)), Icons.Default.SportsEsports, Color(0xFF00FFFF)),
    LudoAvatar(2, "King", "शाही", listOf(Color(0xFFFFD700), Color(0xFFF59E0B)), Icons.Default.WorkspacePremium, Color(0xFFFFD700)),
    LudoAvatar(3, "Strategy", "रणनीति", listOf(Color(0xFF10B981), Color(0xFF059669)), Icons.Default.Psychology, Color(0xFF00FF87)),
    LudoAvatar(4, "Commander", "सेनापति", listOf(Color(0xFFEF4444), Color(0xFFB91C1C)), Icons.Default.MilitaryTech, Color(0xFFE5E7EB)),
    LudoAvatar(5, "Cosmic", "कॉस्मिक", listOf(Color(0xFF8B5CF6), Color(0xFF4C1D95)), Icons.Default.Star, Color(0xFFA5B4FC)),
    LudoAvatar(6, "Artist", "कलाकार", listOf(Color(0xFF06B6D4), Color(0xFF0891B2)), Icons.Default.Brush, Color(0xFFF97316)),
    LudoAvatar(7, "Billionaire", "अरबपति", listOf(Color(0xFF1E293B), Color(0xFF0F172A)), Icons.Default.MonetizationOn, Color(0xFFFFD700))
)

@Composable
fun SettingsDialog(
    selectedLanguage: LudoLanguage,
    selectedMusicMode: String,
    onToggleMusicMode: () -> Unit,
    friendsList: List<String>,
    winsComputer: Int,
    winsOneVsOne: Int,
    winsOnline: Int,
    gameplaySpeed: String,
    onSetGameplaySpeed: (String) -> Unit,
    username: String,
    onUpdateUsername: (String) -> Unit,
    bio: String,
    onUpdateBio: (String) -> Unit,
    selectedAvatarId: Int,
    onUpdateAvatarId: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val email = "kamarpathan0786@gmail.com"

    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var editUsernameInput by remember { mutableStateOf(username) }

    var showEditBioDialog by remember { mutableStateOf(false) }
    var editBioInput by remember { mutableStateOf(bio) }

    val totalWins = winsComputer + winsOneVsOne + winsOnline
    val playerLevel = 1 + totalWins / 3
    val winsInCurrentLevel = totalWins % 3
    val progressToNextLevel = winsInCurrentLevel / 3.0f

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = if (selectedLanguage.code.contains("hi")) "👤 प्रोफाइल और सेटिंग्स" else "👤 Profile & Settings",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- PROFILE INFO & LEVEL PROGRESS ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E1B4B), // Deep royal indigo
                                    Color(0xFF0F0C20), // Luxurious near black
                                    Color(0xFF1B1030)  // Imperial purple
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700), // Pure Gold
                                    Color(0xFFC5A059), // Classic brass gold
                                    Color(0xFFFFEFA6), // Bright white-gold highlight
                                    Color(0xFFF59E0B), // Warm amber gold
                                    Color(0xFFFFD700)  // Pure Gold
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header section: Avatar on Left, Username & Level Badge on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Avatar on Left with luxurious Glowing Gold double ring
                            val currentAvatar = ludoAvatars.firstOrNull { it.id == selectedAvatarId } ?: ludoAvatars[0]
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF2E1065), Color(0xFF0F0C20))
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(2.5.dp, currentAvatar.frameColor, CircleShape)
                                    .padding(4.dp)
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFFFFD700), Color(0xFFFFEFA6), Color(0xFFF59E0B))
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.linearGradient(colors = currentAvatar.gradient),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = currentAvatar.icon,
                                        contentDescription = "Avatar Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            // Username & Level & Bio on Right
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Username
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            editUsernameInput = username
                                            showEditUsernameDialog = true
                                        }
                                ) {
                                    Text(
                                        text = "👑 $username",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 19.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Username",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Level Badge with Sparkling stars
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (selectedLanguage.code.contains("hi")) "✨ लेवल $playerLevel ✨" else "✨ LEVEL $playerLevel ✨",
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                // Bio / status slogan
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .clickable {
                                            editBioInput = bio
                                            showEditBioDialog = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = bio,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Bio",
                                            tint = Color(0xFFFFD700).copy(alpha = 0.5f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Premium Mini Stats Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedLanguage.code.contains("hi")) "🖥️ कंप्यूटर" else "🖥️ CPU WINS",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$winsComputer",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.12f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedLanguage.code.contains("hi")) "⚔️ 1v1 जीत" else "⚔️ 1v1 WINS",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$winsOneVsOne",
                                    color = Color(0xFFEF4444),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.12f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (selectedLanguage.code.contains("hi")) "🌐 ऑनलाइन" else "🌐 LIVE WINS",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$winsOnline",
                                    color = Color(0xFF10B981),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Premium Level Progress section with beautiful Golden Gradient progress bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (selectedLanguage.code.contains("hi")) "🏆 अगले लेवल की प्रगति" else "🏆 Progress to Next Level",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$winsInCurrentLevel / 3 wins",
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }

                            // Glowing custom gold-gradient Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(5.dp))
                            ) {
                                if (progressToNextLevel > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressToNextLevel)
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFFF59E0B), // Honey Gold
                                                        Color(0xFFFFD700), // Pure Gold
                                                        Color(0xFFFFFBEB)  // Glowing white-gold accent
                                                    )
                                                ),
                                                shape = RoundedCornerShape(5.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // --- PROFILE AVATAR / STICKER SELECTION ---
                SettingsSectionCard(
                    title = if (selectedLanguage.code.contains("hi")) "🎭 प्रोफाइल अवतार (Profile Avatars)" else "🎭 Profile Avatars",
                    icon = Icons.Default.Face,
                    iconTint = Color(0xFF00FFFF)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (selectedLanguage.code.contains("hi")) "अपना मनपसंद प्रीमियम अवतार चुनें (ये कार्टून नहीं हैं):" else "Select your favorite premium profile avatar (non-cartoon):",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        // 4 columns x 2 rows
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (row in 0..1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (col in 0..3) {
                                        val index = row * 4 + col
                                        if (index < ludoAvatars.size) {
                                            val avatar = ludoAvatars[index]
                                            val isSelected = selectedAvatarId == avatar.id
                                            
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) Color(0xFF312E81) else Color(0xFF0F172A).copy(alpha = 0.5f)
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) avatar.frameColor else Color.White.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        onUpdateAvatarId(avatar.id)
                                                    }
                                                    .padding(vertical = 8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
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
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (selectedLanguage.code.contains("hi")) avatar.nameHi else avatar.nameEn,
                                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                    fontSize = 8.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- MATCH STATISTICS ---
                SettingsSectionCard(
                    title = if (selectedLanguage.code.contains("hi")) "🏆 खेल सांख्यिकी (Stats)" else "🏆 Match Stats",
                    icon = Icons.Default.EmojiEvents,
                    iconTint = Color(0xFFFFD700)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatRow(
                            label = if (selectedLanguage.code.contains("hi")) "🖥️ बनाम कंप्यूटर (Vs Computer)" else "🖥️ Vs Computer",
                            wins = winsComputer,
                            color = Color(0xFF3B82F6)
                        )
                        StatRow(
                            label = if (selectedLanguage.code.contains("hi")) "⚔️ 1 बनाम 1 (1v1 Match)" else "⚔️ 1v1 Match",
                            wins = winsOneVsOne,
                            color = Color(0xFFEF4444)
                        )
                        StatRow(
                            label = if (selectedLanguage.code.contains("hi")) "🌐 ऑनलाइन गेम (Online Game)" else "🌐 Online Game",
                            wins = winsOnline,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // --- GAMEPLAY SPEED CONTROL ---
                SettingsSectionCard(
                    title = if (selectedLanguage.code.contains("hi")) "⚡ गेम प्ले स्पीड (Game Speed)" else "⚡ Gameplay Speed",
                    icon = Icons.Default.Speed,
                    iconTint = Color(0xFFFF9800)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (selectedLanguage.code.contains("hi")) "गोटी के चलने और बोट के निर्णय की गति बदलें:" else "Control token hopping and bot decision speeds:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("SLOW", "NORMAL", "FAST", "TURBO").forEach { speed ->
                                val isSelected = gameplaySpeed == speed
                                val speedLabel = when (speed) {
                                    "SLOW" -> if (selectedLanguage.code.contains("hi")) "धीमी" else "Slow"
                                    "FAST" -> if (selectedLanguage.code.contains("hi")) "तेज़" else "Fast"
                                    "TURBO" -> if (selectedLanguage.code.contains("hi")) "सुपर तेज़" else "Turbo"
                                    else -> if (selectedLanguage.code.contains("hi")) "सामान्य" else "Normal"
                                }
                                val activeColor = when (speed) {
                                    "SLOW" -> Color(0xFF3B82F6)
                                    "FAST" -> Color(0xFFF59E0B)
                                    "TURBO" -> Color(0xFFEF4444)
                                    else -> Color(0xFF10B981)
                                }

                                Button(
                                    onClick = { onSetGameplaySpeed(speed) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) activeColor else Color(0xFF1E293B),
                                        contentColor = if (isSelected) Color.White else Color(0xFF94A3B8)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) activeColor else Color(0xFF334155),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = speedLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // --- MUSIC & SOUNDS ---
                SettingsSectionCard(
                    title = if (selectedLanguage.code.contains("hi")) "🎵 बैकग्राउंड म्यूजिक" else "🎵 Background Music",
                    icon = Icons.Default.MusicNote,
                    iconTint = Color(0xFFE91E63)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedMusicMode == "GULF") "Current: Gulf Beats 🪘" else "Current: Classic Synth 🎹",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (selectedLanguage.code.contains("hi")) "संगीत बदलने के लिए बटन दबाएं" else "Tap button to toggle style",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = onToggleMusicMode,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = if (selectedLanguage.code.contains("hi")) "बदलें" else "Change",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // --- MY FRIENDS ---
                SettingsSectionCard(
                    title = if (selectedLanguage.code.contains("hi")) "🤝 मेरे दोस्त (My Friends)" else "🤝 My Friends",
                    icon = Icons.Default.People,
                    iconTint = Color(0xFFFFD700)
                ) {
                    if (friendsList.isEmpty()) {
                        Text(
                            text = if (selectedLanguage.code.contains("hi")) "अभी तक कोई दोस्त नहीं बना। दोस्त बनाने के लिए ऑनलाइन मैच खेलें!" else "No friends added yet. Play online matches to make friends!",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            friendsList.forEach { friendName ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = friendName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Privacy Policy Card
                SettingsSectionCard(
                    title = LudoTranslations.getTranslation("privacy_policy", selectedLanguage),
                    icon = Icons.Default.Security,
                    iconTint = Color(0xFF4CAF50)
                ) {
                    Text(
                        text = LudoTranslations.getTranslation("privacy_policy_desc", selectedLanguage),
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                // Contact Us Card
                SettingsSectionCard(
                    title = LudoTranslations.getTranslation("contact_us", selectedLanguage),
                    icon = Icons.Default.Email,
                    iconTint = Color(0xFF2196F3)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = LudoTranslations.getTranslation("contact_us_desc", selectedLanguage),
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = email,
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Ludo Contact Email", email)
                                    clipboard.setPrimaryClip(clip)
                                    val copiedText = LudoTranslations.getTranslation("email_copied", selectedLanguage)
                                    Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = LudoTranslations.getTranslation("copy_email", selectedLanguage),
                                    color = Color(0xFFFBC02D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // About Card
                SettingsSectionCard(
                    title = LudoTranslations.getTranslation("about", selectedLanguage),
                    icon = Icons.Default.Info,
                    iconTint = Color(0xFFE91E63)
                ) {
                    Text(
                        text = LudoTranslations.getTranslation("about_desc", selectedLanguage),
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = LudoTranslations.getTranslation("got_it", selectedLanguage).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBC02D)
                )
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(0.95f)
    )

    // Edit Username Dialog
    if (showEditUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = {
                Text(
                    text = if (selectedLanguage.code.contains("hi")) "नाम बदलें" else "Edit Username",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = editUsernameInput,
                    onValueChange = { editUsernameInput = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color(0xFF4F46E5)
                    ),
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editUsernameInput.trim().isNotEmpty()) {
                            onUpdateUsername(editUsernameInput.trim())
                            showEditUsernameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color(0xFF0F172A))
                ) {
                    Text(text = if (selectedLanguage.code.contains("hi")) "बचाएं" else "Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsernameDialog = false }) {
                    Text(text = if (selectedLanguage.code.contains("hi")) "रद्द करें" else "Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(14.dp)
        )
    }

    // Edit Bio Dialog
    if (showEditBioDialog) {
        AlertDialog(
            onDismissRequest = { showEditBioDialog = false },
            title = {
                Text(
                    text = if (selectedLanguage.code.contains("hi")) "बायो बदलें" else "Edit Bio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = editBioInput,
                    onValueChange = { editBioInput = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color(0xFF4F46E5)
                    ),
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateBio(editBioInput.trim())
                        showEditBioDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color(0xFF0F172A))
                ) {
                    Text(text = if (selectedLanguage.code.contains("hi")) "बचाएं" else "Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBioDialog = false }) {
                    Text(text = if (selectedLanguage.code.contains("hi")) "रद्द करें" else "Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun StatRow(
    label: String,
    wins: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$wins",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                )
            }
            content()
        }
    }
}
