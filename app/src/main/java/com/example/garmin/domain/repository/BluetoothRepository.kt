package com.example.garmin.domain.repository

import com.example.garmin.data.service.ServiceEvent
import kotlinx.coroutines.flow.StateFlow

interface BluetoothRepository {
    val stateHeartRateUpdate: StateFlow<ServiceEvent>
    fun startScan()
}