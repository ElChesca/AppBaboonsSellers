package com.baboons.sellers.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.baboons.sellers.MainActivity
import com.baboons.sellers.R
import com.baboons.sellers.data.local.AppDatabase
import com.baboons.sellers.data.local.entities.PosicionPendienteEntity
import com.baboons.sellers.data.remote.BaboonsApi
import com.baboons.sellers.data.remote.PosicionTrackingDto
import com.baboons.sellers.data.remote.TrackingBatchRequest
import com.baboons.sellers.util.TokenStore
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground service que reporta la posición GPS del chofer cada 60s
 * al ERP. Arranca al abrir la app con sesión activa y se detiene en
 * logout. Encola en Room cuando no hay señal y descarga en lote.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var db: AppDatabase
    @Inject lateinit var api: BaboonsApi

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: FusedLocationProviderClient
    private var flushing = false

    companion object {
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIF_ID = 4001
        private const val INTERVAL_MS = 60_000L
        private const val MIN_DISTANCE_M = 25f

        fun start(context: Context) {
            val i = Intent(context, LocationTrackingService::class.java)
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc in result.locations) {
                serviceScope.launch {
                    db.posicionPendienteDao().insertar(
                        PosicionPendienteEntity(
                            lat = loc.latitude,
                            lng = loc.longitude,
                            accuracy = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
                            speed = if (loc.hasSpeed()) (loc.speed * 3.6) else null, // m/s -> km/h
                            ts = loc.time
                        )
                    )
                    flushPendientes()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundNotification()
        startLocationUpdates()
        // Descargar cola pendiente apenas arranca (por si quedaron puntos sin señal)
        serviceScope.launch { flushPendientes() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Si el proceso muere, Android lo recrea y el servicio vuelve solo
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Rastreo de ruta", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Baboons Sellers")
            .setContentText("Rastreando tu recorrido")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_M)
            .setWaitForAccurateLocation(false)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private suspend fun flushPendientes() {
        if (flushing) return
        flushing = true
        try {
            val negocioId = TokenStore.getNegocioId(this)
            if (negocioId <= 0) return
            val pendientes = db.posicionPendienteDao().getPendientes()
            if (pendientes.isEmpty()) return
            val body = TrackingBatchRequest(
                pendientes.map {
                    PosicionTrackingDto(
                        lat = it.lat, lng = it.lng,
                        accuracy = it.accuracy, speed = it.speed, ts = it.ts
                    )
                }
            )
            val resp = api.postTracking(negocioId, body)
            if (resp.isSuccessful) {
                db.posicionPendienteDao().borrar(pendientes)
            }
        } catch (e: Exception) {
            // Sin señal / server caído: la cola queda para el próximo intento
        } finally {
            flushing = false
        }
    }
}
