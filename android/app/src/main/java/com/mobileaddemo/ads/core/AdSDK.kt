package com.mobileaddemo.ads.core

import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the contract for ad network SDK implementations
 */
interface AdSDK {
    /**
     * Unique identifier for the ad network
     */
    val name: String

    /**
     * Initializes the ad network SDK
     * @param context Application context required for SDK initialization
     * @return true if initialization was successful, false otherwise
     */
    suspend fun initialize(context: Context): Boolean

    /**
     * Checks if the SDK has been properly initialized
     * @return true if SDK is initialized and ready for use
     */
    fun isInitialized(): Boolean

    /**
     * Loads an advertisement from the network
     * @param adUnitId The unique identifier for the ad placement
     * @return Flow emitting [AdResult] states during the ad loading process
     */
    suspend fun loadAd(adUnitId: String): Flow<AdResult>

    /**
     * Creates an ad view for the given ad unit
     * @param context Application context
     * @param adUnitId The unique identifier for the ad placement
     * @return Ad view
     */
    fun createAdView(context: Context, adUnitId: String): View

    /**
     * Cleans up resources for the given ad unit
     * @param adUnitId The unique identifier for the ad placement
     */
    fun cleanup(adUnitId: String)

    /**
     * Destroys the SDK
     */
    fun destroy()

    /**
     * Gets or creates an ad view for the given ad unit
     * @param adUnitId The unique identifier for the ad placement
     * @return Ad view
     */
    fun getOrCreateAdView(adUnitId: String): View
}

/**
 * Sealed class representing possible states during ad loading
 */
sealed class AdResult {
    /**
     * Ad loaded successfully
     * @property adUnitId The identifier of the loaded ad
     */
    data class Success(val adUnitId: String) : AdResult()

    /**
     * Ad loading failed
     * @property adUnitId The identifier of the failed ad
     * @property message Error description
     */
    data class Error(val adUnitId: String, val message: String) : AdResult()

    /**
     * Ad is currently loading
     * @property adUnitId The identifier of the loading ad
     */
    data class Loading(val adUnitId: String) : AdResult()
}


sealed class AdError : Exception() {
    data class InitializationError(override val message: String) : AdError()
    data class LoadError(override val message: String) : AdError()
    data class NotInitializedError(override val message: String = "SDK not initialized") : AdError()
}
