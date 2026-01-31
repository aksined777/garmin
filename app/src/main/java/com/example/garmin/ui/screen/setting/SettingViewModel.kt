package com.example.garmin.ui.screen.setting

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.garmin.domain.interactor.BluetoothInteractor
import com.example.garmin.domain.repository.SharedPreferenceStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


@HiltViewModel
class SettingViewModel @Inject constructor(
    private val bluetoothInteractor: BluetoothInteractor,
    private val sharedPreferenceStorage: SharedPreferenceStorage,
) : ViewModel() {

    fun getMaxRate() =
        sharedPreferenceStorage.maxRate

    fun setMaxRate(max: Int) {
        sharedPreferenceStorage.maxRate = max
    }

    fun setCheckVibration(check: Boolean) {
        sharedPreferenceStorage.checkVibration = check
    }

    fun getCheckVibration() = sharedPreferenceStorage.checkVibration
    fun getCheckMakeup() = sharedPreferenceStorage.checkMakeup
    fun getCheckNoSleep() = sharedPreferenceStorage.checkNoSleep
    fun setCheckMakeup (check: Boolean) {
        sharedPreferenceStorage.checkMakeup = check
    }

    fun setCheckNoSleep (check: Boolean) {
        sharedPreferenceStorage.checkNoSleep = check
    }
}