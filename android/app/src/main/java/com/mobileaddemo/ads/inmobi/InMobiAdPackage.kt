package com.mobileaddemo.ads.inmobi

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

/**
 * ReactPackage implementation for InMobi ads.
 * This package provides the necessary view managers for InMobi ads to be used in a React Native application.
 */
class InMobiAdPackage : ReactPackage {
    /**
     * Creates and returns a list of native modules to be registered with React Native.
     * In this case, no native modules are needed, so an empty list is returned.
     * @param reactContext The React application context.
     * @return A list of native modules.
     */
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return emptyList()
    }

    /**
     * Creates and returns a list of view managers to be registered with React Native.
     * This includes the InMobiViewManager for managing InMobi ad views.
     * @param reactContext The React application context.
     * @return A list of view managers.
     */
    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return listOf(InMobiViewManager(reactContext))
    }
}