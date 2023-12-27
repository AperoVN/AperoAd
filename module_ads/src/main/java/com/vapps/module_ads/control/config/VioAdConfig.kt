package com.vapps.module_ads.control.config

import android.app.Application
import android.util.Log


class VioAdConfig private constructor(
    private val application: Application? = null,
    private val isVariantProduce: Boolean = false,
    private val provider: Int = NetworkProvider.ADMOB,
    private val adjustConfig: AdjustConfig? = null,
    private val listDevices: List<String> = arrayListOf(),
    private val disableAdsResumeByAd: Boolean = true
) {
    val myApplication: Application?
        get() = application
    val isBuildVariantProduce: Boolean
        get() = isVariantProduce
    val mediationProvider: Int
        get() = provider
    val adjust: AdjustConfig?
        get() = adjustConfig
    val listTestDevices: List<String>
        get() = listDevices
    val disableAdsResumeByClickAd: Boolean
        get() = this.disableAdsResumeByAd

    companion object {
        private val TAG = VioAdConfig::class.simpleName
    }

    init {
        Log.e(TAG, " : $isBuildVariantProduce $mediationProvider $adjustConfig $listTestDevices ")
    }

    data class Builder(
        var application: Application? = null,
        var isBuildVariantProduce: Boolean = false,
        var mediationProvider: Int = NetworkProvider.ADMOB,
        var adjustConfig: AdjustConfig? = null,
        var listTestDevices: List<String> = arrayListOf(),
        var disableAdsResumeByAd: Boolean = true
    ) {
        @JvmOverloads
        fun application(application: Application) = apply { this.application = application }

        @JvmOverloads
        fun buildVariantProduce(variantProduce: Boolean) =
            apply { this.isBuildVariantProduce = variantProduce }

        @JvmOverloads
        fun mediationProvider(mediation: Int) = apply { this.mediationProvider = mediation }

        @JvmOverloads
        fun adjustConfig(config: AdjustConfig) = apply { this.adjustConfig = config }

        @JvmOverloads
        fun listTestDevices(listDevices: List<String>) =
            apply { this.listTestDevices = listDevices }

        @JvmOverloads
        fun disableAdsResumeByAd(isDisabled: Boolean) =
            apply { this.disableAdsResumeByAd = isDisabled }

        @JvmOverloads
        fun build() =
            VioAdConfig(
                application,
                isBuildVariantProduce,
                mediationProvider,
                adjustConfig,
                listTestDevices,
                disableAdsResumeByAd
            )
    }
}
