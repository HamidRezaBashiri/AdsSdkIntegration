package com.mobileaddemo.ads.inmobi

import android.content.Context
import android.util.Log
import android.view.View
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiBanner
import com.inmobi.ads.listeners.BannerAdEventListener
import com.inmobi.sdk.InMobiSdk
import com.inmobi.sdk.SdkInitializationListener
import com.mobileaddemo.ads.core.AdResult
import com.mobileaddemo.ads.core.AdSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class InMobiAdSDK private constructor(private val context: Context) : AdSDK {
    private var initialized = false
    private val adViewCache = ConcurrentHashMap<String, InMobiBanner>()

    override val name: String = "InMobi"

    override suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.Main) {
        try {
            if (initialized) return@withContext true

            suspendCancellableCoroutine { continuation ->
                try {
                    val consentObject = JSONObject().apply {
                        put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
                        put("gdpr", "0") // 0 if GDPR not applicable, 1 if applicable
                        put(InMobiSdk.IM_GDPR_CONSENT_IAB, "")
                    }

                    InMobiSdk.init(
                        context,
                        "bfb3f72fbd8345978f7c37d0fa8f09a2", // Replace with your account ID
                        consentObject,
                        object : SdkInitializationListener {
                            override fun onInitializationComplete(error: Error?) {
                                if (error == null) {
                                    initialized = true
                                    Log.d(TAG, "InMobi SDK initialized successfully")
                                    continuation.resume(true)
                                } else {
                                    Log.e(TAG, "InMobi SDK initialization failed: ${error.message}")
                                    continuation.resume(false)
                                }
                            }
                        }
                    )
                    InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG)
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing InMobi SDK", e)
                    continuation.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in initialize", e)
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
            val banner = getOrCreateAdView(adUnitId) as InMobiBanner

            trySend(AdResult.Loading(adUnitId))

            banner.setListener(object : BannerAdEventListener() {
                override fun onAdLoadSucceeded(banner: InMobiBanner, adMetaInfo: AdMetaInfo) {
                    trySend(AdResult.Success(adUnitId))
                }

                override fun onAdLoadFailed(
                    banner: InMobiBanner,
                    status: InMobiAdRequestStatus
                ) {
                    status.message?.let { AdResult.Error(adUnitId, it) }?.let { trySend(it) }
                }

                override fun onAdClicked(banner: InMobiBanner, p1: MutableMap<Any, Any>?) {
                    Log.d(TAG, "Ad clicked")
                }

                override fun onAdDisplayed(banner: InMobiBanner) {
                    Log.d(TAG, "Ad displayed")
                }

                override fun onAdDismissed(banner: InMobiBanner) {
                    Log.d(TAG, "Ad dismissed")
                }
            })

            banner.load()
        }

        awaitClose {
            cleanup(adUnitId)
        }
    }

    override fun createAdView(context: Context, adUnitId: String): View {
        return InMobiBanner(context, adUnitId.toLong()).apply {
            setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF)
            setEnableAutoRefresh(false)
        }
    }

    override fun cleanup(adUnitId: String) {
        adViewCache[adUnitId]?.let { banner ->
            banner.destroy()
            adViewCache.remove(adUnitId)
        }
    }

    override fun destroy() {
        adViewCache.forEach { (_, banner) -> banner.destroy() }
        adViewCache.clear()
        initialized = false
    }

    override fun getOrCreateAdView(adUnitId: String): View {
        return adViewCache.getOrPut(adUnitId) {
            createAdView(context, adUnitId) as InMobiBanner
        }
    }

    companion object {
        private const val TAG = "InMobiAdSDK"

        @Volatile
        private var instance: InMobiAdSDK? = null

        fun getInstance(context: Context): InMobiAdSDK =
            instance ?: synchronized(this) {
                instance ?: InMobiAdSDK(context.applicationContext).also { instance = it }
            }
    }
} 