package com.mobileaddemo.ads.prebid

import android.content.Context
import android.widget.FrameLayout

/**
 * This container extends FrameLayout, just like Google's ReactNativeAdView does.
 * It stores props (configId, adUnitId) and a boolean `propsChanged` to track if we need to reload.
 * It also overrides requestLayout & measureAndLayout for fluid sizing.
 */
class PrebidAdView(context: Context) : FrameLayout(context) {

    // Prebid / GAM identifiers
    var configId: String? = null
    var adUnitId: String? = null

    // If propsChanged is true, manager will call loadAd in onAfterUpdateTransaction
    var propsChanged: Boolean = false

    // If you ever want a "fluid" size approach (like AdSize.FLUID), set this to true
    var isFluid: Boolean = false

    override fun requestLayout() {
        // Google uses the same pattern to ensure fluid ads are properly measured
        super.requestLayout()
        post(measureAndLayout)
    }

    private val measureAndLayout = Runnable {
        // If fluid, we measure with UNSPECIFIED for height to let the ad expand.
        // If not fluid, we measure EXACTLY with the current layout height.
        val heightSpec = if (isFluid) {
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        } else {
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }

        // For width, we just use EXACTLY with whatever width we currently have.
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            heightSpec
        )
        // Then layout at our current position
        layout(left, top, right, top + measuredHeight)
    }
}
