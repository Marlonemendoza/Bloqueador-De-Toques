package com.example.bloqueadordetoques

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class BloqueadorService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val NOTIFICATION_ID = 102
    private val CHANNEL_ID = "CANAL_BLOQUEADOR_SERVICE"

    companion object {
        var isServiceRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }

        actualizarNotificacion(bloqueoActivo = true)
        bloquearPantallaInvisible()

        return START_STICKY
    }

    private fun bloquearPantallaInvisible() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (overlayView == null) {
            overlayView = View(this).apply {
                setBackgroundColor(Color.TRANSPARENT)

                // Ocultar barras del sistema mediante SystemUiVisibility (Compatible con todas las versiones)
                @Suppress("DEPRECATION")
                systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Oculta botones de navegación
                        or View.SYSTEM_UI_FLAG_FULLSCREEN     // Oculta barra de estado
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) // Modo inmersivo persistente
            }
        }

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

        try {
            if (overlayView?.parent == null) {
                windowManager?.addView(overlayView, params)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun actualizarNotificacion(bloqueoActivo: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bloqueo de pantalla",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        // Dentro de BloqueadorService.kt -> actualizarNotificacion:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bloqueo de pantalla",
                NotificationManager.IMPORTANCE_LOW // IMPORTANTE: Que coincida con la actividad
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, BloqueadorService::class.java).apply {
            if (bloqueoActivo) {
                action = "ACTION_STOP_SERVICE"
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getService(this, 0, notificationIntent, pendingIntentFlags)

        val titulo = if (bloqueoActivo) "Bloqueo Activo 🔒" else "Bloqueador Listo 🔓"
        val texto = if (bloqueoActivo) "Toque aquí para desactivar el bloqueo." else "Toque aquí para activar el bloqueo invisible."

        val bitmapLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_no_touch)

        val estiloImagenGrande = NotificationCompat.BigPictureStyle()
            .bigPicture(bitmapLogo)
            .bigLargeIcon(null as android.graphics.Bitmap?)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setSmallIcon(R.drawable.logo_no_touch)
            .setLargeIcon(bitmapLogo)
            .setContentIntent(pendingIntent)
            .setStyle(estiloImagenGrande)
            .setOngoing(bloqueoActivo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false

        if (overlayView != null && windowManager != null) {
            try {
                if (overlayView?.parent != null) {
                    windowManager?.removeView(overlayView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val rearmarIntent = Intent(this, BloqueadorService::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getService(this, 0, rearmarIntent, pendingIntentFlags)
        val bitmapLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_no_touch)

        val notificationInactiva = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bloqueo Desactivado 🔓")
            .setContentText("Toque aquí para activar el bloqueo invisible.")
            .setSmallIcon(R.drawable.logo_no_touch)
            .setLargeIcon(bitmapLogo)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notificationInactiva)
    }
}