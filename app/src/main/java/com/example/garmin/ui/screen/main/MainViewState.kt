package com.example.garmin.ui.screen.main


data class MainViewState(val info: HealthInfoView)

data class HealthInfoView(
    val rate: Int,
    val diff: Int,
)