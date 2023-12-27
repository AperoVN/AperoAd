package com.vapps.vioads

import android.annotation.SuppressLint
import android.app.Activity
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdActivity
import com.vapps.module_ads.control.admob.AdmobFactory
import com.vapps.module_ads.control.admob.AdsMultiDexApplication
import com.vapps.module_ads.control.billing.AppPurchase
import com.vapps.module_ads.control.config.AdjustConfig
import com.vapps.module_ads.control.config.NetworkProvider
import com.vapps.module_ads.control.config.VioAdConfig
import com.vapps.module_ads.control.helper.appoppen.AppResumeAdConfig
import com.vapps.module_ads.control.helper.appoppen.AppResumeAdHelper
import com.vapps.module_ads.control.model.PurchaseItem

class MyApplication : AdsMultiDexApplication() {
    private val APPSFLYER_TOKEN = "2PUNpdyDTkedZTgeKkWCyB"
    private val ADJUST_TOKEN = "cc4jvudppczk"
    private val EVENT_PURCHASE_ADJUST = "gzel1k"
    private val EVENT_AD_IMPRESSION_ADJUST = "gzel1k"
    private val TAG = "MainApplication"
    private var currentActivity: Activity? = null
    private val appResumeAdHelper by lazy { initAppOpenAd() }

    override fun onCreate() {
        super.onCreate()
        application = this
        initBilling()
        configAds()
    }

    private fun configAds() {
        val adjustConfig = AdjustConfig.Builder(ADJUST_TOKEN)
            .eventNamePurchase(application!!.EVENT_PURCHASE_ADJUST)
            .eventAdImpression(EVENT_AD_IMPRESSION_ADJUST)
            .build()
        val vioAdConfig = VioAdConfig.Builder()
            .buildVariantProduce(false)
            .adjustConfig(adjustConfig)
            .mediationProvider(NetworkProvider.ADMOB)
            .listTestDevices(ArrayList())
            .build()
        AdmobFactory.getInstance().initAdmob(this, vioAdConfig)
        initAppOpenAd()
    }

    private fun initBilling() {
        val listPurchaseItem: MutableList<PurchaseItem> = ArrayList()
        listPurchaseItem.add(PurchaseItem("MainActivity.PRODUCT_ID", AppPurchase.TYPE_IAP.PURCHASE))
        AppPurchase.instance!!.initBilling(this, listPurchaseItem)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var application: MyApplication? = null
            private set
    }

    private fun initAppOpenAd(): AppResumeAdHelper {
        val listClassInValid = mutableListOf<Class<*>>()
        listClassInValid.add(AdActivity::class.java)
        val config = AppResumeAdConfig(
            idAds = "ca-app-pub-3940256099942544/9257395921",
            canShowAds = true,
            canReloadAds = true,
            listClassInValid = listClassInValid
        )
        return AppResumeAdHelper(
            application = this,
            lifecycleOwner = ProcessLifecycleOwner.get().lifecycle
        )
    }

}