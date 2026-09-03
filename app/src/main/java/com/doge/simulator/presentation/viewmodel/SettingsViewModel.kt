package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.doge.simulator.data.local.SettingsPrefs
import com.doge.simulator.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPrefs: SettingsPrefs,
    authRepository: AuthRepository,
) : ViewModel() {

    val bgmEnabled: StateFlow<Boolean> = settingsPrefs.bgmEnabled

    /** 문의 시 사용자 식별용. 로그인돼 있으면 Firebase uid. */
    val uid: String? = authRepository.getCurrentUser()?.uid

    fun setBgmEnabled(enabled: Boolean) = settingsPrefs.setBgmEnabled(enabled)
}
