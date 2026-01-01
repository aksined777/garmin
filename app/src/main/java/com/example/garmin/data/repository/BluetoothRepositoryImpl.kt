package com.example.garmin.data.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.telephony.ServiceState
import androidx.annotation.RequiresPermission
import com.example.garmin.data.service.ServiceEvent
import com.example.garmin.data.source.GarminHRMDualManager
import com.example.garmin.domain.repository.BluetoothRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothRepositoryImpl @Inject constructor(
    private val garminHRMDualManager: GarminHRMDualManager,
) : BluetoothRepository {

    override val stateHeartRateUpdate: StateFlow<ServiceEvent> =
        garminHRMDualManager.stateHeartRateUpdate

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun startScan() {
        garminHRMDualManager.startScan()
    }

}