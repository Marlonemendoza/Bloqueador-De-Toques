package com.example.bloqueadordetoques

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch
import java.io.File

class MainActivity : AppCompatActivity() {

    // Canal unificado con el del Servicio
    private val CHANNEL_ID = "CANAL_BLOQUEADOR_SERVICE"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showNotification(this)
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val switchSuperposicion = findViewById<MaterialSwitch>(R.id.switch_superposicion)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            obtenerArchivo("PermisoSuperposicion.txt").writeText("1")
            switchSuperposicion.trackTintList = ColorStateList.valueOf(Color.parseColor("#33B5E5"))
            verificarYProcederConNotificacion()
        } else {
            switchSuperposicion.isChecked = false
            obtenerArchivo("PermisoSuperposicion.txt").writeText("0")
            switchSuperposicion.trackTintList = ColorStateList.valueOf(Color.parseColor("#AAAAAA"))
            Toast.makeText(this, "Permiso de superposición necesario", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        createNotificationChannel(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        activityMain()

        // AUTO-LANZAR: Si al abrir la app ya todo está concedido, enviamos la notificación enseguida
        if (confirmarArchivosTxt()) {
            verificarYProcederConNotificacion()
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bloqueo de pantalla"
            val descriptionText = "Canal del servicio bloqueador"
            val importance = NotificationManager.IMPORTANCE_LOW // Cambiado a LOW para evitar ruidos molestos

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context) {
        val intent = Intent(context, BloqueadorService::class.java)

        if (BloqueadorService.isServiceRunning) {
            intent.action = "ACTION_STOP_SERVICE"
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getService(context, 0, intent, pendingIntentFlags)

        val titulo = if (BloqueadorService.isServiceRunning) "Bloqueo Activo 🔒" else "Bloqueador De Toques"
        val texto = if (BloqueadorService.isServiceRunning) "Toque aquí para desactivar el bloqueo." else "Toque aquí para activar el bloqueo invisible."

        // Agregamos la imagen larga también en la notificación inicial de la actividad
        val bitmapLogo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_no_touch)
        val estiloImagenGrande = NotificationCompat.BigPictureStyle()
            .bigPicture(bitmapLogo)
            .bigLargeIcon(null as android.graphics.Bitmap?)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID) // Uso del mismo canal
            .setSmallIcon(R.drawable.logo_no_touch)
            .setLargeIcon(bitmapLogo)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setStyle(estiloImagenGrande)
            .setOngoing(BloqueadorService.isServiceRunning)
            .setAutoCancel(!BloqueadorService.isServiceRunning)

        try {
            // ID 102 mandatorio para sincronizar con el startForeground del Servicio
            NotificationManagerCompat.from(context).notify(102, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun obtenerArchivo(nombre: String): File {
        return File(filesDir, nombre)
    }

    fun confirmarArchivosTxt(): Boolean {
        val permisoNotificacion = obtenerArchivo("PermisoNotificacion.txt")
        val permisoSuperposicion = obtenerArchivo("PermisoSuperposicion.txt")

        return if (permisoNotificacion.exists() && permisoSuperposicion.exists()) {
            val txtNotif = permisoNotificacion.readText().trim()
            val txtSuper = permisoSuperposicion.readText().trim()
            txtNotif == "1" && txtSuper == "1"
        } else {
            false
        }
    }

    fun activityMain() {
        val switchNotificacion = findViewById<MaterialSwitch>(R.id.switch_notificacion)
        val switchSuperposicion = findViewById<MaterialSwitch>(R.id.switch_superposicion)

        val archivoNotif = obtenerArchivo("PermisoNotificacion.txt")
        val archivoSuper = obtenerArchivo("PermisoSuperposicion.txt")

        val isNotifChecked = archivoNotif.exists() && archivoNotif.readText().trim() == "1"
        val isSuperChecked = archivoSuper.exists() && archivoSuper.readText().trim() == "1" &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))

        switchNotificacion.isChecked = isNotifChecked
        switchSuperposicion.isChecked = isSuperChecked

        switchNotificacion.trackTintList = ColorStateList.valueOf(Color.parseColor(if (isNotifChecked) "#33B5E5" else "#AAAAAA"))
        switchSuperposicion.trackTintList = ColorStateList.valueOf(Color.parseColor(if (isSuperChecked) "#33B5E5" else "#AAAAAA"))

        switchNotificacion.setOnCheckedChangeListener { _, isChecked ->
            val permisoNotificacion = obtenerArchivo("PermisoNotificacion.txt")
            if (isChecked) {
                permisoNotificacion.writeText("1")
                switchNotificacion.trackTintList = ColorStateList.valueOf(Color.parseColor("#33B5E5"))
                verificarYProcederConNotificacion()
            } else {
                permisoNotificacion.writeText("0")
                switchNotificacion.trackTintList = ColorStateList.valueOf(Color.parseColor("#AAAAAA"))
            }
        }

        switchSuperposicion.setOnCheckedChangeListener { _, isChecked ->
            val permisoSuperposicion = obtenerArchivo("PermisoSuperposicion.txt")
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                } else {
                    permisoSuperposicion.writeText("1")
                    switchSuperposicion.trackTintList = ColorStateList.valueOf(Color.parseColor("#33B5E5"))
                    verificarYProcederConNotificacion()
                }
            } else {
                permisoSuperposicion.writeText("0")
                switchSuperposicion.trackTintList = ColorStateList.valueOf(Color.parseColor("#AAAAAA"))
            }
        }
    }

    private fun verificarYProcederConNotificacion() {
        if (confirmarArchivosTxt()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    showNotification(this)
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                showNotification(this)
            }
        }
    }
}