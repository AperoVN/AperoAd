package com.vapps.module_ads.control.admob

import androidx.multidex.MultiDexApplication
import com.vapps.module_ads.control.event.FirebaseAnalyticsUtil.init
import com.vapps.module_ads.control.utils.AppUtil.currentTotalRevenue001Ad
import com.vapps.module_ads.control.utils.SharePreferenceUtils.getCurrentTotalRevenue001Ad
import com.vapps.module_ads.control.utils.SharePreferenceUtils.getInstallTime
import com.vapps.module_ads.control.utils.SharePreferenceUtils.setInstallTime

abstract class AdsMultiDexApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (getInstallTime(this) == 0L) {
            setInstallTime(this)
        }
        init(this)
        currentTotalRevenue001Ad = getCurrentTotalRevenue001Ad(this)
    }
}