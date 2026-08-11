package com.example.monetization

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.example.data.model.FileItem
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface FileListItemEntry {
    data class FileEntry(val file: FileItem) : FileListItemEntry
    data class AdEntry(val slotIndex: Int) : FileListItemEntry
}

class AdsManager(private val context: Context) {

    companion object {
        private const val TAG = "AdsManager"
        var ADS_ENABLED = true

        @Volatile
        private var isInitialized = false

        fun initialize(context: Context) {
            if (!isInitialized) {
                synchronized(this) {
                    if (!isInitialized) {
                        try {
                            MobileAds.initialize(context) { status ->
                                Log.d(TAG, "MobileAds initialized successfully: $status")
                            }
                            isInitialized = true
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to initialize MobileAds", e)
                        }
                    }
                }
            }
        }

        /**
         * Ad Frequency Calculator based on User Requirement:
         * For every 10 files:
         * 1–9 files -> 0 ads
         * 10–19 files -> 1 native ad
         * 20–29 files -> 2 native ads
         * 30–39 files -> 3 native ads
         * Formula: numberOfAds = fileCount / 10
         *
         * Inserts an ad after every 10 files (after file 10, file 20, file 30...).
         * Automatically skips ad slots that failed to load or when ads are disabled.
         */
        fun buildDisplayItems(
            files: List<FileItem>,
            failedSlots: Set<Int>,
            isAdsActive: Boolean
        ): List<FileListItemEntry> {
            val fileCount = files.size
            val numAdsNeeded = fileCount / 10

            if (numAdsNeeded <= 0 || !isAdsActive) {
                return files.map { FileListItemEntry.FileEntry(it) }
            }

            val result = mutableListOf<FileListItemEntry>()
            for (i in files.indices) {
                result.add(FileListItemEntry.FileEntry(files[i]))
                val fileOrdinal = i + 1 // 1-based index of displayed files
                if (fileOrdinal % 10 == 0) {
                    val slotIndex = fileOrdinal / 10
                    if (slotIndex <= numAdsNeeded && !failedSlots.contains(slotIndex)) {
                        result.add(FileListItemEntry.AdEntry(slotIndex))
                    }
                }
            }
            return result
        }
    }

    private val _isAdsActive = MutableStateFlow(ADS_ENABLED)
    val isAdsActive: StateFlow<Boolean> = _isAdsActive.asStateFlow()

    private val adCache = mutableStateMapOf<Int, NativeAd>()
    private val loadingSlotsMap = mutableStateMapOf<Int, Boolean>()
    private val failedSlotsMap = mutableStateMapOf<Int, Boolean>()

    val failedSlots: Set<Int>
        get() = failedSlotsMap.keys.toSet()

    init {
        initialize(context)
    }

    fun disableAdsForPro() {
        _isAdsActive.value = false
        clearAds()
    }

    fun enableAds() {
        _isAdsActive.value = ADS_ENABLED
    }

    fun getAdForSlot(slotIndex: Int): NativeAd? {
        return adCache[slotIndex]
    }

    fun isLoadingSlot(slotIndex: Int): Boolean {
        return loadingSlotsMap[slotIndex] == true
    }

    fun loadAdForSlot(slotIndex: Int) {
        if (!isAdsActive.value) return
        if (adCache.containsKey(slotIndex) || loadingSlotsMap.containsKey(slotIndex) || failedSlotsMap.containsKey(slotIndex)) {
            return
        }

        loadingSlotsMap[slotIndex] = true

        val adUnitId = AdConfig.NATIVE_AD_UNIT_ID
        Log.d(TAG, "Loading native ad for slot $slotIndex using unit ID $adUnitId")

        val adLoader = AdLoader.Builder(context.applicationContext, adUnitId)
            .forNativeAd { nativeAd ->
                loadingSlotsMap.remove(slotIndex)
                adCache[slotIndex] = nativeAd
                Log.d(TAG, "Successfully loaded native ad for slot $slotIndex: ${nativeAd.headline}")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load for slot $slotIndex: ${error.message} (code ${error.code})")
                    loadingSlotsMap.remove(slotIndex)
                    failedSlotsMap[slotIndex] = true
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun clearAds() {
        adCache.values.forEach { it.destroy() }
        adCache.clear()
        loadingSlotsMap.clear()
        failedSlotsMap.clear()
    }
}
