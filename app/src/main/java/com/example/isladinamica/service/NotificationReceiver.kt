package com.example.isladinamica.service

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationReceiver : NotificationListenerService() {

    private var activeMediaController: MediaController? = null

    // 1. RECEPTOR DE ACCIONES: Escucha los clics que vienen desde IslandService
    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.getStringExtra("action")
            Log.d("ISLA_RECEIVER", "Acción recibida de la Isla: $action")

            activeMediaController?.transportControls?.let { controls ->
                when (action) {
                    "PLAY_PAUSE" -> {
                        // Si está reproduciendo, pausa; si no, reproduce.
                        val state = activeMediaController?.playbackState?.state
                        if (state == android.media.session.PlaybackState.STATE_PLAYING) {
                            controls.pause()
                        } else {
                            controls.play()
                        }
                    }
                    "NEXT" -> controls.skipToNext()
                    "PREV" -> controls.skipToPrevious()
                }
            } ?: Log.e("ISLA_RECEIVER", "No hay un controlador de medios activo")
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Registrar el receptor para escuchar a la Isla
        val filter = IntentFilter("CONTROL_MUSIC_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(actionReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(actionReceiver)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // 1. Validaciones iniciales
        val notification = sbn?.notification ?: return
        val extras = notification.extras ?: return
        val pkgName = sbn.packageName

        // Evitamos que la Isla intente procesar su propia notificación (bucle infinito)
        if (pkgName == this.packageName) return

        // 2. CAPTURAR EL CONTROLADOR DE MEDIOS (Token)
        // Usamos el Token para poder controlar la música de la app externa
        val token = extras.getParcelable<android.media.session.MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
        if (token != null) {
            activeMediaController = MediaController(this, token)
        }

        // 3. EXTRAER TEXTOS
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // 4. PROCESAR IMAGEN (Carátula)
        // Obtenemos el Icon y lo convertimos a Bitmap con la función auxiliar que ya tienes
        val iconObject = extras.getParcelable<android.graphics.drawable.Icon>(Notification.EXTRA_LARGE_ICON)
        val bitmap = iconObject?.let { drawableToBitmap(it) }

        // 5. DETECTAR ESTADO DE REPRODUCCIÓN
        // Consultamos el estado real del MediaController
        val isPlaying = activeMediaController?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING

        // 6. ENVIAR A LA ISLA
        if (title.isNotEmpty()) {
            val intent = Intent("UPDATE_ISLAND_NOTIFICATION")
            intent.putExtra("title", title)
            intent.putExtra("text", text)
            intent.putExtra("isPlaying", isPlaying)
            intent.putExtra("packageName", pkgName)

            // IMPORTANTE: Redimensionar antes de enviar.
            // Los Intents fallan si el Bitmap es mayor a 1MB.
            if (bitmap != null) {
                try {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 150, 150, true)
                    intent.putExtra("image", scaledBitmap)
                } catch (e: Exception) {
                    android.util.Log.e("ISLA", "Error al redimensionar imagen: ${e.message}")
                }
            }

            sendBroadcast(intent)
        }
    }

    private fun drawableToBitmap(icon: Icon): Bitmap? {
        return try {
            val drawable = icon.loadDrawable(this) ?: return null
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Opcional: Si se quita la notificación de música, limpiar el controlador
        if (sbn?.packageName == activeMediaController?.packageName) {
            activeMediaController = null
        }
    }
}