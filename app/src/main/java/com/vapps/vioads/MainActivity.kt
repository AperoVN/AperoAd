package com.vapps.vioads

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.vapps.module_ads.control.helper.adnative.NativeAdConfig
import com.vapps.module_ads.control.helper.adnative.NativeAdHelper
import com.vapps.module_ads.control.helper.adnative.params.NativeAdParam
import com.vapps.module_ads.control.helper.appoppen.AppOpenAdConfig
import com.vapps.module_ads.control.helper.appoppen.AppOpenAdHelper
import com.vapps.module_ads.control.helper.appoppen.params.AppOpenAdParam
import com.vapps.module_ads.control.helper.banner.BannerAdConfig
import com.vapps.module_ads.control.helper.banner.BannerAdHelper
import com.vapps.module_ads.control.helper.banner.params.BannerAdParam
import com.vapps.module_ads.control.helper.interstitial.InterstitialAdConfig
import com.vapps.module_ads.control.helper.interstitial.InterstitialAdHelper
import com.vapps.module_ads.control.helper.interstitial.InterstitialAdSplashConfig
import com.vapps.module_ads.control.helper.interstitial.InterstitialAdSplashHelper
import com.vapps.module_ads.control.helper.interstitial.params.InterstitialAdParam
import com.vapps.module_ads.control.listener.AdCallback
import com.vapps.vioads.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val bannerAdHelper by lazy { initBannerAd() }
    private val interAdHelper by lazy { initInterAdAd() }
    private val interAdSplashHelper by lazy { initInterAdSplash() }
    private val appOpenAdHelper by lazy { initAppOpenAd() }

    private val nativeAdHelper by lazy { initNativeAd() }
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        bannerAdHelper.setBannerContentView(binding.frAds)
            .apply { setTagForDebug("BANNER=>>>") }
        nativeAdHelper
            .setNativeContentView(binding.flNativeAds)
            .setShimmerLayoutView(binding.includeShimmer.shimmerContainerNative)
        nativeAdHelper.requestAds(NativeAdParam.Request)
        bannerAdHelper.requestAds(BannerAdParam.Request)
        binding.btnReload.setOnClickListener {
            bannerAdHelper.requestAds(BannerAdParam.Request)
            nativeAdHelper.requestAds(NativeAdParam.Request)
        }
        binding.btnShowDialog.setOnClickListener {
            bannerAdHelper.flagUserEnableReload = !bannerAdHelper.flagUserEnableReload
            nativeAdHelper.flagUserEnableReload = !nativeAdHelper.flagUserEnableReload
        }
        interAdHelper.requestAds(InterstitialAdParam.Request)
        //interAdSplashHelper.requestAds(InterstitialAdParam.Request)
        //appOpenAdHelper.requestAds(AppOpenAdParam.Request)
        binding.btnShowInter.setOnClickListener {
            interAdHelper.requestAds(InterstitialAdParam.ShowAd)
        }
    }

    private fun initInterAdAd(): InterstitialAdHelper {
        val config = InterstitialAdConfig(
            idAds = "ca-app-pub-3940256099942544/1033173712",
            canShowAds = true,
            canReloadAds = true,
            reloadAd = true,
            showByTime = 5
        )
        return InterstitialAdHelper(activity = this, lifecycleOwner = this, config = config).apply {
            registerAdListener(object : AdCallback() {
                override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                    super.onInterstitialLoad(interstitialAd)
                    //requestAds(InterstitialAdParam.Show(interstitialAd!!))
                }
            })
        }
    }

    private fun initInterAdSplash(): InterstitialAdSplashHelper {
        val config = InterstitialAdSplashConfig(
            idAds = "ca-app-pub-3940256099942544/1033173712",
            canShowAds = true,
            canReloadAds = true,
            timeDelay = 500L,
            timeOut = 30000L,
            showReady = false
        )
        return InterstitialAdSplashHelper(
            activity = this,
            lifecycleOwner = this,
            config = config
        ).apply {
            registerAdListener(object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    Toast.makeText(this@MainActivity, "OKOK", Toast.LENGTH_SHORT).show()
                }

                override fun onInterstitialShow() {
                    super.onInterstitialShow()
                    Log.e(TAG, "onInterstitialShow: ")
                }

                override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                    super.onInterstitialLoad(interstitialAd)
                    Log.e("InterstitialAdSplash", "onInterstitialLoad:qwewqe ")
                    interAdSplashHelper.requestAds(InterstitialAdParam.Show(interstitialAd!!))
                }
            })

        }
    }

    private fun initAppOpenAd(): AppOpenAdHelper {
        val config = AppOpenAdConfig(
            idAds = "ca-app-pub-3940256099942544/9257395921",
            canShowAds = true,
            canReloadAds = true,
            timeDelay = 500L,
            timeOut = 30000L,
            showReady = false
        )
        return AppOpenAdHelper(
            activity = this,
            lifecycleOwner = this,
            config = config
        ).apply {
            registerAdListener(object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    Toast.makeText(this@MainActivity, "OKOK", Toast.LENGTH_SHORT).show()
                }

                override fun onAppOpenAdShow() {
                    super.onAppOpenAdShow()
                    Log.e(TAG, "onAppOpenAdShow: ", )
                }

                override fun onAppOpenAdLoaded(appOpenAd: AppOpenAd) {
                    super.onAppOpenAdLoaded(appOpenAd)
                    Log.e(TAG, "onAppOpenAdLoaded: ", )
                    appOpenAdHelper.requestAds(AppOpenAdParam.Show)
                }
            })

        }
    }

    private fun initBannerAd(): BannerAdHelper {
        val config = BannerAdConfig(
            idAds = "ca-app-pub-3940256099942544/6300978111",
            canShowAds = true,
            canReloadAds = true,
        )
        return BannerAdHelper(activity = this, lifecycleOwner = this, config = config)
    }

    private fun initNativeAd(): NativeAdHelper {
        val config = NativeAdConfig(
            idAds = "ca-app-pub-3940256099942544/2247696110",
            canShowAds = true,
            canReloadAds = true,
            layoutId = R.layout.custom_native_admod_medium
        )
        return NativeAdHelper(this, this, config)
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }
}