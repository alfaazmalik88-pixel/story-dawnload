package com.example.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay

interface AdItem {
    val icon: String
    val title: String
    val subtitle: String
    val actionText: String
    val themeColor: Color
    val targetUrl: String
}

data class BannerAdData(
    override val icon: String,
    override val title: String,
    override val subtitle: String,
    override val actionText: String,
    override val themeColor: Color,
    override val targetUrl: String
) : AdItem

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    var showBackupAds by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp) // Total height increased to accommodate 12dp bottom padding beautifully
            .background(Color(0xFF0F172A))
            .padding(bottom = 12.dp), // Lift the banner ad up slightly as requested by the user ("halkasa uper kardo")
        contentAlignment = Alignment.Center
    ) {
        if (!showBackupAds) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = "ca-app-pub-6221583924605189/7574074542" // User's real AdMob ID
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("AdMob", "Banner ad loaded successfully.")
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.e("AdMob", "Banner ad failed to load: ${error.code} - ${error.message}. Switching to backup animated ads.")
                                showBackupAds = true
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                update = { adView ->
                    // AdView handles updates internally
                }
            )
        } else {
            // Render the backup simulated rotating ads beautifully
            BackupSimulatedBannerAd()
        }
    }
}

@Composable
fun BackupSimulatedBannerAd() {
    val ads = remember {
        listOf(
            BannerAdData(
                icon = "🎲",
                title = "Ludo Club Online",
                subtitle = "Challenge 10M+ active players online! 🌐",
                actionText = "PLAY",
                themeColor = Color(0xFF10B981), // Emerald Green
                targetUrl = "https://ai.studio/build"
            ),
            BannerAdData(
                icon = "✨",
                title = "Google AI Studio",
                subtitle = "Build Kotlin apps in seconds using Gemini 3.5",
                actionText = "TRY NOW",
                themeColor = Color(0xFF3B82F6), // Blue
                targetUrl = "https://ai.studio/build"
            ),
            BannerAdData(
                icon = "🪙",
                title = "Ludo Master Pro",
                subtitle = "Collect 10,000 free coins today! 🤑",
                actionText = "CLAIM",
                themeColor = Color(0xFFF59E0B), // Amber
                targetUrl = "https://ai.studio/build"
            ),
            BannerAdData(
                icon = "🚀",
                title = "Cosmic Theme Pack",
                subtitle = "Unlock limited edition neon spaceship gotis! 🌌",
                actionText = "GET",
                themeColor = Color(0xFF8B5CF6), // Purple
                targetUrl = "https://ai.studio/build"
            )
        )
    }

    var currentAdIdx by remember { mutableStateOf(0) }

    // Automatically rotate ads every 5 seconds with animation
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            currentAdIdx = (currentAdIdx + 1) % ads.size
        }
    }

    val currentAd = ads[currentAdIdx]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(4.dp, RoundedCornerShape(0.dp)),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)) // Slate dark banner container
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B).copy(alpha = 0.9f)
                        )
                    )
                )
                .clickable { /* Simulate ad click */ },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Part: AD Tag, Icon, Title and Description
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Small AD badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF334155))
                            .border(0.5.dp, Color(0xFF94A3B8), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Animated Icon
                    AnimatedContent(
                        targetState = currentAd.icon,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "adIcon"
                    ) { iconStr ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(currentAd.themeColor.copy(alpha = 0.15f))
                                .border(1.dp, currentAd.themeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = iconStr, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Ad text details (Title & Subtitle)
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        AnimatedContent(
                            targetState = currentAd.title,
                            transitionSpec = {
                                slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                            },
                            label = "adTitle"
                        ) { titleText ->
                            Text(
                                text = titleText,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        AnimatedContent(
                            targetState = currentAd.subtitle,
                            transitionSpec = {
                                slideInVertically { it / 2 } + fadeIn() togetherWith slideOutVertically { -it / 2 } + fadeOut()
                            },
                            label = "adSubtitle"
                        ) { subtitleText ->
                            Text(
                                text = subtitleText,
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Part: Install / Play Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(currentAd.themeColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = currentAd.actionText,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "adAction"
                    ) { actionStr ->
                        Text(
                            text = actionStr,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
