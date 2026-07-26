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
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: LudoViewModel by viewModels()

  // Ad caching pool to instantly serve 4-5 preloaded Start.io ads
  private val cachedStartIoAds = java.util.Collections.synchronizedList(mutableListOf<StartAppAd>())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize user profile preferences and audio
    viewModel.initPrefs(this)
    LudoAudioEngine.init(applicationContext)
    com.example.audio.RealtimeVoiceManager.init(applicationContext)

    // Initialize Start.io Ads SDK with Original App ID 206275910
    try {
      StartAppSDK.init(this, "206275910", false)
      StartAppSDK.enableReturnAds(false)
      StartAppAd.disableSplash()
      Log.d("StartIO", "Start.io SDK initialized with App ID 206275910")
      preloadStartIoAds()
    } catch (e: Exception) {
      Log.e("StartIO", "Error initializing Start.io SDK: ${e.message}")
    }

    // Register Network Callback to automatically reload 4-5 ads as soon as internet turns ON
    try {
      val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
      cm?.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
          Log.d("StartIO", "📶 Network reconnected! Preloading 4-5 Start.io ads automatically.")
          runOnUiThread {
            preloadStartIoAds()
          }
        }
      })
    } catch (e: Exception) {
      Log.w("StartIO", "Could not register NetworkCallback: ${e.message}")
    }

    // Collect uiState changes to dynamically show Start.io ads
    lifecycleScope.launch {
      viewModel.uiState.collectLatest { state ->
        val adType = state.adType
        if (adType != null && !state.isRealAdShowing) {
          showStartIoAd(adType)
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
      preloadStartIoAds()
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

  private var cachedInterstitialAd: StartAppAd? = null
  private var cachedRewardedAd: StartAppAd? = null

  private fun preloadStartIoAds() {
    if (!isNetworkConnected()) return
    Log.d("StartIO", "Preloading Start.io Interstitial and Rewarded Ads...")
    
    // Preload Interstitial
    if (cachedInterstitialAd == null || cachedInterstitialAd?.isReady == false) {
      val interstitial = StartAppAd(this@MainActivity)
      interstitial.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
        override fun onReceiveAd(a: Ad) {
          Log.d("StartIO", "Preloaded Interstitial Ad successfully")
          cachedInterstitialAd = interstitial
        }
        override fun onFailedToReceiveAd(a: Ad?) {
          Log.w("StartIO", "Failed to preload Interstitial Ad: ${a?.errorMessage}")
        }
      })
    }

    // Preload Rewarded Video
    if (cachedRewardedAd == null || cachedRewardedAd?.isReady == false) {
      val rewarded = StartAppAd(this@MainActivity)
      rewarded.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
        override fun onReceiveAd(a: Ad) {
          Log.d("StartIO", "Preloaded Rewarded Video Ad successfully")
          cachedRewardedAd = rewarded
        }
        override fun onFailedToReceiveAd(a: Ad?) {
          Log.w("StartIO", "Failed to preload Rewarded Video Ad: ${a?.errorMessage}")
        }
      })
    }
  }

  private fun showStartIoAd(adType: AdType) {
    val isRewarded = (adType == AdType.GUARANTEED_SIX || adType == AdType.EXTEND_TIME || adType == AdType.WATCH_AD)

    if (!isNetworkConnected()) {
      if (isRewarded) {
        viewModel.onAdFailedOrOffline("❌ Internet Connection Required! Turn on mobile data or Wi-Fi to load video ads.")
      } else {
        viewModel.onRealAdCompleted(adType)
      }
      return
    }

    // Mark real ad as started immediately so ViewModel fallback timer doesn't cancel it prematurely
    viewModel.onRealAdStarted()

    if (isRewarded) {
      val readyAd = cachedRewardedAd
      if (readyAd != null && readyAd.isReady) {
        Log.d("StartIO", "Displaying preloaded Rewarded Video Ad!")
        cachedRewardedAd = null
        readyAd.showAd(object : AdDisplayListener {
          override fun adDisplayed(ad: Ad) {
            Log.d("StartIO", "Start.io Rewarded Ad displayed successfully")
          }
          override fun adHidden(ad: Ad) {
            Log.d("StartIO", "Start.io Rewarded Ad closed")
            viewModel.onRealAdCompleted(adType)
            preloadStartIoAds()
          }
          override fun adClicked(ad: Ad) {}
          override fun adNotDisplayed(ad: Ad) {
            Log.w("StartIO", "Start.io Rewarded Ad failed to display")
            viewModel.onRealAdCompleted(adType)
            preloadStartIoAds()
          }
        })
      } else {
        Log.d("StartIO", "Loading fresh Rewarded Video Ad on demand...")
        val freshAd = StartAppAd(this@MainActivity)
        freshAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
          override fun onReceiveAd(ad: Ad) {
            freshAd.showAd(object : AdDisplayListener {
              override fun adDisplayed(ad: Ad) {
                Log.d("StartIO", "Fresh Rewarded Ad displayed")
              }
              override fun adHidden(ad: Ad) {
                Log.d("StartIO", "Fresh Rewarded Ad closed")
                viewModel.onRealAdCompleted(adType)
                preloadStartIoAds()
              }
              override fun adClicked(ad: Ad) {}
              override fun adNotDisplayed(ad: Ad) {
                Log.w("StartIO", "Fresh Rewarded Ad not displayed")
                viewModel.onRealAdCompleted(adType)
                preloadStartIoAds()
              }
            })
          }

          override fun onFailedToReceiveAd(ad: Ad?) {
            Log.e("StartIO", "Failed to receive Rewarded Ad: ${ad?.errorMessage}. Trying Automatic fallback...")
            val backupAd = StartAppAd(this@MainActivity)
            backupAd.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
              override fun onReceiveAd(a: Ad) {
                backupAd.showAd(object : AdDisplayListener {
                  override fun adDisplayed(a: Ad) {}
                  override fun adHidden(a: Ad) {
                    viewModel.onRealAdCompleted(adType)
                    preloadStartIoAds()
                  }
                  override fun adClicked(a: Ad) {}
                  override fun adNotDisplayed(a: Ad) {
                    viewModel.onRealAdCompleted(adType)
                  }
                })
              }

              override fun onFailedToReceiveAd(a: Ad?) {
                Log.w("StartIO", "Backup Ad also failed to receive")
                viewModel.onRealAdCompleted(adType)
              }
            })
          }
        })
      }
    } else {
      // Interstitial / Fullscreen ad
      val readyAd = cachedInterstitialAd
      if (readyAd != null && readyAd.isReady) {
        Log.d("StartIO", "Displaying preloaded Interstitial Ad!")
        cachedInterstitialAd = null
        readyAd.showAd(object : AdDisplayListener {
          override fun adDisplayed(ad: Ad) {
            Log.d("StartIO", "Start.io Interstitial displayed")
          }
          override fun adHidden(ad: Ad) {
            Log.d("StartIO", "Start.io Interstitial closed")
            viewModel.onRealAdCompleted(adType)
            preloadStartIoAds()
          }
          override fun adClicked(ad: Ad) {}
          override fun adNotDisplayed(ad: Ad) {
            Log.w("StartIO", "Start.io Interstitial not displayed")
            viewModel.onRealAdCompleted(adType)
            preloadStartIoAds()
          }
        })
      } else {
        Log.d("StartIO", "Loading fresh Interstitial Ad on demand...")
        val freshAd = StartAppAd(this@MainActivity)
        freshAd.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
          override fun onReceiveAd(ad: Ad) {
            freshAd.showAd(object : AdDisplayListener {
              override fun adDisplayed(ad: Ad) {
                Log.d("StartIO", "Fresh Interstitial displayed")
              }
              override fun adHidden(ad: Ad) {
                Log.d("StartIO", "Fresh Interstitial closed")
                viewModel.onRealAdCompleted(adType)
                preloadStartIoAds()
              }
              override fun adClicked(ad: Ad) {}
              override fun adNotDisplayed(ad: Ad) {
                Log.w("StartIO", "Fresh Interstitial not displayed")
                viewModel.onRealAdCompleted(adType)
                preloadStartIoAds()
              }
            })
          }

          override fun onFailedToReceiveAd(ad: Ad?) {
            Log.e("StartIO", "Failed to receive Interstitial Ad: ${ad?.errorMessage}")
            viewModel.onRealAdCompleted(adType)
          }
        })
      }
    }
  }
}


