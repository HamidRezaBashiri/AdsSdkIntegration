package com.mobileaddemo

import android.app.Application
import android.util.Log
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactInstanceEventListener
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.shell.MainReactPackage
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import com.mobileaddemo.ads.core.AdManager
import com.mobileaddemo.ads.core.AdProvider
import com.mobileaddemo.ads.google.GoogleAdSDK
import com.mobileaddemo.ads.google.GoogleAdPackage
import com.mobileaddemo.ads.inmobi.InMobiAdSDK
import com.mobileaddemo.ads.inmobi.InMobiAdPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Application class responsible for React Native initialization
 * and Ad SDK setup
 */
class MainApplication : Application(), ReactApplication {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val googleAdSDK: GoogleAdSDK by lazy { GoogleAdSDK.getInstance(this) }
    private val inMobiAdSDK: InMobiAdSDK by lazy { InMobiAdSDK.getInstance(this) }
    private val adManager: AdManager by lazy { AdManager.getInstance() }

    private val _reactNativeHost: ReactNativeHost by lazy {
        object : DefaultReactNativeHost(this) {
            override fun getPackages(): List<ReactPackage> {
                val packages = PackageList(this).packages.toMutableList()
                packages.add(MainReactPackage())
                packages.add(GoogleAdPackage())
                packages.add(InMobiAdPackage())
                return packages
            }

            override fun getJSMainModuleName(): String = "index"
            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG
            override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
            override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
        }
    }

    override val reactNativeHost: ReactNativeHost get() = _reactNativeHost

    override val reactHost: ReactHost
        get() = getDefaultReactHost(applicationContext, reactNativeHost)

    override fun onCreate() {
        super.onCreate()
        initializeReactNative()
        initializeAdSDKs()
    }

    private fun initializeReactNative() {
        SoLoader.init(this, OpenSourceMergedSoMapping)
        if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
            load()
        }
    }

    private fun initializeAdSDKs() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Starting Ad SDK initialization")

                // Register SDKs
                adManager.registerSDK(AdProvider.GOOGLE, googleAdSDK)
                adManager.registerSDK(AdProvider.INMOBI, inMobiAdSDK)

                // Initialize
                val initialized = adManager.initialize(applicationContext)
                if (!initialized) {
                    Log.e(TAG, "Failed to initialize one or more Ad SDKs")
                } else {
                    Log.i(TAG, "Successfully initialized Ad SDKs")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Ad SDKs", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }

    companion object {
        private const val TAG = "MainApplication"
    }
}
