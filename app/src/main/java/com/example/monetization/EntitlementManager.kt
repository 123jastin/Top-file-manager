package com.example.monetization

import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class EntitlementManager(private val prefsRepository: UserPreferencesRepository) {

    val isProUser: Flow<Boolean> = prefsRepository.isProUser

    suspend fun upgradeToPro() {
        prefsRepository.setProUser(true)
    }

    suspend fun resetProStatus() {
        prefsRepository.setProUser(false)
    }
}
