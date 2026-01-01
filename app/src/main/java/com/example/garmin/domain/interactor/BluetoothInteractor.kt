package com.example.garmin.domain.interactor

import com.example.garmin.data.service.ServiceEvent
import com.example.garmin.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class BluetoothInteractor @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
){
    fun startScan() =  bluetoothRepository.startScan()

    val stateHeartRateUpdate: StateFlow<ServiceEvent> = bluetoothRepository.stateHeartRateUpdate

}