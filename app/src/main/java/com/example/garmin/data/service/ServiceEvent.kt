package com.example.garmin.data.service

import com.example.garmin.domain.model.HealthInfo

sealed class ServiceEvent {
    data class DataUpdate(val data: HealthInfo) : ServiceEvent()
    data class Error(val message: String) : ServiceEvent()
    object None : ServiceEvent()
}