package com.example.garmin.data.service


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.garmin.R
import com.example.garmin.data.source.GarminHRMDualManager
import com.example.garmin.domain.repository.SharedPreferenceStorage
import com.example.garmin.ui.activity.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class BluetoothService : Service() {

    @Inject
    lateinit var garminSDK: GarminHRMDualManager

    @Inject
    lateinit var sharedPreferenceStorage: SharedPreferenceStorage

    lateinit var serviceEvents: StateFlow<ServiceEvent>

    private var mediaPlayer1: MediaPlayer? = null

    private var time: Long = 0L

    private val vibrator: Vibrator by lazy {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    }

    private val TAG = "BluetoothService"
    val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "bluetooth_service_channel"
        private var instance: BluetoothService? = null
        fun getServiceInstance(): BluetoothService? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        mediaPlayer1 = MediaPlayer.create(this, R.raw.ritm1)
        mediaPlayer1?.isLooping = true
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        garminSDK.startScan()
        serviceEvents = garminSDK.stateHeartRateUpdate
        serviceScope.launch {
            garminSDK.stateHeartRateUpdate.collect { state ->
                if (state is ServiceEvent.Error) {
                    stopSelf()
                } else if (state is ServiceEvent.DataUpdate) {
                    alarm(sharedPreferenceStorage.maxRate - state.data.rate)
                }
            }
        }
        return START_STICKY
    }


    override fun onDestroy() {
        instance = null
        releaseMediaPlayer()
        releaseWakeLock()
        serviceScope.cancel()
        garminSDK.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun alarm(diff: Int) {

        if (System.currentTimeMillis() - time < 2500)
            return

        if (mediaPlayer1 == null)
            return
        if (diff < 0) {
            if (!mediaPlayer1!!.isPlaying) {
                mediaPlayer1?.start()
            }
            startVibration()
            startMakeUp()
        } else {
            if (mediaPlayer1!!.isPlaying) {
                mediaPlayer1?.pause()
                cancelVibration()
            }

        }

        time = System.currentTimeMillis()

    }

    private fun cancelVibration() {
        vibrator.cancel()
    }

    private fun startMakeUp(){
        // OLOLO
    }
    private fun startVibration() {
        if (sharedPreferenceStorage.checkVibration){
            val effect = VibrationEffect.createOneShot(500, 255)
            vibrator.vibrate(effect)
        }
    }

    private fun releaseMediaPlayer() {
        if (mediaPlayer1 == null)
            return
        try {
            if (mediaPlayer1?.isPlaying == true) {
                mediaPlayer1?.stop() // Остановить воспроизведение
            }
            mediaPlayer1?.reset() // Сбросить состояние
            mediaPlayer1?.release() // Освободить системные ресурсы
            mediaPlayer1 = null

        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Garmin::BluetoothServiceWakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L) // 10 часов максимум
        }
        Log.d(TAG, "WakeLock acquired")
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock: ${e.message}")
        }
    }
}



