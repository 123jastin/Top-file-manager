package com.example.monetization

/**
 * AdMob Configuration for Top File Manager app.
 *
 * Requirements:
 * - App ID: ca-app-pub-8155064094205693~8067827700
 * - Production Native Ad Unit ID: ca-app-pub-8155064094205693/1945272485
 * - Test Native Ad Unit ID: ca-app-pub-3940256099942544/2247696110
 */
object AdConfig {
    /**
     * Set to `true` during development/testing to load AdMob test ads.
     * Set to `false` for production release to use actual AdMob Native Ad Unit ID.
     */
    var USE_TEST_ADS = false

    const val APP_ID = "ca-app-pub-8155064094205693~8067827700"
    const val PRODUCTION_NATIVE_AD_UNIT_ID = "ca-app-pub-8155064094205693/1945272485"
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    val NATIVE_AD_UNIT_ID: String
        get() = if (USE_TEST_ADS) TEST_NATIVE_AD_UNIT_ID else PRODUCTION_NATIVE_AD_UNIT_ID
}
