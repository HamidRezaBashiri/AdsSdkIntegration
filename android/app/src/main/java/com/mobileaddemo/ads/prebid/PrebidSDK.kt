 package com.mobileaddemo.ads.prebid

 import android.content.Context
 import android.util.Log
 import android.view.View
 import com.google.android.gms.ads.AdListener
 import com.google.android.gms.ads.AdSize
 import com.google.android.gms.ads.LoadAdError
 import com.google.android.gms.ads.MobileAds
 import com.google.android.gms.ads.admanager.AdManagerAdRequest
 import com.google.android.gms.ads.admanager.AdManagerAdView
 import com.inmobi.ads.InMobiBanner
 import com.mobileaddemo.ads.core.AdResult
 import com.mobileaddemo.ads.core.AdSDK
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.channels.awaitClose
 import kotlinx.coroutines.flow.Flow
 import kotlinx.coroutines.flow.callbackFlow
 import kotlinx.coroutines.suspendCancellableCoroutine
 import kotlinx.coroutines.withContext
 import org.prebid.mobile.BannerAdUnit
 import org.prebid.mobile.Host
 import org.prebid.mobile.PrebidMobile
 import java.util.concurrent.ConcurrentHashMap
 import kotlin.coroutines.resume

 class PrebidSDK private constructor(private val context: Context) : AdSDK {

     private var initialized = false
     private val adViewCache = ConcurrentHashMap<String, InMobiBanner>()

     override val name: String = "Prebid"

     override suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.Main) {
         try {
             if (initialized) return@withContext true

             suspendCancellableCoroutine { continuation ->
                 try {
                     PrebidMobile.setPrebidServerAccountId("0689a263-318d-448b-a3d4-b02e8a709d9d")

                     PrebidMobile.setPrebidServerHost(Host.createCustomHost("https://prebid-server-test-j.prebid.org/openrtb2/auction"))

//                   PrebidMobile.setPrebidServerHost(Host.APPNEXUS)

//                     PrebidMobile.setCustomStatusEndpoint(PREBID_SERVER_STATUS_ENDPOINT)

//                     stored auction responses signal Prebid Server to respond with a static response matching the storedAuctionResponse found in the Prebid Server Database, useful for debugging and integration testing.
                     PrebidMobile.setStoredAuctionResponse("prebid-demo-banner-320-50")
                     PrebidMobile.setPbsDebug(true)
                     PrebidMobile.setLogLevel(PrebidMobile.LogLevel.DEBUG)
                     //Check compatibility with your GMA SDK
                     PrebidMobile.checkGoogleMobileAdsCompatibility(MobileAds.getVersion().toString())

                     // Initialize the Prebid SDK
                     PrebidMobile.initializeSdk(context) { status ->
                         initialized = true
                         Log.i(TAG, "onInitializationComplete: $status ")
                         continuation.resume(true)

                     }



                 } catch (e: Exception) {
                     Log.e(TAG, "Error initializing Prebid SDK", e)
                     continuation.resume(false)
                 }
             }
         } catch (e: Exception) {
             Log.e(TAG, "Error in initialize", e)
             false
         }
     }

     override fun isInitialized(): Boolean = initialized

     override suspend fun loadAd(adUnitId: String): Flow<AdResult> = callbackFlow {
         if (!isInitialized()) {
             trySend(AdResult.Error(adUnitId, "SDK not initialized"))
             close()
             return@callbackFlow
         }

         val  adUnit = BannerAdUnit("prebid-demo-banner-320-50"	, 320, 50)

         val adView = AdManagerAdView(context)
         adView.adUnitId = "prebid-demo-banner-320-50"
         adView.setAdSizes(AdSize.BANNER)
         adView.adListener = object : AdListener() {
             override fun onAdLoaded() {
                 Log.d("Prebid", "Ad loaded")
             }

             override fun onAdFailedToLoad(p0: LoadAdError) {
                 Log.e(TAG, "onAdFailedToLoad: $p0", )
                 super.onAdFailedToLoad(p0)
             }
         }
//         withContext(Dispatchers.Main) {
//          val adView = getOrCreateAdView(adUnitId)

         // Add GMA SDK banner view to the app UI
//         adWrapperView.addView(adView)

         // 4. Make a bid request to Prebid Server
         val request = AdManagerAdRequest.Builder().build()
         adUnit.fetchDemand( request) {
             // 5. Load GAM Ad
             Log.i(TAG, "loadAd: ")
             adView.loadAd(request)
         }
//
//             trySend(AdResult.Loading(adUnitId))
//
//             banner.setListener(object : BannerAdEventListener() {
//                 override fun onAdLoadSucceeded(banner: InMobiBanner, adMetaInfo: AdMetaInfo) {
//                     trySend(AdResult.Success(adUnitId))
//                 }
//
//                 override fun onAdLoadFailed(
//                     banner: InMobiBanner,
//                     status: InMobiAdRequestStatus
//                 ) {
//                     status.message?.let { AdResult.Error(adUnitId, it) }?.let { trySend(it) }
//                 }
//
//                 override fun onAdClicked(banner: InMobiBanner, p1: MutableMap<Any, Any>?) {
//                     Log.d(TAG, "Ad clicked")
//                 }
//
//                 override fun onAdDisplayed(banner: InMobiBanner) {
//                     Log.d(TAG, "Ad displayed")
//                 }
//
//                 override fun onAdDismissed(banner: InMobiBanner) {
//                     Log.d(TAG, "Ad dismissed")
//                 }
//             })
//
//             banner.load()
//         }

         awaitClose {
             cleanup(adUnitId)
         }
     }

     override fun createAdView(context: Context, adUnitId: String): View {


         val adView = AdManagerAdView(context)
         adView.adUnitId = "prebid-demo-banner-320-50"
         adView.setAdSizes(AdSize.BANNER)
         adView.adListener = object : AdListener() {
             override fun onAdLoaded() {
                 Log.d("Prebid", "Ad loaded")
             }
         }

         return adView
     }

     override fun cleanup(adUnitId: String) {
         adViewCache[adUnitId]?.let { banner ->
             banner.destroy()
             adViewCache.remove(adUnitId)
         }
     }

     override fun destroy() {
         adViewCache.forEach { (_, banner) -> banner.destroy() }
         adViewCache.clear()
         initialized = false
     }

     override fun getOrCreateAdView(adUnitId: String): View {
         return adViewCache.getOrPut(adUnitId) {
             createAdView(context, adUnitId) as InMobiBanner
         }
     }

     companion object {
         private const val TAG = "PrebidSDK"

         @Volatile
         private var instance: PrebidSDK? = null

         fun getInstance(context: Context): PrebidSDK =
             instance ?: synchronized(this) {
                 instance ?: PrebidSDK(context.applicationContext).also { instance = it }
             }
     }
 }