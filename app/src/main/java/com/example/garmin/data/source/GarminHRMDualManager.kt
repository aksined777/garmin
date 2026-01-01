package com.example.garmin.data.source


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.garmin.data.service.ServiceEvent
import com.example.garmin.domain.error.PermissionException
import com.example.garmin.domain.model.HealthInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class GarminHRMDualManager(
    @ApplicationContext private val context: Context,
) {
    val _stateHeartRateUpdate: MutableStateFlow<ServiceEvent> = MutableStateFlow(ServiceEvent.None)
    val stateHeartRateUpdate get() = _stateHeartRateUpdate.asStateFlow()

    companion object {
        const val TAG = "GarminHRMDualManager"

        // UUID для службы монитора сердечного ритма
        val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID =
            UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var heartRateSensorAddress: String? = null
    private var isScanning = false

    private val handler = Handler(Looper.getMainLooper())
    private val scanCallback = GarminScanCallback()

    private fun hasScanPermissions(): Boolean {
        val scanPermission = android.Manifest.permission.BLUETOOTH_SCAN
        val connectPermission = android.Manifest.permission.BLUETOOTH_CONNECT
        val locationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION

        val hasScan = ContextCompat.checkSelfPermission(
            context,
            scanPermission
        ) == PackageManager.PERMISSION_GRANTED
        val hasConnect = ContextCompat.checkSelfPermission(
            context,
            connectPermission
        ) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            locationPermission
        ) == PackageManager.PERMISSION_GRANTED

        val result = hasScan && hasConnect && hasLocation
        if (!result) {
            Log.e(
                TAG,
                "Missing permissions: scan=$hasScan, connect=$hasConnect, location=$hasLocation"
            )
            throw PermissionException()
        }
        return result
    }

    init {
        Log.d(TAG, "GarminHRMDualManager initializing")
        try {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth not supported on this device")
            } else {
                Log.d(TAG, "Bluetooth adapter initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing bluetooth", e)
        }
    }

    /**
     * Проверяет, включен ли Bluetooth на устройстве
     * @return true если Bluetooth включен, false если выключен или не поддерживается
     */
    fun checkBluetooth(): Boolean {
        return try {
            val isEnabled = bluetoothAdapter?.isEnabled ?: false
            Log.d(TAG, "Bluetooth enabled: $isEnabled")
            isEnabled
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in checkBluetooth", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception in checkBluetooth", e)
            false
        }
    }

    /**
     * Проверяет, поддерживает ли устройство Bluetooth LE
     * @return true если поддерживается
     */
    fun isBluetoothLESupported(): Boolean {
        return try {
            val isSupported =
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            Log.d(TAG, "Bluetooth LE supported: $isSupported")
            isSupported
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth LE support", e)
            false
        }
    }

    /**
     * Проверяет, доступен ли Bluetooth адаптер
     * @return true если доступен
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Получает статус Bluetooth
     * @return строку со статусом Bluetooth
     */
    fun getBluetoothStatus(): String {
        return when {
            bluetoothAdapter == null -> "Bluetooth не поддерживается"
            !checkBluetooth() -> "Bluetooth выключен"
            !isBluetoothLESupported() -> "Bluetooth LE не поддерживается"
            else -> "Bluetooth готов к работе"
        }
    }

    fun startScan() {
        Log.d(TAG, "startScan() called")

        // Проверяем поддержку Bluetooth LE
        if (!isBluetoothLESupported()) {
            Log.e(TAG, "Bluetooth LE is not supported on this device")
            return
        }

        if (!hasScanPermissions()) {
            //   onScanStatus?.invoke(false)
            return
        }

        // Проверяем доступность Bluetooth адаптера
        if (!isBluetoothAvailable()) {
            Log.e(TAG, "Bluetooth adapter is not available")
            return
        }

        // Проверяем, включен ли Bluetooth
        if (!checkBluetooth()) {
            Log.e(TAG, "Bluetooth is not enabled. Please enable Bluetooth first.")
            return
        }

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null")
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            if (bluetoothLeScanner == null) {
                Log.e(TAG, "Failed to get BluetoothLeScanner")
                return
            }
        }

        if (isScanning) {
            Log.d(TAG, "Already scanning, stopping first")
            stopScan()
        }

        try {
            // Создаем фильтр для поиска BLE устройств
            val filters = mutableListOf<ScanFilter>()

            // Фильтр по UUID сервиса сердечного ритма
            filters.add(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
                    .build()
            )

            // Настройки сканирования
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()

            Log.d(TAG, "Starting BLE scan with filters: $filters")
            isScanning = true
            //   onScanStatus?.invoke(true)
            bluetoothLeScanner?.startScan(filters, settings, scanCallback)

            // Остановить сканирование через 15 секунд
            handler.postDelayed({
                Log.d(TAG, "Auto-stopping scan after 20 seconds")
                stopScan()
            }, 20000)

            Log.d(TAG, "BLE scan started successfully")

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in startScan. Check permissions.", e)
            isScanning = false
            // onScanStatus?.invoke(false)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException in startScan", e)
            isScanning = false
            //  onScanStatus?.invoke(false)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startScan", e)
            isScanning = false
            //  onScanStatus?.invoke(false)
        }
    }

    // Альтернативный метод для сканирования всех BLE устройств (для отладки)
    fun startScanAllDevices() {
        Log.d(TAG, "startScanAllDevices() called - scanning ALL BLE devices")
        if (!checkBluetooth()) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }

        if (!hasScanPermissions()) {
            //   onScanStatus?.invoke(false)
            return
        }

        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0)
                .build()

            isScanning = true
            //   onScanStatus?.invoke(true)

            // Создаем новый callback для отладки
            val debugCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    val name = device.name ?: "Unknown"
                    val address = device.address
                    val rssi = result.rssi

                    Log.d(TAG, "!!!!BLE Device: '$name' ($address), RSSI: $rssi")

                    // Проверяем сервисы в scanRecord
                    val scanRecord = result.scanRecord
                    val serviceUuids = scanRecord?.serviceUuids
                    if (serviceUuids != null && serviceUuids.isNotEmpty()) {
                        Log.d(TAG, "  Services: $serviceUuids")

                        // Проверяем, содержит ли устройство сервис сердечного ритма
                        if (serviceUuids.contains(ParcelUuid(HEART_RATE_SERVICE_UUID))) {
                            Log.d(TAG, "  ⭐⭐ FOUND HEART RATE MONITOR! ⭐⭐")
                            //       onDeviceFound?.invoke(name, address)
                            stopScan()
                            connectToDevice(device)
                        }
                    }

                    // Также проверяем по имени
                    if (name.contains("HRM", ignoreCase = true) ||
                        name.contains("GARMIN", ignoreCase = true) ||
                        name.contains("DUAL", ignoreCase = true)
                    ) {
                        Log.d(TAG, "  ⭐⭐ POSSIBLE GARMIN DEVICE BY NAME! ⭐⭐")
                        //   onDeviceFound?.invoke(name, address)
                        stopScan()
                        connectToDevice(device)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    super.onScanFailed(errorCode)
                    isScanning = false
                    //    onScanStatus?.invoke(false)
                    Log.e(TAG, "Scan failed with error: $errorCode")
                }
            }

            // Сканируем без фильтров
            bluetoothLeScanner?.startScan(null, settings, debugCallback)

            // Остановить через 20 секунд
            handler.postDelayed({
                Log.d(TAG, "Stopping debug scan")
                bluetoothLeScanner?.stopScan(debugCallback)
                isScanning = false
                //    onScanStatus?.invoke(false)
            }, 20000)

        } catch (e: Exception) {
            Log.e(TAG, "Error in startScanAllDevices", e)
        }
    }

    fun stopScan() {
        Log.d(TAG, "stopScan() called, isScanning: $isScanning")
        try {
            if (isScanning) {
                bluetoothLeScanner?.stopScan(scanCallback)
                isScanning = false
                //  onScanStatus?.invoke(false)
                Log.d(TAG, "Scan stopped successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
        }
    }

    private inner class GarminScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "!!!onScanrResult")
            val device = result.device
            val deviceName = device.name ?: "Unknown"
            val deviceAddress = device.address
            val rssi = result.rssi

            Log.d(TAG, "Found device: '$deviceName' ($deviceAddress), RSSI: $rssi dBm")

            // Проверяем ScanRecord на наличие сервиса сердечного ритма
            val serviceUuids = result.scanRecord?.serviceUuids
            val hasHeartRateService =
                serviceUuids?.contains(ParcelUuid(HEART_RATE_SERVICE_UUID)) == true

            Log.d(TAG, "Has Heart Rate Service: $hasHeartRateService")

            // Ищем Garmin HRM-Dual
            val isLikelyGarminHRM = deviceName.contains("HRM", ignoreCase = true) ||
                    deviceName.contains("GARMIN", ignoreCase = true) ||
                    deviceName.contains("DUAL", ignoreCase = true) ||
                    hasHeartRateService

            if (isLikelyGarminHRM) {
                Log.d(TAG, "⭐⭐ Garmin HRM detected! Name: '$deviceName' ⭐⭐")
                // onDeviceFound?.invoke(deviceName, deviceAddress)

                // Останавливаем сканирование и подключаемся
                stopScan()
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            isScanning = false
            //  onScanStatus?.invoke(false)

            val errorMessage = when (errorCode) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error: $errorCode"
            }

            Log.e(TAG, "Scan failed: $errorMessage")
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectToDevice: ${device.name ?: "Unknown"} (${device.address})")

        try {
            // Отключаем предыдущее соединение, если есть
            gatt?.disconnect()
            gatt?.close()

            // Сохраняем адрес для возможности переподключения
            heartRateSensorAddress = device.address

            // Подключаемся
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            Log.d(TAG, "connectGatt called successfully")

        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "SecurityException in connectToDevice. Check BLUETOOTH_CONNECT permission.",
                e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception in connectToDevice", e)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    //  onConnectionStateChange(true)

                    // Обнаружение сервисов
                    handler.postDelayed({
                        try {
                            Log.d(TAG, "Discovering services...")
                            val success = gatt.discoverServices()
                            Log.d(TAG, "discoverServices() returned: $success")
                        } catch (e: SecurityException) {
                            Log.e(TAG, "SecurityException in discoverServices", e)
                        }
                    }, 100)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server, status: $status")
                    //  onConnectionStateChange(false)

                    // Попытка переподключения через 3 секунды
                    handler.postDelayed({
                        Log.d(TAG, "Attempting to reconnect...")
                        reconnect()
                    }, 3000)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)

            Log.d(TAG, "onServicesDiscovered: status=$status")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered successfully")

                // Логируем все найденные сервисы
                gatt.services?.forEach { service ->
                    Log.d(TAG, "Service UUID: ${service.uuid}")
                    service.characteristics?.forEach { characteristic ->
                        Log.d(
                            TAG,
                            "  Characteristic: ${characteristic.uuid}, properties: ${characteristic.properties}"
                        )
                    }
                }

                // Ищем сервис сердечного ритма
                val heartRateService = gatt.getService(HEART_RATE_SERVICE_UUID)

                if (heartRateService != null) {
                    Log.d(TAG, "Heart Rate Service found")

                    // Ищем характеристику измерения сердечного ритма
                    val heartRateChar = heartRateService.getCharacteristic(
                        HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID
                    )

                    if (heartRateChar != null) {
                        Log.d(TAG, "Heart Rate Measurement Characteristic found")

                        // Включаем уведомления
                        try {
                            // Сначала включаем уведомления на характеристике
                            val enabled = gatt.setCharacteristicNotification(heartRateChar, true)
                            Log.d(TAG, "setCharacteristicNotification returned: $enabled")

                            // Затем настраиваем дескриптор
                            val descriptor =
                                heartRateChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                            if (descriptor != null) {
                                Log.d(TAG, "Descriptor found, writing ENABLE_NOTIFICATION_VALUE")
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            } else {
                                Log.e(TAG, "Descriptor not found!")
                            }

                        } catch (e: SecurityException) {
                            Log.e(TAG, "SecurityException in enabling notifications", e)
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception in enabling notifications", e)
                        }
                    } else {
                        Log.e(TAG, "Heart Rate Measurement Characteristic NOT found!")
                    }
                } else {
                    Log.e(TAG, "Heart Rate Service NOT found!")
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

            Log.d(TAG, "onCharacteristicChanged: ${characteristic.uuid}")

            if (characteristic.uuid == HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID) {
                val heartRate = parseHeartRate(characteristic.value)
                Log.d(TAG, "heartRate = : ${heartRate}")
                //  onHeartRateUpdate(heartRate)
                _stateHeartRateUpdate.value =
                    ServiceEvent.DataUpdate(HealthInfo(rate = heartRate ))
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)

            Log.d(TAG, "onDescriptorWrite: status=$status, descriptor=${descriptor.uuid}")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write successful, notifications enabled")
            } else {
                Log.e(TAG, "Descriptor write failed with status: $status")
            }
        }
    }

    private fun parseHeartRate(data: ByteArray): Int {
        if (data.isEmpty()) {
            Log.w(TAG, "Empty heart rate data")
            return 0
        }

        try {
            Log.d(TAG, "Heart rate data bytes: ${data.joinToString(", ") { it.toString() }}")

            val flag = data[0].toInt() and 0xFF
            Log.d(TAG, "Heart rate flag: $flag")

            val is16Bit = (flag and 0x01) != 0

            return if (is16Bit && data.size >= 3) {
                // 16-bit формат
                val heartRate = ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                Log.d(TAG, "16-bit heart rate: $heartRate")
                heartRate
            } else if (data.size >= 2) {
                // 8-bit формат
                val heartRate = data[1].toInt() and 0xFF
                Log.d(TAG, "8-bit heart rate: $heartRate")
                heartRate
            } else {
                Log.w(TAG, "Invalid data length: ${data.size}")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing heart rate", e)
            return 0
        }
    }

    fun disconnect() {
        Log.d(TAG, "disconnect() called")
        try {
            gatt?.disconnect()
            gatt?.close()
            gatt = null
            heartRateSensorAddress = null
            stopScan()
        } catch (e: Exception) {
            Log.e(TAG, "Error in disconnect", e)
        }
    }

    private fun reconnect() {
        Log.d(TAG, "reconnect() called")
        heartRateSensorAddress?.let { address ->
            try {
                val device = bluetoothAdapter?.getRemoteDevice(address)
                if (device != null) {
                    Log.d(TAG, "Reconnecting to $address")
                    connectToDevice(device)
                } else {
                    Log.e(TAG, "Device not found for address: $address")
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid address format: $address", e)
            }
        }
    }
}
