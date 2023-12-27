package com.vapps.module_ads.control.helper.interstitial

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.vapps.module_ads.control.admob.AdmobFactory
import com.vapps.module_ads.control.dialog.LoadingDialog
import com.vapps.module_ads.control.helper.AdsHelper
import com.vapps.module_ads.control.helper.interstitial.params.AdInterstitialState
import com.vapps.module_ads.control.helper.interstitial.params.InterstitialAdParam
import com.vapps.module_ads.control.listener.AdCallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class InterstitialAdSplashHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val config: InterstitialAdSplashConfig
) : AdsHelper<InterstitialAdSplashConfig, InterstitialAdParam>(activity, lifecycleOwner, config) {
    private val loadingDialog by lazy { LoadingDialog() }
    private val listAdCallback: CopyOnWriteArrayList<AdCallback> = CopyOnWriteArrayList()
    private val adInterstitialState: MutableStateFlow<AdInterstitialState> =
        MutableStateFlow(if (canRequestAds()) AdInterstitialState.None else AdInterstitialState.Fail)
    var interstitialAdValue: InterstitialAd? = null
        private set

    private var requestTimeOutJob: Job? = null
    private var requestDelayJob: Job? = null
    private var showValid = false

    override fun requestAds(param: InterstitialAdParam) {
        lifecycleOwner.lifecycleScope.launch {
            if (canRequestAds()) {
                when (param) {
                    is InterstitialAdParam.Request -> {
                        flagActive.compareAndSet(false, true)
                        if (interstitialAdValue == null) {
                            adInterstitialState.emit(AdInterstitialState.Loading)
                        }
                        createInterAds(activity)
                    }

                    is InterstitialAdParam.Show -> {
                        flagActive.compareAndSet(false, true)
                        interstitialAdValue = param.interstitialAd
                        adInterstitialState.emit(AdInterstitialState.Loaded)
                        showInterAds(activity)
                    }

                    else -> {

                    }
                }
            }
        }
    }

    private fun showInterAds(activity: Activity) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                Log.e(
                    TAG,
                    "showInterAds: ${adInterstitialState.value == AdInterstitialState.Loaded} ${adInterstitialState.value == AdInterstitialState.ShowFail}",
                )

                if (adInterstitialState.value == AdInterstitialState.Loaded || adInterstitialState.value == AdInterstitialState.ShowFail) {
                    requestDelayJob?.cancel()
                    showDialogLoading()
                    delay(800)
                    AdmobFactory.getInstance()
                        .showInterstitial(activity, interstitialAdValue, invokeListenerAdCallback())
                }
            }
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

    private fun createInterAds(activity: Activity) {
        requestTimeOutJob = lifecycleOwner.lifecycleScope.launch {
            AdmobFactory.getInstance()
                .requestInterstitialAds(activity, config.idAds, invokeListenerAdCallback())
            delay(config.timeOut)
            if (interstitialAdValue != null && config.showReady) {
                showInterAds(activity)
            } else {
                invokeAdListener { it.onNextAction() }
                requestTimeOutJob?.cancel()
            }
        }
        requestDelayJob = lifecycleOwner.lifecycleScope.launch {
            delay(config.timeDelay)
            showValid = true
            if (interstitialAdValue != null && config.showReady) {
                showInterAds(activity)
            }
        }

    }

    override fun cancel() {
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
                Log.e(TAG, "onInterstitialLoad")
                interstitialAdValue = interstitialAd
                lifecycleOwner.lifecycleScope.launch {
                    adInterstitialState.emit(AdInterstitialState.Loaded)
                }
                if (showValid && config.showReady) {
                    showInterAds(activity)
                } else {
                    invokeAdListener { it.onInterstitialLoad(interstitialAd) }
                }
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                loadingDialog.dismiss()
                lifecycleOwner.lifecycleScope.launch {
                    adInterstitialState.emit(AdInterstitialState.ShowFail)
                }
                invokeAdListener { it.onAdFailedToShow(adError) }
                if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    invokeAdListener { it.onNextAction() }
                }
            }

            override fun onInterstitialShow() {
                super.onInterstitialShow()
                loadingDialog.dismiss()
                lifecycleOwner.lifecycleScope.launch {
                    adInterstitialState.emit(AdInterstitialState.Showed)
                }
                requestTimeOutJob?.cancel()
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
        private val TAG = InterstitialAdSplashHelper::class.simpleName
    }
}