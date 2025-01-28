// Updated PrebidSDK.kt
package com.mobileaddemo.ads.prebid

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.mobileaddemo.ads.core.AdSDK
import com.mobileaddemo.ads.prebid.PrebidAdViewManager.Companion.REACT_CLASS
import io.invertase.googlemobileads.common.SharedUtils.sendEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.prebid.mobile.BannerAdUnit
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.PrebidMobile.setLogLevel
import org.prebid.mobile.PrebidMobile.setPbsDebug
import org.prebid.mobile.PrebidMobile.setPrebidServerAccountId
import org.prebid.mobile.PrebidMobile.setPrebidServerHost
import org.prebid.mobile.api.data.InitializationStatus
import kotlin.coroutines.resume

class PrebidSDK private constructor(private val context: Context) : AdSDK {

    companion object {
        private const val TAG = "PrebidSDK"
        private val DEFAULT_AD_SIZE = AdSize(320, 50)

        @Volatile
        private var instance: PrebidSDK? = null

        fun getInstance(context: Context): PrebidSDK =
            instance ?: synchronized(this) {
                instance ?: PrebidSDK(context.applicationContext).also { instance = it }
            }
    }

    private var initialized = false
    override val name: String
        get() = "PrebidAdView"

    override suspend fun initialize(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        if (initialized) {
            continuation.resume(true) { Log.d(TAG, "Initialization cancelled") }
            return@suspendCancellableCoroutine
        }

        try {
                setPrebidServerAccountId("0689a263-318d-448b-a3d4-b02e8a709d9d")
                setPrebidServerHost(Host.createCustomHost("https://prebid-server-test-j.prebid.org/openrtb2/auction"))
                setLogLevel(org.prebid.mobile.PrebidMobile.LogLevel.DEBUG)
                setPbsDebug(true)

            Handler(Looper.getMainLooper()).post {
                PrebidMobile.initializeSdk(context) { status ->
                    when (status) {
                        InitializationStatus.SUCCEEDED -> {
                            initialized = true
                            Log.d(TAG, "SDK initialized")

                            continuation.resume(true) { Log.w(TAG, "Initialization cancelled") }
                        }
                        else -> {
                            Log.e(TAG, "Initialization failed: ${status.description}")
                            continuation.resume(false) { Log.w(TAG, "Initialization cancelled") }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Initialization error", e)
            continuation.resume(false) { Log.w(TAG, "Initialization cancelled") }
        }
    }

    override fun getOrCreateAdView(adUnitId: String, configId: String): AdManagerAdView {
        Log.d(TAG, "Created new AdView instance for $adUnitId")
        return createAdView(adUnitId,configId)
    }

    private fun createAdView(adUnitId: String, configId: String): AdManagerAdView {
        val adView = AdManagerAdView(context).apply {
            setAdUnitId(adUnitId)
            setAdSizes(DEFAULT_AD_SIZE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.gravity = Gravity.CENTER
            }
        }
        return adView
    }

    /**
     * Actually load the ad once you have a valid AdManagerAdView
     */
    suspend fun loadAd(adView: AdManagerAdView, configId: String) {
        try {
                val request = createAdRequest(configId)
                adView.loadAd(request)
            } catch (e: Exception) {
                Log.e(TAG, "Ad loading failed", e)
            }
    }

     private suspend fun createAdRequest(configId: String): AdManagerAdRequest = suspendCancellableCoroutine { continuation ->
        val adUnit = BannerAdUnit(configId, DEFAULT_AD_SIZE.width, DEFAULT_AD_SIZE.height)
         val builder = AdManagerAdRequest.Builder()

         adUnit.fetchDemand(builder.build()) { resultCode ->
             if (resultCode == org.prebid.mobile.ResultCode.SUCCESS) {
                 Log.d(TAG, "Demand fetched successfully")
             } else {
                 Log.e(TAG, "Demand fetch failed: $resultCode")
             }
             continuation.resume(builder.build()) { Log.w(TAG, "Ad request cancelled") }
        }
    }

    override fun isInitialized() = initialized

    // Remaining cleanup and destruction methods
    override fun cleanup(adUnitId: String) {
    }

    override fun destroy() {
        initialized = false
    }
}