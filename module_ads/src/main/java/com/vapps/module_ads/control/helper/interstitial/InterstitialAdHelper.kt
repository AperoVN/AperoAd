package com.vapps.module_ads.control.helper.interstitial

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.vapps.module_ads.control.admob.AdmobFactory
import com.vapps.module_ads.control.dialog.LoadingDialog
import com.vapps.module_ads.control.helper.AdsHelper
import com.vapps.module_ads.control.helper.interstitial.params.AdInterstitialState
import com.vapps.module_ads.control.helper.interstitial.params.InterstitialAdParam
import com.vapps.module_ads.control.listener.AdCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class InterstitialAdHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val config: InterstitialAdConfig
) : AdsHelper<InterstitialAdConfig, InterstitialAdParam>(activity, lifecycleOwner, config) {
    private val loadingDialog by lazy { LoadingDialog() }
    private val listAdCallback: CopyOnWriteArrayList<AdCallback> = CopyOnWriteArrayList()
    private val adInterstitialState: MutableStateFlow<AdInterstitialState> =
        MutableStateFlow(if (canRequestAds()) AdInterstitialState.None else AdInterstitialState.Fail)
    var interstitialAdValue: InterstitialAd? = null
        private set
    private var requestShowCount = 0

    init {
        registerAdListener(getDefaultCallback())
    }

    override fun requestAds(param: InterstitialAdParam) {
        lifecycleOwner.lifecycleScope.launch {
            if (canRequestAds()) {
                when (param) {
                    is InterstitialAdParam.Request -> {
                        flagActive.compareAndSet(false, true)
                        createInterAds(activity)
                    }

                    is InterstitialAdParam.Show -> {
                        flagActive.compareAndSet(false, true)
                        interstitialAdValue = param.interstitialAd
                        showInterAds(activity)
                    }

                    is InterstitialAdParam.ShowAd -> {
                        flagActive.compareAndSet(false, true)
                        showInterAds(activity)
                    }

                    else -> {

                    }
                }
            }
        }
    }

    private fun showInterAds(activity: Activity) {
        if (config.showByTime != 1) {
            requestShowCount++
        }
        if (requestShowCount % config.showByTime == 0) {
            lifecycleOwner.lifecycleScope.launch {
                showDialogLoading()
                delay(800)
                AdmobFactory.getInstance()
                    .showInterstitial(activity, interstitialAdValue, invokeListenerAdCallback())
            }
        } else if (requestShowCount % config.showByTime ==
            if (config.showByTime <= 2) {
                1
            } else {
                config.showByTime - 1
            }
            && adInterstitialState.value != AdInterstitialState.Loading
        ) {
            requestAds(InterstitialAdParam.Request)
        }
    }

    private fun showDialogLoading() {
        try {
            val transaction: FragmentTransaction =
                (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            val prev = activity.supportFragmentManager.findFragmentByTag(
                LoadingDialog.TAG
            )
            if (prev != null) {
                transaction.remove(prev)
            }
            transaction.addToBackStack(null)
            loadingDialog.show(transaction, LoadingDialog.TAG)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun requestValid(): Boolean {
        val showConfigValid = (config.showByTime == 1 || requestShowCount % config.showByTime ==
                if (config.showByTime <= 2) {
                    1
                } else {
                    config.showByTime - 1
                })
        val valueValid =
            (interstitialAdValue == null && adInterstitialState.value != AdInterstitialState.Loading) || adInterstitialState.value == AdInterstitialState.Showed
        return canRequestAds() && showConfigValid && valueValid
    }

    private fun createInterAds(activity: Activity) {
        if (requestValid()) {
            lifecycleOwner.lifecycleScope.launch {
                adInterstitialState.emit(AdInterstitialState.Loading)
                AdmobFactory.getInstance()
                    .requestInterstitialAds(
                        activity,
                        config.idAds,
                        invokeListenerAdCallback()
                    )
            }
        }
    }

    override fun cancel() {
    }

    private fun getDefaultCallback(): AdCallback {
        return object : AdCallback() {
            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
            }

            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                interstitialAdValue = interstitialAd
            }

            override fun onAdImpression() {
                super.onAdImpression()
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
            }
        }
    }


    fun registerAdListener(adCallback: AdCallback) {
        this.listAdCallback.add(adCallback)
    }

    fun unregisterAdListener(adCallback: AdCallback) {
        this.listAdCallback.remove(adCallback)
    }

    fun unregisterAllAdListener() {
        this.listAdCallback.clear()
    }

    private fun invokeAdListener(action: (adCallback: AdCallback) -> Unit) {
        listAdCallback.forEach(action)
    }

    private fun invokeListenerAdCallback(): AdCallback {
        return object : AdCallback() {
            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                invokeAdListener { it.onAdFailedToLoad(i) }
            }

            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                Log.e(TAG, "onInterstitialLoad: ")
                lifecycleOwner.lifecycleScope.launch {
                    adInterstitialState.emit(AdInterstitialState.Loaded)
                }
                invokeAdListener { it.onInterstitialLoad(interstitialAd) }
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                loadingDialog.dismiss()
                lifecycleOwner.lifecycleScope.launch {
                    adInterstitialState.emit(AdInterstitialState.ShowFail)
                }
            }

            override fun onInterstitialShow() {
                super.onInterstitialShow()
                Log.e(TAG, "onInterstitialShow: ")
                loadingDialog.dismiss()
                lifecycleOwner.lifecycleScope.launch {
                    adInterstitialState.emit(AdInterstitialState.Showed)
                }
                if (config.reloadAd) {
                    requestAds(InterstitialAdParam.Request)
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                invokeAdListener { it.onAdClicked() }
            }

            override fun onAdImpression() {
                super.onAdImpression()
                invokeAdListener { it.onAdImpression() }
            }

        }
    }

    companion object {
        private val TAG = InterstitialAdHelper::class.simpleName
    }
}