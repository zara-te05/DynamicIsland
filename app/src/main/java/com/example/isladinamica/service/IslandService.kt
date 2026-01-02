package com.example.isladinamica.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.isladinamica.UIPill.DynamicPill
import com.example.isladinamica.UIPill.PillUiState
import com.example.isladinamica.UIPill.PillMode
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.example.isladinamica.R

class IslandService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val mViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = mViewModelStore

    // PillState es observable por Compose
    private val pillState = mutableStateOf(PillUiState(PillMode.NORMAL, "Esperando música..."))

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var isIslandShown = false

    // Receptor de datos provenientes del NotificationReceiver
    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "UPDATE_ISLAND_NOTIFICATION") {
                val title = intent.getStringExtra("title") ?: return
                val text = intent.getStringExtra("text") ?: ""
                val isPlaying = intent.getBooleanExtra("isPlaying", false)

                // Nota: getParcelableExtra está deprecated en API 33+, pero se usa así por compatibilidad
                val albumArt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("image", android.graphics.Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra("image")
                }

                // Actualizamos el estado. Compose detecta el cambio y redibuja la imagen.
                pillState.value = pillState.value.copy(
                    text = title,
                    subText = text,
                    isPlaying = isPlaying,
                    albumArt = albumArt
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)

        // 1. Iniciar Foreground Service correctamente
        startMyForegroundService()

        // 2. Registrar el receptor de datos
        val filter = IntentFilter("UPDATE_ISLAND_NOTIFICATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dataReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(dataReceiver, filter)
        }
    }

    private fun startMyForegroundService() {
        val channelId = "ISLAND_CHANNEL"
        val channelName = "Servicio de Isla Dinámica"

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Isla Activa")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // ID 1 es el identificador de la notificación de foreground
        startForeground(1, notification)
    }

    private fun showIsland() {
        if (!Settings.canDrawOverlays(this) || isIslandShown) return

        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 50
            }

            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@IslandService)
                setViewTreeSavedStateRegistryOwner(this@IslandService)
                setViewTreeViewModelStoreOwner(this@IslandService)

                setContent {
                    // Pasamos el estado reactivo
                    DynamicPill(state = pillState.value) { accion ->
                        handleMusicAction(accion)
                    }
                }
            }

            windowManager?.addView(composeView, params)
            isIslandShown = true
        } catch (e: Exception) {
            Log.e("ISLA", "Error al crear ventana: ${e.message}")
        }
    }

    private fun handleMusicAction(action: String) {
        // En lugar de intentar conectar un MediaController aquí (que es inestable),
        // enviamos la orden de vuelta al NotificationReceiver, quien tiene el control real.
        val intent = Intent("CONTROL_MUSIC_ACTION")
        intent.putExtra("action", action)
        sendBroadcast(intent)

        // Feedback visual inmediato (opcional)
        if (action == "PLAY_PAUSE") {
            pillState.value = pillState.value.copy(isPlaying = !pillState.value.isPlaying)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        showIsland()
        return START_STICKY
    }

    override fun onDestroy() {
        if (isIslandShown) {
            windowManager?.removeView(composeView)
            isIslandShown = false
        }
        try { unregisterReceiver(dataReceiver) } catch (e: Exception) {}
        super.onDestroy()
    }
}