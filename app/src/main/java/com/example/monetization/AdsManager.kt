package com.example.monetization

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdsManager(private val context: Context) {

    companion object {
        var ADS_ENABLED = true
        var MOCK_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    }

    private val _isAdsActive = MutableStateFlow(ADS_ENABLED)
    val isAdsActive: StateFlow<Boolean> = _isAdsActive.asStateFlow()

    fun disableAdsForPro() {
        _isAdsActive.value = false
    }

    fun enableAds() {
        _isAdsActive.value = ADS_ENABLED
    }
}
