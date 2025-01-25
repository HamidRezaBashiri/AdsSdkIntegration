package com.mobileaddemo.ads.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton class to manage advertisement SDKs.
 */
class AdManager private constructor() {
    // Map to hold the SDK instances for each AdProvider
    private val sdkMap = ConcurrentHashMap<AdProvider, AdSDK>()
    
    // MutableStateFlow to hold the state of each AdProvider
    private val _adState = MutableStateFlow<Map<AdProvider, AdState>>(emptyMap())
    
    // Publicly exposed StateFlow to observe the state of each AdProvider
    val adState: StateFlow<Map<AdProvider, AdState>> = _adState.asStateFlow()
    
    /**
     * Sealed class representing the state of an AdProvider.
     */
    sealed class AdState {
        object Idle : AdState()
        object Initializing : AdState()
        object Ready : AdState()
        data class Error(val error: String) : AdState()
    }

    /**
     * Initializes all the SDKs in the sdkMap.
     * @param context The application context.
     * @return Boolean indicating if all SDKs were successfully initialized.
     */
    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        var allInitialized = true
        sdkMap.forEach { (provider, sdk) ->
            try {
                updateState(provider, AdState.Initializing)
                val isInitialized = sdk.initialize(context)
                if (isInitialized) {
                    updateState(provider, AdState.Ready)
                } else {
                    updateState(provider, AdState.Error("Initialization failed"))
                    allInitialized = false
                }
            } catch (e: Exception) {
                updateState(provider, AdState.Error(e.message ?: "Unknown error"))
                allInitialized = false
            }
        }
        allInitialized
    }

    /**
     * Updates the state of a specific AdProvider.
     * @param provider The AdProvider whose state is to be updated.
     * @param state The new state of the AdProvider.
     */
    private fun updateState(provider: AdProvider, state: AdState) {
        val currentMap = _adState.value.toMutableMap()
        currentMap[provider] = state
        _adState.value = currentMap
    }

    fun registerSDK(provider: AdProvider, sdk: AdSDK) {
        sdkMap[provider] = sdk
        updateState(provider, AdState.Idle)
    }

    fun getSDK(provider: AdProvider): AdSDK? = sdkMap[provider]
    
    fun isSDKInitialized(provider: AdProvider): Boolean =
        _adState.value[provider] == AdState.Ready

    fun cleanup() {
        sdkMap.forEach { (_, sdk) ->
            sdk.destroy()
        }
        sdkMap.clear()
        _adState.value = emptyMap()
    }

    companion object {
        @Volatile
        private var instance: AdManager? = null
        
        fun getInstance(): AdManager =
            instance ?: synchronized(this) {
                instance ?: AdManager().also { instance = it }
            }
    }
}