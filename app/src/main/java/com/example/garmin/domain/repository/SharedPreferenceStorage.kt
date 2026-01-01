package com.example.garmin.domain.repository

interface SharedPreferenceStorage {
    var maxRate: Int

    var checkVibration: Boolean
    var checkMakeup: Boolean
}