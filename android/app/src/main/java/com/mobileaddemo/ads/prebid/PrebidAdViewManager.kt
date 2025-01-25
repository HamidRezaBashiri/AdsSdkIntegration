package com.mobileaddemo.ads.prebid

import android.graphics.Color
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import org.prebid.mobile.BannerAdUnit
import org.prebid.mobile.ResultCode

class PrebidAdViewManager : SimpleViewManager<FrameLayout>() {

    companion object {
        private const val REACT_CLASS = "PrebidAdView"
    }

    private var configId: String = "prebid-demo-banner-320-50"
    private var width: Int = 320
    private var height: Int = 50

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(themedReactContext: ThemedReactContext): FrameLayout {
        return FrameLayout(themedReactContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    @ReactProp(name = "configId")
    fun setConfigId(view: FrameLayout, configIdFromJS: String) {
        configId = configIdFromJS
        Log.d(REACT_CLASS, "Config ID set to: $configId")
    }

    @ReactProp(name = "width", defaultInt = 320)
    fun setAdWidth(view: FrameLayout, w: Int) {
        width = w
        Log.d(REACT_CLASS, "Width set to: $width")
    }

    @ReactProp(name = "height", defaultInt = 50)
    fun setAdHeight(view: FrameLayout, h: Int) {
        height = h
        Log.d(REACT_CLASS, "Height set to: $height")
    }

    override fun onAfterUpdateTransaction(view: FrameLayout) {
        super.onAfterUpdateTransaction(view)

        Log.d(REACT_CLASS, "Fetching demand for configId: $configId with width: $width, height: $height")

        val bannerAdUnit = BannerAdUnit(configId, width, height)
        val adRequest = AdManagerAdRequest.Builder().build()

        val adView = AdManagerAdView(view.context).apply {
            setAdSizes(AdSize(width, height))
        }

        view.removeAllViews()

        bannerAdUnit.fetchDemand(adRequest) { resultCode ->
            if (resultCode == ResultCode.SUCCESS) {
                Log.d(REACT_CLASS, "Demand fetched successfully. Loading GAM ad...")
                adView.loadAd(adRequest)
                view.addView(adView)
            } else {
                Log.e(REACT_CLASS, "Failed to fetch demand. Showing fallback message.")
                view.post {
                    showFallbackMessage(view, "No ads available for this placement.")
                }
            }
        }
    }

    private fun showFallbackMessage(view: FrameLayout, message: String) {
        Log.d("PrebidAdViewManager", "Adding fallback message view.")

        // Create the TextView
        val textView = TextView(view.context).apply {
            text = message
            textSize = 16f
            setBackgroundColor(Color.YELLOW)
            setTextColor(Color.BLACK)
            setPadding(16, 16, 16, 16)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Remove all existing children and add the TextView
        view.removeAllViews()
        view.addView(textView)

        // Force layout pass and measure the TextView
        view.post {
            textView.measure(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            textView.layout(0, 0, textView.measuredWidth, textView.measuredHeight)

            // Log the dimensions of the TextView after layout pass
            Log.d("PrebidAdViewManager", "TextView final width: ${textView.measuredWidth}, height: ${textView.measuredHeight}")

            // Apply fallback dimensions if necessary
            if (textView.measuredWidth == 0 || textView.measuredHeight == 0) {
                textView.layoutParams = FrameLayout.LayoutParams(320, 50) // Default size
                textView.requestLayout()
                Log.d("PrebidAdViewManager", "Fallback size applied to TextView.")
            }

            logViewHierarchy(view) // Log view hierarchy
        }
    }



    private fun logViewHierarchy(view: FrameLayout) {
        Log.d(REACT_CLASS, "FrameLayout children count: ${view.childCount}")
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            Log.d(REACT_CLASS, "Child at $i: ${child.javaClass.simpleName}")
        }
    }
}
