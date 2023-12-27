package com.vapps.module_ads.control.config

/**
 * Created by Vio on 26/11/2023.
 */
class AdjustConfig private constructor(
    private val adjustConfigToken: String,
    private val configEventNamePurchase: String = "",
    private val configEventAdImpression: String = ""
) {
    val adjustToken: String
        get() = adjustConfigToken
    val eventNamePurchase: String
        get() = configEventNamePurchase
    val eventAdImpression: String
        get() = configEventAdImpression

    data class Builder(var adjustToken: String? = null) {
        private var eventNamePurchase: String = ""
        private var eventAdImpression: String = ""

        @JvmOverloads
        fun adjustToken(token: String) = apply { this.adjustToken = token }

        @JvmOverloads
        fun eventNamePurchase(eventName: String) = apply { this.eventNamePurchase = eventName }

        @JvmOverloads
        fun eventAdImpression(eventAd: String) = apply { this.eventAdImpression = eventAd }

        @JvmOverloads
        fun build() = AdjustConfig(adjustToken!!, eventNamePurchase, eventAdImpression)
    }
}