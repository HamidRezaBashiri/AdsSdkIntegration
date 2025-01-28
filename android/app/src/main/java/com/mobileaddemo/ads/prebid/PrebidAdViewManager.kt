package com.mobileaddemo.ads.prebid

import android.util.Log
import android.view.ViewGroup
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.EventDispatcher
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdView
import kotlinx.coroutines.*
import com.facebook.react.uimanager.events.RCTEventEmitter
import io.invertase.googlemobileads.OnNativeEvent

/**
 * This manager replicates how Google does it:
 * - We create a custom PrebidAdView in createViewInstance
 * - We set props (configId, adUnitId)
 * - In onAfterUpdateTransaction, if propsChanged is true, we do loadAd
 * - loadAd destroys any old child AdView, creates a new one, sets listener, calls prebidSDK.loadAd(...)
 */
class PrebidAdViewManager(private val reactContext: ReactApplicationContext) :
    SimpleViewManager<PrebidAdView>() {

    companion object {
        const val REACT_CLASS = "PrebidAdView"
        private const val COMMAND_LOAD = 1
        private const val TAG = "PrebidAdViewManager"
    }

    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun getName(): String {
        return REACT_CLASS
    }

    override fun createViewInstance(context: ThemedReactContext): PrebidAdView {
        // Return our custom FrameLayout-based container
        return PrebidAdView(context)
    }

    override fun getCommandsMap(): Map<String, Int> {
        // For manual loading from JS, e.g. UIManager.dispatchViewManagerCommand
        return mapOf("load" to COMMAND_LOAD)
    }

    override fun receiveCommand(root: PrebidAdView, commandId: String?, args: ReadableArray?) {
        when (commandId?.toInt()) {
            COMMAND_LOAD -> {
                mainScope.launch {
                    loadAd(root)
                }
            }
        }
    }

    @ReactProp(name = "adUnitId")
    fun setAdUnitId(view: PrebidAdView, adUnitId: String?) {
        view.adUnitId = adUnitId
        view.propsChanged = true
    }

    @ReactProp(name = "configId")
    fun setConfigId(view: PrebidAdView, configId: String?) {
        view.configId = configId
        view.propsChanged = true
    }

    // If you want to replicate fluid approach, you can have a prop for it:
    // e.g. @ReactProp(name = "fluid") fun setFluid(view: PrebidAdView, fluid: Boolean) { ... }
    // Then you do view.isFluid = fluid

    /**
     * After RN sets all props, this is called once. If we detect a prop change, let's do loadAd.
     */
    override fun onAfterUpdateTransaction(view: PrebidAdView) {
        super.onAfterUpdateTransaction(view)
        if (view.propsChanged) {
            view.propsChanged = false
            mainScope.launch {
                loadAd(view)
            }
        }
    }

    /**
     * Main function to load or reload the ad:
     * 1. Destroy any existing AdManagerAdView child
     * 2. Create a new AdView
     * 3. Set up listener
     * 4. Pass it to prebidSDK.loadAd(...)
     */
    private suspend fun loadAd(container: PrebidAdView) {
        val adUnitId = container.adUnitId
        val configId = container.configId

        if (adUnitId.isNullOrEmpty() || configId.isNullOrEmpty()) {
            sendEvent(container, "error", mapOf("code" to 0, "message" to "Missing adUnitId or configId"))
            return
        }

        // Destroy and remove old child if present
        val oldChild = container.getChildAt(0)
        if (oldChild is AdManagerAdView) {
//            oldChild.adListener = null
            oldChild.destroy()
        }
        container.removeAllViews()

        val prebidSDK = PrebidSDK.getInstance(container.context)

        val isReady = waitForSdkInitialization(prebidSDK, maxRetries = 3, delayMs = 2000)
        if (!isReady){
            // If still not initialized, send an error event
            sendEvent(container, "error", mapOf(
                "code" to 0,
                "message" to "Prebid SDK not initialized after 3 attempts"
            ))
            return
        }

        // Create brand-new AdView
        val adView = prebidSDK.getOrCreateAdView(adUnitId, configId)

        // Attach final AdListener
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "AdListener onAdLoaded: $adUnitId")
                sendEvent(container, "loaded", mapOf("width" to 320, "height" to 50))
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "AdListener onAdFailedToLoad: ${error.message}")
                sendEvent(container, "error", mapOf("code" to error.code, "message" to error.message))
            }
        }

        // Add new child
        container.addView(adView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        // Finally, call the Prebid load logic
        prebidSDK.loadAd(adView, configId)
    }

    /**
     * When React Native unmounts the view, we destroy the AdView.
     */
    override fun onDropViewInstance(view: PrebidAdView) {
        val child = view.getChildAt(0)
        if (child is AdManagerAdView) {
//            child.adListener = null
            child.destroy()
        }
        view.removeAllViews()
        super.onDropViewInstance(view)
    }

    /**
     * Helper to send an event to JS with type & payload.
     * We'll fire "onNativeEvent" in JS, then parse the event in the PrebidAdView.tsx
     */
    private fun sendEvent(container: PrebidAdView, eventType: String, payload: Map<String, Any?>) {
        val event = Arguments.createMap().apply {
            putString("type", eventType)
            payload.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Double -> putDouble(key, value)
                    is Int -> putInt(key, value)
                    else -> putString(key, value?.toString())
                }
            }
        }
        val context = container.context
        if (context is ThemedReactContext) {
            val dispatcher: EventDispatcher? = UIManagerHelper.getEventDispatcherForReactTag(context, container.id)
            dispatcher?.dispatchEvent(OnNativeEvent(container.id, event))
        }
    }
    private suspend fun waitForSdkInitialization(
        prebidSDK: PrebidSDK,
        maxRetries: Int = 3,
        delayMs: Long = 2000
    ): Boolean {
        var attempt = 0
        while (!prebidSDK.isInitialized() && attempt < maxRetries) {
            Log.w(TAG, "Prebid SDK not initialized - waiting $delayMs ms (attempt ${attempt + 1}/$maxRetries)")
            delay(delayMs) // Suspend for delayMs
            attempt++
        }
        return prebidSDK.isInitialized()
    }

}
