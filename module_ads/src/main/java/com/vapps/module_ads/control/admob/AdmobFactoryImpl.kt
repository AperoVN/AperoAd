package com.vapps.module_ads.control.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.IntDef
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.vapps.module_ads.R
import com.vapps.module_ads.control.billing.AppPurchase
import com.vapps.module_ads.control.config.ErrorCode
import com.vapps.module_ads.control.config.VioAdConfig
import com.vapps.module_ads.control.event.VioLogEventManager
import com.vapps.module_ads.control.listener.AdCallback
import com.vapps.module_ads.control.utils.AdType

class AdmobFactoryImpl : AdmobFactory {
    private lateinit var vioAdConfig: VioAdConfig
    override fun initAdmob(
        context: Context,
        adConfig: VioAdConfig
    ) {
        this.vioAdConfig = adConfig
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = Application.getProcessName()
            val packageName = context.packageName
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }
        MobileAds.initialize(context) { initializationStatus: InitializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                Log.d(
                    TAG, String.format(
                        "Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status!!.description, status.latency
                    )
                )
            }
        }
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(adConfig.listTestDevices).build()
        )
    }

    override fun requestBannerAd(context: Context, adId: String, adCallback: AdCallback) {
        if (AppPurchase.instance?.isPurchased == true) {
            adCallback.onAdFailedToLoad(
                LoadAdError(
                    1999,
                    "App isPurchased",
                    "",
                    null,
                    null
                )
            )
            return
        }
        try {
            val adView = AdView(context)
            adView.adUnitId = adId
            val adSize = getAdSize(context as Activity, false, BannerInlineStyle.LARGE_STYLE)
            adView.setAdSize(adSize)
            adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adCallback.onAdFailedToLoad(loadAdError)
                }

                override fun onAdLoaded() {
                    adCallback.onBannerLoaded(adView)
                    adView.onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
                        VioLogEventManager.logPaidAdImpression(
                            context,
                            adValue,
                            adView.adUnitId,
                            adView.responseInfo!!
                                .mediationAdapterClassName!!, AdType.BANNER
                        )
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onAdClicked()
                    VioLogEventManager.logClickAdsEvent(context, adId)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    adCallback.onAdImpression()
                }
            }
            adView.loadAd(adRequest)
        } catch (ex: Exception) {
            adCallback.onAdFailedToLoad(
                LoadAdError(
                    1991,
                    ex.message.toString(),
                    "",
                    null,
                    null
                )
            )
        }
    }

    override fun requestNativeAd(
        context: Context,
        adId: String,
        adCallback: AdCallback
    ) {
        if (AppPurchase.instance?.isPurchased == true) {
            adCallback.onAdFailedToLoad(
                LoadAdError(
                    1999,
                    "App isPurchased",
                    "",
                    null,
                    null
                )
            )
            return
        }
        val builder = AdLoader.Builder(context, adId)

        val videoOptions =
            VideoOptions.Builder().setStartMuted(true).build()

        val adOptions = com.google.android.gms.ads.nativead.NativeAdOptions.Builder()
            .setVideoOptions(videoOptions).build()

        builder.withNativeAdOptions(adOptions)
        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { nativeAd ->
                adCallback.onUnifiedNativeAdLoaded(nativeAd)
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    VioLogEventManager.logPaidAdImpression(
                        context,
                        adValue,
                        adId,
                        nativeAd.responseInfo!!.mediationAdapterClassName!!, AdType.NATIVE
                    )
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adCallback.onAdFailedToLoad(error)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    adCallback.onAdImpression()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onAdClicked()
                    VioLogEventManager.logClickAdsEvent(context, adId)
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
        adLoader.loadAd(adRequest)
    }

    override fun populateNativeAdView(
        context: Context,
        nativeAd: NativeAd,
        nativeAdViewId: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        adCallback: AdCallback
    ) {
        val adView = LayoutInflater.from(context).inflate(nativeAdViewId, null) as NativeAdView
        // Set the media view.
        adView.mediaView = adView.findViewById(R.id.ad_media)

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let { (adView.mediaView)?.setMediaContent(it) }


        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        nativeAd.body?.let {
            adView.bodyView?.visibility = View.VISIBLE
            adView.bodyView?.let { view ->
                (view as TextView).text = it
            }
        } ?: kotlin.run {
            adView.bodyView?.visibility = View.INVISIBLE
        }

        nativeAd.callToAction?.let {
            adView.callToActionView?.visibility = View.VISIBLE
            adView.callToActionView?.let { view ->
                (view as TextView).text = it
            }
        } ?: kotlin.run {
            adView.callToActionView?.visibility = View.INVISIBLE
        }
        nativeAd.icon?.let {
            adView.iconView?.visibility = View.VISIBLE
            adView.iconView?.let { view ->
                (view as ImageView).setImageDrawable(it.drawable)
            }
        } ?: kotlin.run {
            adView.iconView?.visibility = View.GONE
        }
        nativeAd.price?.let {
            adView.priceView?.visibility = View.VISIBLE
            adView.priceView?.let { view ->
                (view as TextView).text = it
            }
        } ?: kotlin.run {
            adView.priceView?.visibility = View.INVISIBLE
        }

        nativeAd.starRating?.let {
            adView.starRatingView?.visibility = View.VISIBLE
            adView.starRatingView?.let { view ->
                (view as RatingBar).rating = it.toFloat()
            }
        } ?: kotlin.run {
            adView.starRatingView?.visibility = View.INVISIBLE
        }
        nativeAd.advertiser?.let {
            adView.advertiserView?.visibility = View.VISIBLE
            adView.advertiserView?.let { view ->
                (view as TextView).text = it
            }

        } ?: kotlin.run {
            adView.advertiserView?.visibility = View.INVISIBLE
        }
        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        val mediaContent = nativeAd.mediaContent
        val vc = mediaContent?.videoController

        // Updates the UI to say whether or not this ad has a video asset.
        if (vc != null && mediaContent.hasVideoContent()) {
            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        // Publishers should allow native ads to complete video playback before
                        // refreshing or replacing them with another ad in the same UI location.
                        super.onVideoEnd()
                    }
                }
        }
        try {
            adPlaceHolder.visibility = View.VISIBLE
            adPlaceHolder.removeAllViews()
            adPlaceHolder.addView(adView)
            containerShimmerLoading?.visibility = View.GONE
        } catch (ex: Exception) {
            adCallback.onAdFailedToShow(AdError(ErrorCode.SHOW_FAIL_CODE, "", ""))
        }
    }

    override fun requestInterstitialAds(context: Context, adId: String, adCallback: AdCallback) {
        if (AppPurchase.instance?.isPurchased == true) {
            adCallback.onAdFailedToLoad(LoadAdError(ErrorCode.PURCHASED, "", "", null, null))
            return
        }
        InterstitialAd.load(
            context,
            adId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    adCallback.onAdFailedToLoad(adError)
                    Log.e(TAG, "onAdFailedToLoad: ", )
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    adCallback.onInterstitialLoad(ad)
                    Log.e(TAG, "onAdLoaded: ", )
                }
            }
        )
    }

    override fun showInterstitial(
        context: Context,
        interstitialAd: InterstitialAd?,
        adCallback: AdCallback
    ) {
        if (AppPurchase.instance?.isPurchased == true || interstitialAd == null) {
            adCallback.onNextAction()
            return
        }

        interstitialAd.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    adCallback.onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    adCallback.onAdFailedToShow(adError)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                    // Called when ad is dismissed.
                    adCallback.onInterstitialShow()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onAdClicked()
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    adCallback.onAdImpression()
                }
            }
        interstitialAd.show(context as Activity)
    }

    override fun requestAppOpenAds(context: Context, adId: String, adCallback: AdCallback) {
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            adId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                /**
                 * Called when an app open ad has loaded.
                 *
                 * @param ad the loaded app open ad.
                 */
                override fun onAdLoaded(ad: AppOpenAd) {
                    adCallback.onAppOpenAdLoaded(ad)
                }

                /**
                 * Called when an app open ad has failed to load.
                 *
                 * @param loadAdError the error.
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adCallback.onAdFailedToLoad(loadAdError)
                }
            }
        )
    }

    override fun showAppOpenAds(activity: Activity, appOpenAd: AppOpenAd, adCallback: AdCallback) {
        appOpenAd.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                /** Called when full screen content is dismissed. */
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    adCallback.onNextAction()
                }

                /** Called when fullscreen content failed to show. */
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    adCallback.onAdFailedToShow(adError)
                }

                /** Called when fullscreen content is shown. */
                override fun onAdShowedFullScreenContent() {
                    adCallback.onAppOpenAdShow()
                }
            }
        appOpenAd.show(activity)
    }

    companion object {
        private val TAG = AdmobFactoryImpl::class.simpleName
    }
}

@IntDef(BannerInlineStyle.SMALL_STYLE, BannerInlineStyle.LARGE_STYLE)
annotation class BannerInlineStyle {
    companion object {
        const val SMALL_STYLE = 0
        const val LARGE_STYLE = 1
    }
}