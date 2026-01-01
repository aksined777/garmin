package com.example.garmin.ui.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.garmin.domain.interactor.BluetoothInteractor
import com.example.garmin.domain.repository.SharedPreferenceStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import javax.inject.Inject

@OptIn(InternalCoroutinesApi::class)
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val sharedPreferenceStorage: SharedPreferenceStorage,
    private val bluetoothInteractor: BluetoothInteractor,
):ViewModel()
{

}