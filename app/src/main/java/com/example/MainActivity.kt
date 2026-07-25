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

  private fun preloadStartIoAds() {
    if (!isNetworkConnected()) return
    Log.d("StartIO", "Preloading batch of 4-5 Start.io ads...")
    for (i in 1..5) {
      val ad = StartAppAd(this@MainActivity)
      val mode = if (i % 2 == 0) StartAppAd.AdMode.REWARDED_VIDEO else StartAppAd.AdMode.AUTOMATIC
      ad.loadAd(mode, object : AdEventListener {
        override fun onReceiveAd(a: Ad) {
          Log.d("StartIO", "Preloaded ad #$i successfully")
          synchronized(cachedStartIoAds) {
            cachedStartIoAds.add(ad)
          }
        }
        override fun onFailedToReceiveAd(a: Ad?) {}
      })
    }
  }

  private fun showStartIoAd(adType: AdType) {
    val isRewarded = (adType == AdType.GUARANTEED_SIX || adType == AdType.EXTEND_TIME || adType == AdType.WATCH_AD)
    
    // Check if we have a pre-cached ad ready
    var readyCachedAd: StartAppAd? = null
    synchronized(cachedStartIoAds) {
      val iterator = cachedStartIoAds.iterator()
      while (iterator.hasNext()) {
        val candidate = iterator.next()
        if (candidate.isReady) {
          readyCachedAd = candidate
          iterator.remove()
          break
        }
      }
    }

    if (readyCachedAd != null) {
      Log.d("StartIO", "Serving preloaded Start.io ad from fast cache!")
      viewModel.onRealAdStarted()
      readyCachedAd?.showAd(object : AdDisplayListener {
        override fun adDisplayed(ad: Ad) {}
        override fun adHidden(ad: Ad) {
          viewModel.onRealAdCompleted(adType)
          preloadStartIoAds() // Replenish cache immediately!
        }
        override fun adClicked(ad: Ad) {}
        override fun adNotDisplayed(ad: Ad) {
          viewModel.onRealAdCompleted(adType)
        }
      })
      return
    }

    if (!isNetworkConnected()) {
      if (isRewarded) {
        viewModel.onAdFailedOrOffline("❌ Internet Connection Required! Turn on mobile data or Wi-Fi to load video ads.")
      } else {
        viewModel.onRealAdCompleted(adType)
      }
      return
    }

    val sAd = StartAppAd(this@MainActivity)

    if (isRewarded) {
      var isVideoCompleted = false
      sAd.setVideoListener(object : VideoListener {
        override fun onVideoCompleted() {
          Log.d("StartIO", "Start.io Rewarded Video Completed successfully!")
          isVideoCompleted = true
        }
      })

      sAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
        override fun onReceiveAd(ad: Ad) {
          viewModel.onRealAdStarted()
          runOnUiThread {
            sAd.showAd(object : AdDisplayListener {
              override fun adDisplayed(ad: Ad) {
                Log.d("StartIO", "Start.io Rewarded Ad displayed")
              }
              override fun adHidden(ad: Ad) {
                Log.d("StartIO", "Start.io Rewarded Ad hidden/closed")
                viewModel.onRealAdCompleted(adType)
                preloadStartIoAds()
              }
              override fun adClicked(ad: Ad) {}
              override fun adNotDisplayed(ad: Ad) {
                Log.w("StartIO", "Start.io Rewarded Ad not displayed, falling back to in-app timer")
              }
            })
          }
        }

        override fun onFailedToReceiveAd(ad: Ad?) {
          Log.e("StartIO", "Start.io Rewarded Ad failed to receive: ${ad?.errorMessage}. Trying Automatic fallback.")
          val backupAd = StartAppAd(this@MainActivity)
          backupAd.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
            override fun onReceiveAd(a: Ad) {
              viewModel.onRealAdStarted()
              runOnUiThread {
                backupAd.showAd(object : AdDisplayListener {
                  override fun adDisplayed(a: Ad) {}
                  override fun adHidden(a: Ad) {
                    viewModel.onRealAdCompleted(adType)
                    preloadStartIoAds()
                  }
                  override fun adClicked(a: Ad) {}
                  override fun adNotDisplayed(a: Ad) {}
                })
              }
            }
            override fun onFailedToReceiveAd(a: Ad?) {
              Log.w("StartIO", "Backup Start.io ad also failed, using in-app video player timer")
            }
          })
        }
      })
    } else {
      // Interstitial / Gameplay finish / Reset
      sAd.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
        override fun onReceiveAd(ad: Ad) {
          viewModel.onRealAdStarted()
          runOnUiThread {
            sAd.showAd(object : AdDisplayListener {
              override fun adDisplayed(ad: Ad) {
                Log.d("StartIO", "Start.io Interstitial displayed")
              }
              override fun adHidden(ad: Ad) {
                Log.d("StartIO", "Start.io Interstitial hidden")
                viewModel.onRealAdCompleted(adType)
                preloadStartIoAds()
              }
              override fun adClicked(ad: Ad) {}
              override fun adNotDisplayed(ad: Ad) {
                Log.w("StartIO", "Start.io Interstitial not displayed")
                viewModel.onRealAdCompleted(adType)
              }
            })
          }
        }

        override fun onFailedToReceiveAd(ad: Ad?) {
          Log.e("StartIO", "Start.io Interstitial failed to receive: ${ad?.errorMessage}")
          viewModel.onRealAdCompleted(adType)
        }
      })
    }
  }
}


