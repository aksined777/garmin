package com.example.garmin.ui.screen.setting

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.garmin.R
import com.example.garmin.ui.view.SwitchView


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SettingScreen(
    navController: NavHostController,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val vibrationCheckedState = remember { mutableStateOf(viewModel.getCheckVibration()) }
    val makeUpCheckedState = remember { mutableStateOf(viewModel.getCheckMakeup()) }
    var error by remember { mutableStateOf(false) }
    val minValue: Int = 30
    val maxValue: Int = 220
    var maxRate by remember { mutableStateOf<String>(viewModel.getMaxRate().toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = maxRate, // The current text to display
            onValueChange = { newText ->
                val filteredText = newText.filter { it.isDigit() }
                maxRate = filteredText

                if (filteredText.isNotEmpty()) {
                    val intValue = filteredText.toIntOrNull()
                    if (intValue != null) {
                        error = intValue !in minValue..maxValue
                        if (!error) {
                            viewModel.setMaxRate(intValue)
                        }
                    } else {
                        error = true
                    }
                } else {
                    error = false
                }
            },
            label = { Text("Enter a number ($minValue - $maxValue)") },
            isError = error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("Optional placeholder") },
            supportingText = {
                if (error) {
                    Text("Value must be between $minValue and $maxValue")
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        SwitchView(
            stringResource(id = R.string.check_vibro), vibrationCheckedState
        ) { check ->
            viewModel.setCheckVibration(check)
        }

        Spacer(modifier = Modifier.height(20.dp))

        SwitchView(
            stringResource(id = R.string.check_makeup), makeUpCheckedState
        ) { check ->
            viewModel.setCheckMakeup(check)
        }
    }

}