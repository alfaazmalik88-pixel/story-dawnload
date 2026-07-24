package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.model.AdType
import com.example.model.GamePhase
import com.example.model.LudoViewModel
import com.example.audio.LudoAudioEngine
import com.example.ui.LudoBoard
import com.example.ui.LudoMenu
import com.example.ui.LudoSplashScreen
import com.example.ui.CoinRedeemOverlay
import com.example.ui.LudoMatchmakingScreen
import androidx.compose.foundation.layout.Box
import com.example.ui.theme.MyApplicationTheme
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: LudoViewModel by viewModels()

  private var mInterstitialAd: InterstitialAd? = null
  private var mRewardedAd: RewardedAd? = null
  private var startAppAd: StartAppAd? = null

  private var isInterstitialLoading = false
  private var isRewardedLoading = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize user profile preferences and daily rewards
    viewModel.initPrefs(this)
    LudoAudioEngine.init(applicationContext)
    com.example.audio.RealtimeVoiceManager.init(applicationContext)

    // Initialize Start.io Ads SDK with App ID 206275910
    try {
      StartAppSDK.init(this, "206275910", false)
      StartAppSDK.enableReturnAds(false)
      StartAppAd.disableSplash()
      startAppAd = StartAppAd(this)
      Log.d("StartIO", "Start.io SDK initialized with App ID 206275910")
    } catch (e: Exception) {
      Log.e("StartIO", "Error initializing Start.io SDK: ${e.message}")
    }
    
    // Initialize Mobile Ads SDK
    MobileAds.initialize(this) {
      loadInterstitialAd()
      loadRewardedAd()
    }

    // Collect uiState changes to dynamically show loaded ads
    lifecycleScope.launch {
      viewModel.uiState.collectLatest { state ->
        val adType = state.adType
        if (adType != null) {
          // 1. Try displaying Start.io Ad
          val sAd = startAppAd
          if (sAd != null) {
            sAd.loadAd(object : AdEventListener {
              override fun onReceiveAd(ad: Ad) {
                viewModel.onRealAdStarted()
                runOnUiThread {
                  sAd.showAd(object : AdDisplayListener {
                    override fun adDisplayed(ad: Ad) {
                      Log.d("StartIO", "Start.io ad displayed.")
                    }
                    override fun adHidden(ad: Ad) {
                      Log.d("StartIO", "Start.io ad hidden/completed.")
                      viewModel.onRealAdCompleted(adType)
                    }
                    override fun adClicked(ad: Ad) {}
                    override fun adNotDisplayed(ad: Ad) {
                      Log.w("StartIO", "Start.io ad not displayed, falling back to AdMob")
                      showAdMobOrLocal(adType)
                    }
                  })
                }
              }

              override fun onFailedToReceiveAd(ad: Ad?) {
                Log.e("StartIO", "Start.io ad failed to load: ${ad?.errorMessage}")
                showAdMobOrLocal(adType)
              }
            })
          } else {
            showAdMobOrLocal(adType)
          }
        }
      }
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val state by viewModel.uiState.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
          if (state.gamePhase == GamePhase.SPLASH) {
            LudoSplashScreen(modifier = Modifier.fillMaxSize())
          } else {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
              when (state.gamePhase) {
                GamePhase.MODE_SELECT, GamePhase.SETUP -> {
                  LudoMenu(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                  )
                }
                GamePhase.PLAYING, GamePhase.FINISHED -> {
                  LudoBoard(
                    viewModel = viewModel,
                    onBack = { viewModel.triggerAd(AdType.GAME_FINISH) },
                    modifier = Modifier.padding(innerPadding)
                  )
                }
                else -> {}
              }
            }
          }

          if (state.coinRedeemAmount > 0) {
            CoinRedeemOverlay(
              amount = state.coinRedeemAmount,
              onDismiss = { viewModel.dismissCoinRedeemAnimation() }
            )
          }

          if (state.isFindingOpponent) {
            LudoMatchmakingScreen(
              state = state,
              viewModel = viewModel,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    LudoAudioEngine.startBgm(this)
  }

  override fun onStop() {
    super.onStop()
    LudoAudioEngine.stopBgm()
  }

  private fun showAdMobOrLocal(adType: AdType) {
    if (adType == AdType.GAME_FINISH || adType == AdType.RESET) {
      val ad = mInterstitialAd
      if (ad != null) {
        mInterstitialAd = null
        viewModel.onRealAdStarted()
        runOnUiThread {
          ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
              loadInterstitialAd()
              viewModel.onRealAdCompleted(adType)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
              mInterstitialAd = null
              Log.e("AdMob", "Interstitial ad failed to show: ${adError.message}")
            }
          }
          ad.show(this@MainActivity)
        }
      }
    } else if (adType == AdType.GUARANTEED_SIX || adType == AdType.EXTEND_TIME || adType == AdType.WATCH_AD) {
      val ad = mRewardedAd
      if (ad != null) {
        mRewardedAd = null
        viewModel.onRealAdStarted()
        runOnUiThread {
          var rewardEarned = false
          ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
              loadRewardedAd()
              if (rewardEarned) {
                viewModel.onRealAdCompleted(adType)
              } else {
                viewModel.dismissAd()
              }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
              mRewardedAd = null
              Log.e("AdMob", "Rewarded ad failed to show: ${adError.message}")
            }
          }
          ad.show(this@MainActivity) {
            rewardEarned = true
          }
        }
      }
    }
  }

  private fun loadInterstitialAd() {
    if (mInterstitialAd != null || isInterstitialLoading) return
    isInterstitialLoading = true
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(
      this,
      "ca-app-pub-3940256099942544/1033173712", // Google AdMob Test Interstitial ID
      adRequest,
      object : InterstitialAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
          mInterstitialAd = null
          isInterstitialLoading = false
          Log.e("AdMob", "Interstitial ad failed to load: ${adError.message}. Retrying in 15 seconds...")
          lifecycleScope.launch {
            kotlinx.coroutines.delay(15000)
            loadInterstitialAd()
          }
        }

        override fun onAdLoaded(interstitialAd: InterstitialAd) {
          mInterstitialAd = interstitialAd
          isInterstitialLoading = false
          Log.d("AdMob", "Interstitial ad loaded successfully.")
        }
      }
    )
  }

  private fun loadRewardedAd() {
    if (mRewardedAd != null || isRewardedLoading) return
    isRewardedLoading = true
    val adRequest = AdRequest.Builder().build()
    RewardedAd.load(
      this,
      "ca-app-pub-3940256099942544/5224354917", // Google AdMob Test Rewarded ID
      adRequest,
      object : RewardedAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
          mRewardedAd = null
          isRewardedLoading = false
          Log.e("AdMob", "Rewarded ad failed to load: ${adError.message}. Retrying in 15 seconds...")
          lifecycleScope.launch {
            kotlinx.coroutines.delay(15000)
            loadRewardedAd()
          }
        }

        override fun onAdLoaded(rewardedAd: RewardedAd) {
          mRewardedAd = rewardedAd
          isRewardedLoading = false
          Log.d("AdMob", "Rewarded ad loaded successfully.")
        }
      }
    )
  }
}

