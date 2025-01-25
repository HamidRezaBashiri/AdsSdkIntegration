package com.mobileaddemo.ads.prebid

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.AdSize
import com.mobileaddemo.ads.core.AdResult
import com.mobileaddemo.ads.core.AdSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.prebid.mobile.BannerAdUnit
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.ResultCode
import org.prebid.mobile.api.data.InitializationStatus
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class PrebidSDK private constructor(private val context: Context) : AdSDK {

    private var initialized = false
    private val adViewCache = ConcurrentHashMap<String, AdManagerAdView>()

    override val name: String = "Prebid"

    override suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.Main) {
        Log.d(TAG, "Initializing Prebid SDK...")
        if (initialized) {
            Log.d(TAG, "Prebid SDK already initialized.")
            return@withContext true
        }

        suspendCancellableCoroutine { continuation ->
            try {
                PrebidMobile.setPrebidServerAccountId("1481")
                PrebidMobile.setPrebidServerHost(
                    Host.createCustomHost("https://mp.4dex.io/pbs/openrtb2/auction")
                )
                PrebidMobile.setCustomStatusEndpoint("https://mp.4dex.io/healthcheck")
                PrebidMobile.setPbsDebug(true)
                PrebidMobile.setLogLevel(PrebidMobile.LogLevel.DEBUG)

                Log.d(TAG, "PrebidMobile configuration set. Initializing SDK...")

                MobileAds.initialize(context) {}
                PrebidMobile.checkGoogleMobileAdsCompatibility(
                    MobileAds.getVersion().toString()
                )

                PrebidMobile.initializeSdk(context) { status ->
                    Log.d(TAG, "Prebid SDK initialization status: $status")
                    if (status == InitializationStatus.SUCCEEDED) {
                        Log.d(TAG, "Prebid SDK initialized successfully!")
                        initialized = true
                        continuation.resume(true)
                    } else {
                        Log.e(TAG, "Prebid SDK initialization failed: ${status.description}")
                        continuation.resume(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during Prebid SDK initialization", e)
                continuation.resume(false)
            }
        }
    }

    override fun isInitialized(): Boolean = initialized

    override suspend fun loadAd(adUnitId: String): Flow<AdResult> = callbackFlow {
        if (!isInitialized()) {
            Log.e(TAG, "Prebid SDK is not initialized. Cannot load ad.")
            trySend(AdResult.Error(adUnitId, "Prebid SDK not initialized."))
            close()
            return@callbackFlow
        }

        val prebidAdUnit = BannerAdUnit(adUnitId, 320, 50)

        val adView = adViewCache.getOrPut(adUnitId) {
            AdManagerAdView(context).apply {
                setAdSizes(AdSize.BANNER)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "GAM onAdLoaded for $adUnitId")
                        trySend(AdResult.Success(adUnitId))
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "GAM onAdFailedToLoad for $adUnitId: ${error.message}")
                        trySend(AdResult.Error(adUnitId, error.message ?: "Unknown error"))
                        close()
                    }
                }
            }
        }

        val adRequest = AdManagerAdRequest.Builder().build()
        trySend(AdResult.Loading(adUnitId))

        prebidAdUnit.fetchDemand(adRequest) { resultCode ->
            Log.d(TAG, "Prebid fetchDemand($adUnitId) -> $resultCode")
            if (resultCode == ResultCode.SUCCESS) {
                Log.d(TAG, "Demand fetched successfully for $adUnitId. Loading GAM ad...")
                adView.loadAd(adRequest)
            } else {
                Log.e(TAG, "Failed to fetch demand for $adUnitId. ResultCode: $resultCode")
                trySend(AdResult.Error(adUnitId, "Prebid demand fetch failed."))
                close()
            }
        }

        awaitClose {
            cleanup(adUnitId)
        }
    }

    override fun createAdView(context: Context, adUnitId: String): View {
        TODO("Not yet implemented")
    }

    override fun cleanup(adUnitId: String) {
        adViewCache[adUnitId]?.destroy()
        adViewCache.remove(adUnitId)
    }

    override fun destroy() {
        adViewCache.values.forEach { it.destroy() }
        adViewCache.clear()
        initialized = false
    }

    override fun getOrCreateAdView(adUnitId: String): View {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "PrebidSDK"

        @Volatile
        private var instance: PrebidSDK? = null

        fun getInstance(context: Context): PrebidSDK =
            instance ?: synchronized(this) {
                instance ?: PrebidSDK(context.applicationContext).also { instance = it }
            }
    }
}
