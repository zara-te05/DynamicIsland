package com.example.isladinamica

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isladinamica.service.IslandService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    // --- ESTADOS DE PERMISOS ---
                    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
                    var hasNotificationAccess by remember { mutableStateOf(isNotificationServiceEnabled()) }
                    var isIgnoringBattery by remember {
                        val pm = getSystemService(POWER_SERVICE) as PowerManager
                        mutableStateOf(pm.isIgnoringBatteryOptimizations(packageName))
                    }

                    // Refrescar estados cuando el usuario regresa a la app
                    LaunchedEffect(Unit) {
                        hasOverlay = Settings.canDrawOverlays(context)
                        hasNotificationAccess = isNotificationServiceEnabled()
                        val pm = getSystemService(POWER_SERVICE) as PowerManager
                        isIgnoringBattery = pm.isIgnoringBatteryOptimizations(packageName)
                    }

                    IslandControlScreen(
                        hasOverlay = hasOverlay,
                        hasNotification = hasNotificationAccess,
                        isIgnoringBattery = isIgnoringBattery,
                        onOverlayClick = { openOverlaySettings() },
                        onNotificationClick = { openNotificationSettings() },
                        onBatteryClick = { openBatterySettings() },
                        onAutostartClick = { openAutostartSettings() },
                        // Dentro de MainActivity -> onToggleService
                        onToggleService = { isEnabled ->
                            try {
                                val intent = Intent(this, IslandService::class.java)
                                if (isEnabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(intent)
                                    } else {
                                        startService(intent)
                                    }
                                } else {
                                    stopService(intent)
                                }
                            } catch (e: Exception) {
                                // Esto te dirá en el Logcat exactamente por qué falló el inicio
                                Log.e("ISLA_ERROR", "Error al gestionar el servicio: ${e.message}")
                                Toast.makeText(this, "Error de sistema: Activa los permisos de inicio automático", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // --- LÓGICA DE COMPROBACIÓN ---

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    // --- LÓGICA DE NAVEGACIÓN A AJUSTES ---

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun openNotificationSettings() {
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun openBatterySettings() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openAutostartSettings() {
        try {
            // Intento específico para Xiaomi/MIUI/HyperOS
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Si no es Xiaomi, abrir información de la app para ver permisos manuales
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

// --- INTERFAZ DE USUARIO (COMPOSE) ---

@Composable
fun IslandControlScreen(
    hasOverlay: Boolean,
    hasNotification: Boolean,
    isIgnoringBattery: Boolean,
    onOverlayClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onAutostartClick: () -> Unit,
    onToggleService: (Boolean) -> Unit
) {
    var isServiceRunning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Panel de Control Isla",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configura los permisos para activar la experiencia.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        // SECCIÓN 1: PERMISOS CRÍTICOS
        PermissionHeader("Permisos Obligatorios")
        PermissionCard(
            "Superposición de pantalla",
            "Permite dibujar la isla sobre otras apps.",
            hasOverlay,
            onOverlayClick
        )
        PermissionCard(
            "Acceso a Notificaciones",
            "Permite leer música y mensajes entrantes.",
            hasNotification,
            onNotificationClick
        )

        Spacer(Modifier.height(24.dp))

        // SECCIÓN 2: OPTIMIZACIÓN DE SISTEMA (Especialmente para Xiaomi)
        PermissionHeader("Estabilidad y Fondo")
        PermissionCard(
            "Ignorar Optimización Batería",
            "Evita que el sistema cierre la isla por 'ahorro'.",
            isIgnoringBattery,
            onBatteryClick
        )
        PermissionCard(
            "Inicio Automático / Ventanas",
            "Activa 'Inicio automático' y 'Pop-ups' en Xiaomi.",
            false, // Generalmente no detectable, se deja como guía
            onAutostartClick
        )

        Spacer(Modifier.height(32.dp))

        // INTERRUPTOR MAESTRO
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Estado de la Isla", fontWeight = FontWeight.Bold)
                    Text(
                        if (isServiceRunning) "Servicio Activo" else "Servicio Detenido",
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = isServiceRunning && hasOverlay,
                    onCheckedChange = {
                        if (hasOverlay) {
                            isServiceRunning = it
                            onToggleService(it)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun PermissionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCard(title: String, subtitle: String, granted: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (granted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Text(text = subtitle, fontSize = 12.sp, color = Color.DarkGray)
            }
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (granted) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}