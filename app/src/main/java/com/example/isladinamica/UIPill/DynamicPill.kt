package com.example.isladinamica.UIPill

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicPill(state: PillUiState, onControlClick: (String) -> Unit) {
    // Estado local para controlar si está expandida o no
    var isExpanded by remember { mutableStateOf(state.mode == PillMode.EXPANDED) }

    // Sincronizamos el estado local si el Service decide cambiar el modo (ej. llega notificación)
    LaunchedEffect(state.mode) {
        isExpanded = state.mode == PillMode.EXPANDED
    }

    // Animaciones de tamaño
    val width by animateDpAsState(
        targetValue = if (isExpanded) 350.dp else 200.dp,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
        label = "width"
    )
    val height by animateDpAsState(
        targetValue = if (isExpanded) 180.dp else 45.dp,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
        label = "height"
    )

    Surface(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(30.dp))
            // CLIC PARA EXPANDIR/CONTRAER
            .clickable { isExpanded = !isExpanded },
        color = Color.Black
    ) {
        if (!isExpanded) {
            // --- DISEÑO NORMAL (COMPACTO) ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AlbumArt(state.albumArt, size = 28.dp)

                Text(
                    text = state.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )

                AudioVisualizer(state.isPlaying)
            }
        } else {
            // --- DISEÑO EXPANDIDO (CONTROLES) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(state.albumArt, size = 60.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = state.text,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Text(
                            text = state.subText,
                            color = Color.Gray,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }

                // Barra de progreso (puedes vincularla al tiempo real después)
                LinearProgressIndicator(
                    progress = 0.4f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.DarkGray
                )

                // BOTONES DE CONTROL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onControlClick("PREV") }) {
                        Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Botón Play/Pause cambia según el estado de reproducción
                    IconButton(
                        onClick = { onControlClick("PLAY_PAUSE") },
                        modifier = Modifier.background(Color.White, RoundedCornerShape(50.dp)).size(45.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black
                        )
                    }

                    IconButton(onClick = { onControlClick("NEXT") }) {
                        Icon(Icons.Default.SkipNext, "Siguiente", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

// --- COMPONENTES DE APOYO ---

@Composable
fun AlbumArt(bitmap: Bitmap?, size: Dp) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Album Art",
            modifier = Modifier.size(size).clip(RoundedCornerShape(size / 4)),
            contentScale = ContentScale.Crop
        )
    } else {
        // Icono por defecto si no hay carátula (ej. icono de música o la app)
        Box(
            modifier = Modifier
                .size(size)
                .background(Color(0xFF222222), RoundedCornerShape(size / 4)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(size / 2))
        }
    }
}

@Composable
fun AudioVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "music_bars")
    Row(
        modifier = Modifier.height(16.dp).width(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val bars = listOf(0.3f, 0.9f, 0.5f, 0.7f)
        bars.forEach { initialHeight ->
            val heightMultiplier by infiniteTransition.animateFloat(
                initialValue = initialHeight,
                targetValue = if (isPlaying) 1f else initialHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(450, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "bar_anim"
            )
            Box(
                Modifier
                    .fillMaxHeight(if (isPlaying) heightMultiplier else initialHeight)
                    .width(3.dp)
                    .background(if (isPlaying) Color(0xFF1DB954) else Color.Gray, RoundedCornerShape(1.dp))
            )
        }
    }
}