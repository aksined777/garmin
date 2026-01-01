package com.example.garmin.ui.screen.main


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmin.data.service.BluetoothService
import com.example.garmin.data.service.ServiceEvent
import com.example.garmin.domain.interactor.BluetoothInteractor
import com.example.garmin.domain.repository.SharedPreferenceStorage
import com.example.garmin.ui.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothInteractor: BluetoothInteractor,
    private val sharedPreferenceStorage: SharedPreferenceStorage,

    ) : ViewModel() {

    val _heartRate: MutableStateFlow<ServiceEvent> = MutableStateFlow(ServiceEvent.None)
    val heartRate: StateFlow<ServiceEvent> = _heartRate.asStateFlow()

    val screenState: StateFlow<ScreenState<Any>> = combine(heartRate) { _rate ->
        val rate = _rate.first()
        when (rate) {
            is ServiceEvent.DataUpdate -> {
                ScreenState.Content(MainViewState(HealthInfoView((rate).data.rate,
                    sharedPreferenceStorage.maxRate - (rate).data.rate)))
            }

            is ServiceEvent.Error ->{
                ScreenState.Error((rate).message)
            }

            is ServiceEvent.None -> {
                ScreenState.Loading
            }
        }
    }.catch { err ->
        ScreenState.Error(err.message ?: "")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = ScreenState.Loading
    )

    fun startListeningToService() {
        viewModelScope.launch {
            _heartRate.value =ServiceEvent.None
            delay(5000)
            BluetoothService.getServiceInstance()?.serviceEvents?.collect { event ->
                _heartRate.value = event
            }
        }
    }

    fun startScan() {
        bluetoothInteractor.startScan()
    }

    fun getMaxRate() = sharedPreferenceStorage.maxRate

}