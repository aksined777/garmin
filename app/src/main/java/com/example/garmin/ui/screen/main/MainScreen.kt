package com.example.garmin.ui.screen.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.garmin.R
import com.example.garmin.data.service.BluetoothService
import com.example.garmin.ui.ScreenState
import com.example.garmin.ui.navigation.HomeRoute
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = remember { mutableStateOf("") }
    val colorBack = remember { mutableStateOf(Color.Black) }
    val showDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val bluetoothScanState = rememberPermissionState(
        Manifest.permission.BLUETOOTH_SCAN
    )

    val bluetoothConnectState = rememberPermissionState(
        Manifest.permission.BLUETOOTH_CONNECT
    )
    val accessFineLocationState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    CheckPermission()
    val state by produceState(
        initialValue = viewModel.screenState.collectAsState().value,
        key1 = viewModel.screenState
    ) {
        viewModel.screenState.collect {
            if (it is ScreenState.Error) {
                errorMessage.value = it.errorMessage
                showDialog.value = true
            }
            value = it
        }
    }


    LaunchedEffect(Unit) {
        viewModel.startListeningToService()

        if (context is ComponentActivity) {
            if (viewModel.getCheckNoSleep()) {
                context.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                context.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 100.dp)
            )
        },

        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorBack.value)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 0.dp)
                    .height((LocalConfiguration.current.screenHeightDp * 0.1f).dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_stroke_gear),
                    contentDescription = "secret button",
                    modifier = Modifier

                        .size(30.dp)
                        .clickable {
                            navController.navigate(route = HomeRoute.Setting.route)
                        })

            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((LocalConfiguration.current.screenHeightDp * 0.5f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                if (state == ScreenState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(100.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else if (state is ScreenState.Content) {
                    val data = (state as ScreenState.Content<*>).data as MainViewState
                    colorBack.value =  if (data.info.diff < 0) Color.Red else Color.Black
                    Text(
                        fontSize = 46.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        text = data.info.rate.toString(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        text = "max rate: ${viewModel.getMaxRate()}",
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((LocalConfiguration.current.screenHeightDp * 0.4f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            )
            {

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    onClick = {

                        if (bluetoothConnectState.status == PermissionStatus.Denied(true)
                            || accessFineLocationState.status == PermissionStatus.Denied(true)
                            || bluetoothScanState.status == PermissionStatus.Denied(true)
                        ) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", context.packageName, null)
                            intent.data = uri
                            context.startActivity(intent)
                        } else {
                            val instance = BluetoothService.getServiceInstance()
                            if (instance == null) {
                                context.startService(Intent(context, BluetoothService::class.java))
                            } else {
                                viewModel.startScan()
                            }
                        }
                    },
                    content = { Text(text = stringResource(R.string.app_name)) })
            }

        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CheckPermission() {
    val bluetoothScanState = rememberPermissionState(
        Manifest.permission.BLUETOOTH_SCAN
    )

    val bluetoothConnectState = rememberPermissionState(
        Manifest.permission.BLUETOOTH_CONNECT
    )
    val accessFineLocationState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val accessCoarseLocationState = rememberPermissionState(
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val requestPermission = remember { mutableStateOf(false) }

    val isAllPermission = bluetoothScanState.status.isGranted
            && bluetoothConnectState.status.isGranted
            && accessFineLocationState.status.isGranted
            && accessCoarseLocationState.status.isGranted


    LaunchedEffect(Unit) {
        if (!isAllPermission) {
            requestPermission.value = true
        }
    }
    if (requestPermission.value) {
        var permissionCount = 0
        if (!bluetoothScanState.status.isGranted
            && bluetoothScanState.status == PermissionStatus.Denied(false)
        ) {
            LaunchedEffect(Unit) {
                bluetoothScanState.launchPermissionRequest()
            }
            permissionCount += 1
        }

        if (bluetoothScanState.status.isGranted
            && !accessFineLocationState.status.isGranted
            && accessFineLocationState.status == PermissionStatus.Denied(false)
        ) {
            LaunchedEffect(Unit) {
                accessFineLocationState.launchPermissionRequest()
            }
            permissionCount += 1
        }


        if (accessFineLocationState.status.isGranted
            && !bluetoothConnectState.status.isGranted
            && bluetoothConnectState.status == PermissionStatus.Denied(false)
        ) {
            LaunchedEffect(Unit) {
                bluetoothConnectState.launchPermissionRequest()
            }
            permissionCount += 1
        }


        if (permissionCount > 0) requestPermission.value = true else requestPermission.value =
            false


    }
}