package com.mobileaddemo.ads.google

import android.content.res.Resources
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class GoogleAdViewManager : SimpleViewManager<GoogleAdViewManager.AdContainer>() {

    override fun getName(): String = "GoogleAdView"

    inner class AdContainer(context: ThemedReactContext) : FrameLayout(context) {
        var adView: AdView? = null
            private set
        private val progressBar: ProgressBar
        private val defaultBannerHeight = (50 * Resources.getSystem().displayMetrics.density).toInt()
        private val adContentLayout: FrameLayout

        var isLoading: Boolean = false
            set(value) {
                field = value
                progressBar.visibility = if (value) View.VISIBLE else View.GONE
                adView?.visibility = if (value) View.INVISIBLE else View.VISIBLE
            }

        init {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                defaultBannerHeight
            )

            // Create ad content layout
            adContentLayout = FrameLayout(context).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            addView(adContentLayout)

            // Setup progress bar
            progressBar = ProgressBar(context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                visibility = View.GONE
            }
            addView(progressBar)
        }

        fun setAdView(newAdView: AdView) {
            adView?.let { 
                adContentLayout.removeView(it)
                it.destroy()
            }
            
            adView = newAdView
            newAdView.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
            adContentLayout.addView(newAdView)
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            val width = right - left
            val height = bottom - top

            // Layout ad content
            adContentLayout.layout(0, 0, width, height)

            // Center the progress bar
            val progressWidth = progressBar.measuredWidth
            val progressHeight = progressBar.measuredHeight
            val progressLeft = (width - progressWidth) / 2
            val progressTop = (height - progressHeight) / 2
            progressBar.layout(
                progressLeft,
                progressTop,
                progressLeft + progressWidth,
                progressTop + progressHeight
            )
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = defaultBannerHeight

            // Measure ad content layout
            adContentLayout.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )

            // Measure progress bar with wrap content
            progressBar.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
            )

            setMeasuredDimension(width, height)
        }

        fun cleanup() {
            adView?.let {
                adContentLayout.removeView(it)
                it.destroy()
                adView = null
            }
            isLoading = false
        }
    }

    private fun createAdView(context: ThemedReactContext): AdView {
        Log.d(TAG, "Creating new AdView")
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun initializeAdView(view: AdView) {
        Log.d(TAG, "Initializing AdView")
        view.adListener = createAdListener(view)
    }

    @ReactProp(name = "adUnitId")
    fun setAdUnitId(container: AdContainer, adUnitId: String?) {
        if (adUnitId == null) return
        container.adView?.let { adView ->
            Log.d(TAG, "Setting ad unit ID: $adUnitId")
            if (adView.adUnitId != adUnitId) {
                adView.adUnitId = adUnitId
            }
        }
    }

    override fun createViewInstance(reactContext: ThemedReactContext): AdContainer {
        return AdContainer(reactContext).also { container ->
            val adView = createAdView(reactContext)
            container.setAdView(adView)
            initializeAdView(adView)
        }
    }

    override fun getCommandsMap(): Map<String, Int> = mapOf(
        "loadAd" to COMMAND_LOAD,
        "pauseAd" to COMMAND_PAUSE,
        "resumeAd" to COMMAND_RESUME
    )

    override fun receiveCommand(root: AdContainer, commandId: Int, args: ReadableArray?) {
        root.adView?.let { adView ->
            when (commandId) {
                COMMAND_LOAD -> loadAd(adView, root)
                COMMAND_PAUSE -> adView.pause()
                COMMAND_RESUME -> adView.resume()
                else -> Log.e(TAG, "Unknown command: $commandId")
            }
        }
    }

    private fun createAdListener(adView: AdView) = object : AdListener() {
        override fun onAdLoaded() {
            Log.d(TAG, "Ad loaded successfully")
            (adView.parent?.parent as? AdContainer)?.let { container ->
                container.isLoading = false
                
                val event = Arguments.createMap().apply {
                    putInt("height", adView.adSize?.getHeightInPixels(adView.context) ?: 0)
                }
                sendEvent(container, EVENT_AD_LOADED, event)
            }
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            Log.e(TAG, """
                Ad failed to load:
                Error Code: ${error.code}
                Error Domain: ${error.domain}
                Error Message: ${error.message}
            """.trimIndent())
            
            (adView.parent?.parent as? AdContainer)?.let { container ->
                container.isLoading = false
                sendEvent(container, EVENT_AD_FAILED_TO_LOAD, createErrorEvent(error.message, error.code))
            }
        }

        override fun onAdOpened() {
            Log.d(TAG, "Ad opened")
            sendEvent(adView.parent?.parent as AdContainer, EVENT_AD_OPENED, createEvent("Ad opened"))
        }

        override fun onAdClosed() {
            Log.d(TAG, "Ad closed")
            sendEvent(adView.parent?.parent as AdContainer, EVENT_AD_CLOSED, createEvent("Ad closed"))
        }

        override fun onAdClicked() {
            Log.d(TAG, "Ad clicked")
            sendEvent(adView.parent?.parent as AdContainer, EVENT_AD_CLICKED, createEvent("Ad clicked"))
        }

        override fun onAdImpression() {
            Log.d(TAG, "Ad impression recorded")
            sendEvent(adView.parent?.parent as AdContainer, EVENT_AD_IMPRESSION, createEvent("Ad impression recorded"))
        }
    }

    private fun loadAd(adView: AdView, container: AdContainer) {
        try {
            Log.d(TAG, "Loading ad with unit ID: ${adView.adUnitId}")
            container.isLoading = true
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            Log.d(TAG, "Ad request sent")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading ad", e)
            container.isLoading = false
            sendEvent(container, EVENT_AD_FAILED_TO_LOAD, createErrorEvent(e.message ?: "Unknown error"))
        }
    }

    private fun sendEvent(container: AdContainer, eventName: String, params: WritableMap) {
        try {
            (container.context as ReactContext)
                .getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(container.id, eventName, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending event", e)
        }
    }

    private fun createEvent(message: String): WritableMap =
        Arguments.createMap().apply {
            putString("message", message)
        }

    private fun createErrorEvent(error: String, code: Int? = null): WritableMap =
        Arguments.createMap().apply {
            putString("error", error)
            code?.let { putInt("code", it) }
        }

    override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any> = MapBuilder.builder<String, Any>()
        .put(EVENT_AD_LOADED, MapBuilder.of("registrationName", EVENT_AD_LOADED))
        .put(EVENT_AD_FAILED_TO_LOAD, MapBuilder.of("registrationName", EVENT_AD_FAILED_TO_LOAD))
        .put(EVENT_AD_OPENED, MapBuilder.of("registrationName", EVENT_AD_OPENED))
        .put(EVENT_AD_CLOSED, MapBuilder.of("registrationName", EVENT_AD_CLOSED))
        .put(EVENT_AD_CLICKED, MapBuilder.of("registrationName", EVENT_AD_CLICKED))
        .put(EVENT_AD_IMPRESSION, MapBuilder.of("registrationName", EVENT_AD_IMPRESSION))
        .build()

    companion object {
        private const val TAG = "GoogleAdViewManager"
        
        // Commands
        private const val COMMAND_LOAD = 1
        private const val COMMAND_PAUSE = 2
        private const val COMMAND_RESUME = 3
        
        // Events
        const val EVENT_AD_LOADED = "onAdLoaded"
        const val EVENT_AD_FAILED_TO_LOAD = "onAdFailedToLoad"
        const val EVENT_AD_OPENED = "onAdOpened"
        const val EVENT_AD_CLOSED = "onAdClosed"
        const val EVENT_AD_CLICKED = "onAdClicked"
        const val EVENT_AD_IMPRESSION = "onAdImpression"
    }
} 