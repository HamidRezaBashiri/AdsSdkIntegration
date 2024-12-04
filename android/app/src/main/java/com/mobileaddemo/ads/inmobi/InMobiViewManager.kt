package com.mobileaddemo.ads.inmobi

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.inmobi.ads.AdMetaInfo
import com.inmobi.ads.InMobiAdRequestStatus
import com.inmobi.ads.InMobiBanner
import com.inmobi.ads.listeners.BannerAdEventListener

class InMobiViewManager(
    private val reactContext: ReactApplicationContext
) : SimpleViewManager<View>() {

    override fun getName(): String = "InMobiBannerView"

    override fun createViewInstance(reactContext: ThemedReactContext): View {
        val banner = InMobiBanner(reactContext, 10000061355)
        banner.setBannerSize(350,50)
        return banner
    }

    @ReactProp(name = "placementId")
    fun setPlacementId(view: View, placementId: String) {
        if (view !is InMobiBanner) return
        
        try {
            val placement = placementId.toLong()
//            view.placementId = placement
            setupBannerListeners(view)
            view.load()
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Invalid placement ID: $placementId", e)
        }
    }

    private fun setupBannerListeners(banner: InMobiBanner) {
        banner.setListener(object : BannerAdEventListener() {
            override fun onAdLoadSucceeded(banner: InMobiBanner, adMetaInfo: AdMetaInfo) {
                Log.d(TAG, "InMobi ad loaded successfully")
            }

            override fun onAdLoadFailed(banner: InMobiBanner, status: InMobiAdRequestStatus) {
                Log.e(TAG, "InMobi ad failed to load: ${status.message}")
            }

            override fun onAdClicked(banner: InMobiBanner, p1: MutableMap<Any, Any>?) {
                Log.d(TAG, "InMobi ad clicked")
            }

            override fun onAdDisplayed(banner: InMobiBanner) {
                Log.d(TAG, "InMobi ad displayed")
            }

            override fun onAdDismissed(banner: InMobiBanner) {
                Log.d(TAG, "InMobi ad dismissed")
            }
        })
    }

    companion object {
        private const val TAG = "InMobiViewManager"
    }
} 