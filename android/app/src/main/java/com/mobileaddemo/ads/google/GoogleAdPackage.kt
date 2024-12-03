package com.mobileaddemo.ads.google

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import com.mobileaddemo.ads.core.AdManager
import com.mobileaddemo.ads.core.AdProvider

/**
 * React Native package for Google Ad integration
 * Provides native modules for ad functionality
 */
class GoogleAdPackage : ReactPackage {
    /**
     * Creates native modules that can be accessed from JavaScript
     * @return List containing the Google Ads native module
     */
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return emptyList()
    }

    /**
     * Creates view managers for rendering ad components
     * Note: View managers are provided by GAMAdPackage instead
     * @return Empty list since view managers are handled elsewhere
     */
    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return listOf(GoogleAdViewManager())
    }
} 