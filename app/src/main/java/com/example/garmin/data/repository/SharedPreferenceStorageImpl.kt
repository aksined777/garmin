package com.example.garmin.data.repository

import android.content.SharedPreferences
import com.example.garmin.domain.repository.SharedPreferenceStorage
import com.example.garmin.util.Constants.COUNT_MAX_RATE
import com.example.garmin.util.Constants.SESSION_PREF_NAME
import javax.inject.Inject
import javax.inject.Named
import androidx.core.content.edit
import com.example.garmin.util.Constants.CHECK_MAKEUP
import com.example.garmin.util.Constants.CHECK_NO_SLEEP
import com.example.garmin.util.Constants.CHECK_VIBRATION

class SharedPreferenceStorageImpl @Inject constructor(
    @Named(SESSION_PREF_NAME) private val preferences: SharedPreferences,

)
    : SharedPreferenceStorage {

    override var maxRate: Int
    get() = preferences.getInt(COUNT_MAX_RATE, 120)
    set(value) {
        preferences.edit {
            putInt(COUNT_MAX_RATE, value)
        }
    }

    override var checkVibration: Boolean
        get() = preferences.getBoolean(CHECK_VIBRATION, false)
        set(value) {
            preferences.edit {
                putBoolean(CHECK_VIBRATION, value)
            }
        }

    override var checkMakeup: Boolean
        get() = preferences.getBoolean(CHECK_MAKEUP, false)
        set(value) {
            preferences.edit {
                putBoolean(CHECK_MAKEUP, value)
            }
        }

    override var checkNoSleep: Boolean
        get() = preferences.getBoolean(CHECK_NO_SLEEP, false)
        set(value) {
            preferences.edit {
                putBoolean(CHECK_NO_SLEEP, value)
            }
        }



}