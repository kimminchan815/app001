package com.example.proximityalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

class LocationTrackingService : Service() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var notificationManager: NotificationManager
    private lateinit var prefs: SharedPreferences
    
    private var destinationLocation: LatLng? = null
    private var alertLevel = 0 // 0: no alert, 1-3: progressive alerts
    private var lastAlertDistance = Float.MAX_VALUE
    
    override fun onCreate() {
        super.onCreate()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        createNotificationChannel()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    checkProximity(location)
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val lat = it.getDoubleExtra("destination_lat", 0.0)
            val lng = it.getDoubleExtra("destination_lng", 0.0)
            destinationLocation = LatLng(lat, lng)
        }
        
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        startLocationUpdates()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000 // 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5000) // 5 seconds
            setMaxUpdateDelayMillis(15000)
        }.build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission not granted
            stopSelf()
        }
    }
    
    private fun checkProximity(currentLocation: Location) {
        destinationLocation?.let { destination ->
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                destination.latitude,
                destination.longitude,
                results
            )
            
            val distance = results[0] // Distance in meters
            
            // Get alert distances from preferences
            val alertDistance1 = prefs.getString("alert_distance_1", "1000")?.toFloatOrNull() ?: 1000f
            val alertDistance2 = prefs.getString("alert_distance_2", "500")?.toFloatOrNull() ?: 500f
            val alertDistance3 = prefs.getString("alert_distance_3", "200")?.toFloatOrNull() ?: 200f
            
            // Check if we should trigger an alert
            when {
                distance <= alertDistance3 && alertLevel < 3 && distance < lastAlertDistance -> {
                    alertLevel = 3
                    lastAlertDistance = distance
                    triggerProximityAlert(3, distance)
                }
                distance <= alertDistance2 && alertLevel < 2 && distance < lastAlertDistance -> {
                    alertLevel = 2
                    lastAlertDistance = distance
                    triggerProximityAlert(2, distance)
                }
                distance <= alertDistance1 && alertLevel < 1 && distance < lastAlertDistance -> {
                    alertLevel = 1
                    lastAlertDistance = distance
                    triggerProximityAlert(1, distance)
                }
            }
            
            // Update foreground notification with distance
            updateForegroundNotification(distance)
        }
    }
    
    private fun triggerProximityAlert(level: Int, distance: Float) {
        // Get volume settings from preferences
        val baseVolume = prefs.getInt("alert_volume", 50) / 100f
        val volume = when (level) {
            1 -> baseVolume * 0.5f
            2 -> baseVolume * 0.75f
            3 -> baseVolume * 1.0f
            else -> baseVolume
        }
        
        // Create notification
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationTitle = when (level) {
            1 -> "목적지 접근 중"
            2 -> "목적지가 가까워지고 있습니다"
            3 -> "곧 목적지에 도착합니다"
            else -> "알림"
        }
        
        val notificationText = "목적지까지 약 ${distance.toInt()}m 남았습니다"
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        
        notificationManager.notify(ALERT_NOTIFICATION_ID + level, notification)
        
        // Vibrate
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationPattern = when (level) {
            1 -> longArrayOf(0, 200, 200, 200)
            2 -> longArrayOf(0, 300, 200, 300)
            3 -> longArrayOf(0, 500, 200, 500, 200, 500)
            else -> longArrayOf(0, 200)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(vibrationPattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationPattern, -1)
        }
        
        // Play sound with volume
        playAlertSound(volume)
    }
    
    private fun playAlertSound(volume: Float) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            val targetVolume = (maxVolume * volume).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(
                AudioManager.STREAM_NOTIFICATION,
                targetVolume,
                0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "위치 추적",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "목적지 추적 서비스"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createForegroundNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("목적지 추적 중")
        .setContentText("목적지까지의 거리를 추적하고 있습니다")
        .setSmallIcon(R.drawable.ic_notification)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
    
    private fun updateForegroundNotification(distance: Float) {
        val distanceText = if (distance >= 1000) {
            "목적지까지 약 %.1fkm".format(distance / 1000)
        } else {
            "목적지까지 약 ${distance.toInt()}m"
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("목적지 추적 중")
            .setContentText(distanceText)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 100
    }
}
