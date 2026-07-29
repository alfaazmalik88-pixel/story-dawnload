package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.audio.LudoAudioEngine
import com.example.model.AdType
import com.example.model.GamePhase
import com.example.model.LudoViewModel
import com.example.ui.CoinRedeemOverlay
import com.example.ui.LudoBoard
import com.example.ui.LudoMatchmakingScreen
import com.example.ui.LudoMenu
import com.example.ui.LudoSplashScreen
import com.example.ui.theme.MyApplicationTheme
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

  // AdMob Test Ads
  private var adMobInterstitialAd: InterstitialAd? = null
  private var adMobRewardedAd: RewardedAd? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize user profile preferences and audio
    viewModel.initPrefs(this)
    LudoAudioEngine.init(applicationContext)
    com.example.audio.RealtimeVoiceManager.init(applicationContext)

    // Initialize Google AdMob SDK for Test Ads
    try {
      MobileAds.initialize(this) { initializationStatus ->
        Log.d("AdMob", "Google AdMob SDK Initialized successfully: $initializationStatus")
        preloadAdMobTestAds()
      }
    } catch (e: Exception) {
      Log.e("AdMob", "Error initializing Google AdMob SDK: ${e.message}")
    }

    // Register Network Callback to automatically reload ads when internet reconnects
    try {
      val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
      cm?.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
          Log.d("AdEngine", "📶 Network reconnected! Preloading AdMob ads.")
          runOnUiThread {
            preloadAdMobTestAds()
          }
        }
      })
    } catch (e: Exception) {
      Log.w("AdEngine", "Could not register NetworkCallback: ${e.message}")
    }

    // Collect uiState changes to dynamically show AdMob ads
    lifecycleScope.launch {
      viewModel.uiState.collectLatest { state ->
        val adType = state.adType
        if (adType != null && !state.isRealAdShowing) {
          showAdMobAd(adType)
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
    if (isNetworkConnected()) {
      preloadAdMobTestAds()
    }
  }

  override fun onStop() {
    super.onStop()
    LudoAudioEngine.stopBgm()
  }

  private fun isNetworkConnected(): Boolean {
    val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
    if (connectivityManager != null) {
      val network = connectivityManager.activeNetwork ?: return false
      val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
      return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    return false
  }

  private fun preloadAdMobTestAds() {
    if (!isNetworkConnected()) return
    Log.d("AdMob", "Preloading AdMob Test Interstitial & Rewarded Ads...")

    // AdMob Test Interstitial
    if (adMobInterstitialAd == null) {
      val adRequest = AdRequest.Builder().build()
      InterstitialAd.load(
        this,
        "ca-app-pub-3940256099942544/1033173712", // Google Official Test Interstitial Unit ID
        adRequest,
        object : InterstitialAdLoadCallback() {
          override fun onAdLoaded(ad: InterstitialAd) {
            Log.d("AdMob", "AdMob Test Interstitial Ad Loaded Successfully!")
            adMobInterstitialAd = ad
          }

          override fun onAdFailedToLoad(error: LoadAdError) {
            Log.w("AdMob", "Failed to load AdMob Test Interstitial: ${error.message}")
            adMobInterstitialAd = null
          }
        }
      )
    }

    // AdMob Test Rewarded
    if (adMobRewardedAd == null) {
      val adRequest = AdRequest.Builder().build()
      RewardedAd.load(
        this,
        "ca-app-pub-3940256099942544/5224354917", // Google Official Test Rewarded Unit ID
        adRequest,
        object : RewardedAdLoadCallback() {
          override fun onAdLoaded(ad: RewardedAd) {
            Log.d("AdMob", "AdMob Test Rewarded Ad Loaded Successfully!")
            adMobRewardedAd = ad
          }

          override fun onAdFailedToLoad(error: LoadAdError) {
            Log.w("AdMob", "Failed to load AdMob Test Rewarded: ${error.message}")
            adMobRewardedAd = null
          }
        }
      )
    }
  }

  private fun showAdMobAd(adType: AdType) {
    val isRewarded = (adType == AdType.GUARANTEED_SIX || adType == AdType.EXTEND_TIME || adType == AdType.WATCH_AD)

    if (!isNetworkConnected()) {
      if (isRewarded) {
        viewModel.onAdFailedOrOffline("❌ Internet Connection Required! Turn on mobile data or Wi-Fi to load test ads.")
      } else {
        viewModel.onRealAdCompleted(adType)
      }
      return
    }

    viewModel.onRealAdStarted()

    if (isRewarded) {
      val readyAdMob = adMobRewardedAd
      if (readyAdMob != null) {
        Log.d("AdMob", "Displaying AdMob Test Rewarded Ad!")
        adMobRewardedAd = null
        readyAdMob.fullScreenContentCallback = object : FullScreenContentCallback() {
          override fun onAdDismissedFullScreenContent() {
            Log.d("AdMob", "AdMob Test Rewarded Ad closed")
            viewModel.onRealAdCompleted(adType)
            preloadAdMobTestAds()
          }
          override fun onAdFailedToShowFullScreenContent(error: AdError) {
            Log.w("AdMob", "AdMob Test Rewarded Ad failed to show: ${error.message}")
            viewModel.onRealAdCompleted(adType)
            preloadAdMobTestAds()
          }
        }
        readyAdMob.show(this) { rewardItem ->
          Log.d("AdMob", "User awarded: ${rewardItem.amount} ${rewardItem.type}")
        }
      } else {
        Log.d("AdMob", "AdMob Rewarded ad not ready yet, loading on demand...")
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
          this,
          "ca-app-pub-3940256099942544/5224354917",
          adRequest,
          object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
              ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                  viewModel.onRealAdCompleted(adType)
                  preloadAdMobTestAds()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                  viewModel.onRealAdCompleted(adType)
                  preloadAdMobTestAds()
                }
              }
              ad.show(this@MainActivity) { rewardItem ->
                Log.d("AdMob", "Rewarded item: ${rewardItem.amount}")
              }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
              Log.e("AdMob", "Failed to load on-demand rewarded ad: ${error.message}")
              viewModel.onRealAdCompleted(adType)
            }
          }
        )
      }
    } else {
      val readyAdMob = adMobInterstitialAd
      if (readyAdMob != null) {
        Log.d("AdMob", "Displaying AdMob Test Interstitial Ad!")
        adMobInterstitialAd = null
        readyAdMob.fullScreenContentCallback = object : FullScreenContentCallback() {
          override fun onAdDismissedFullScreenContent() {
            Log.d("AdMob", "AdMob Test Interstitial Ad closed")
            viewModel.onRealAdCompleted(adType)
            preloadAdMobTestAds()
          }
          override fun onAdFailedToShowFullScreenContent(error: AdError) {
            Log.w("AdMob", "AdMob Test Interstitial Ad failed to show: ${error.message}")
            viewModel.onRealAdCompleted(adType)
            preloadAdMobTestAds()
          }
        }
        readyAdMob.show(this)
      } else {
        Log.d("AdMob", "AdMob Interstitial ad not ready yet, loading on demand...")
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
          this,
          "ca-app-pub-3940256099942544/1033173712",
          adRequest,
          object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
              ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                  viewModel.onRealAdCompleted(adType)
                  preloadAdMobTestAds()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                  viewModel.onRealAdCompleted(adType)
                  preloadAdMobTestAds()
                }
              }
              ad.show(this@MainActivity)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
              Log.e("AdMob", "Failed to load on-demand interstitial ad: ${error.message}")
              viewModel.onRealAdCompleted(adType)
            }
          }
        )
      }
    }
  }
}
