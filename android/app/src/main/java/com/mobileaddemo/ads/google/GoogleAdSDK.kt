package com.mobileaddemo.ads.google

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.ads.mediation.inmobi.InMobiAdapter
import com.google.ads.mediation.inmobi.InMobiNetworkKeys
import com.google.ads.mediation.inmobi.InMobiNetworkValues
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.mobileaddemo.ads.core.AdError
import com.mobileaddemo.ads.core.AdResult
import com.mobileaddemo.ads.core.AdSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.util.concurrent.ConcurrentHashMap
import java.util.Collections
//this file is not used in current usecase 
class GoogleAdSDK private constructor(private val context: Context) : AdSDK {
    private var initialized = false
    private val viewRegistry = mutableMapOf<String, String>() // viewKey -> adUnitId
    private val adViewCache = ConcurrentHashMap<String, AdView>()
    private val loadedAds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    override val name: String = "Google Ads"

    /**
     * Initializes the Google Mobile Ads SDK.
     * @param context The application context.
     * @return Boolean indicating if the SDK was successfully initialized.
     */
    override suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // If already initialized, return true
            if (initialized) return@withContext true

            // Suspend the coroutine until the initialization is complete
            suspendCancellableCoroutine { continuation ->
                try {
                    // Enable verbose logging for Google Mobile Ads SDK
                    MobileAds.setRequestConfiguration(
                        MobileAds.getRequestConfiguration()
                            .toBuilder()
                            .setTestDeviceIds(listOf("EMULATOR"))
                            .build()
                    )
                    // Initialize the Google Mobile Ads SDK
                    MobileAds.initialize(context) { initializationStatus ->
                        // Mark as initialized and resume the coroutine
                        initialized = true
                        continuation.resume(true)
                    }
                } catch (e: Exception) {
                    // If initialization fails, resume the coroutine with false
                    continuation.resume(false)
                }
            }
        } catch (e: Exception) {
            // If an exception occurs, log it and return false
            Log.e("GoogleAdSDK", "Initialization failed", e)
            false
        }
    }

    override fun isInitialized(): Boolean = initialized

    override suspend fun loadAd(adUnitId: String): Flow<AdResult> = callbackFlow {
        if (!isInitialized()) {
            trySend(AdResult.Error(adUnitId, "SDK not initialized"))
            close()
            return@callbackFlow
        }

        withContext(Dispatchers.Main) {
            val adView = getOrCreateAdView(adUnitId)

            trySend(AdResult.Loading(adUnitId))

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loadedAds.add(adUnitId)
                    trySend(AdResult.Success(adUnitId))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadedAds.remove(adUnitId)
                    trySend(AdResult.Error(adUnitId, error.message))
                }
            }

            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(InMobiAdapter::class.java, createMediationExtras())
                .build()

            adView.loadAd(adRequest)
        }

        awaitClose {
            cleanup(adUnitId)
        }
    }

    fun isAdLoaded(adUnitId: String): Boolean {
        return loadedAds.contains(adUnitId)
    }

    override fun createAdView(context: Context, adUnitId: String): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            this.adUnitId = adUnitId
        }
    }

    override fun cleanup(adUnitId: String) {
        adViewCache[adUnitId]?.let { adView ->
            adView.destroy()
            adViewCache.remove(adUnitId)
            loadedAds.remove(adUnitId)
        }
    }

    override fun destroy() {
        adViewCache.forEach { (_, adView) -> adView.destroy() }
        adViewCache.clear()
        initialized = false
    }

    override fun getOrCreateAdView(adUnitId: String): AdView {
        return adViewCache.getOrPut(adUnitId) {
            createAdView(context, adUnitId)
        }
    }

    fun registerAdView(viewKey: String, adUnitId: String) {
        viewRegistry[viewKey] = adUnitId
    }

    fun unregisterAdView(viewKey: String) {
        viewRegistry.remove(viewKey)
    }

    fun getAdViewForKey(viewKey: String): AdView? {
        val adUnitId = viewRegistry[viewKey] ?: return null
        return getOrCreateAdView(adUnitId)
    }

    private fun createMediationExtras(): Bundle {
        return Bundle().apply {
            // InMobi targeting parameters
            putString(InMobiNetworkKeys.AGE_GROUP, InMobiNetworkValues.BETWEEN_18_AND_24)
            putString(InMobiNetworkKeys.INTERESTS, "sports,music,technology")
            putString(InMobiNetworkKeys.LANGUAGE, "en-US")
            // Add any other targeting parameters you need
        }
    }

    companion object {
        private const val TAG = "GoogleAdSDK"
        
        @Volatile
        private var instance: GoogleAdSDK? = null

        fun getInstance(context: Context): GoogleAdSDK =
            instance ?: synchronized(this) {
                instance ?: GoogleAdSDK(context.applicationContext).also { instance = it }
            }
    }
}