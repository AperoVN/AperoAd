package com.vapps.module_ads.control.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.vapps.module_ads.control.config.VioAdConfig
import com.vapps.module_ads.control.listener.AdCallback

interface AdmobFactory {
    fun initAdmob(context: Context, vioAdConfig: VioAdConfig)
    fun requestBannerAd(context: Context, adId: String, adCallback: AdCallback)

    fun requestNativeAd(context: Context, adId: String, adCallback: AdCallback)

    fun populateNativeAdView(
        activity: Context,
        nativeAd: NativeAd,
        @LayoutRes nativeAdViewId: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        adCallback: AdCallback
    )
    fun requestInterstitialAds(context: Context, adId: String, adCallback: AdCallback)

    fun showInterstitial(context: Context, interstitialAd: InterstitialAd?, adCallback: AdCallback)

    fun requestAppOpenAds(context: Context, adId: String, adCallback: AdCallback)
    fun showAppOpenAds(activity: Activity, appOpenAd: AppOpenAd, adCallback: AdCallback)
    companion object {
        @JvmStatic
        fun getInstance(): AdmobFactory = AdmobFactoryImpl()
    }
}