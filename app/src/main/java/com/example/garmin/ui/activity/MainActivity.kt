package com.example.garmin.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.example.garmin.data.service.BluetoothService
import com.example.garmin.ui.navigation.NavigationGraph
import com.example.garmin.ui.theme.GarminTheme
import dagger.hilt.android.AndroidEntryPoint

// GARMIN HRM-DUAL
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, BluetoothService::class.java))
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            GarminTheme {
                NavigationGraph(navController = navController)
            }
        }
    }

    override fun onDestroy() {
        stopService(Intent(this, BluetoothService::class.java))
        super.onDestroy()
    }

}