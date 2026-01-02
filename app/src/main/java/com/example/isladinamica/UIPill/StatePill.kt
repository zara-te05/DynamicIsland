package com.example.isladinamica.UIPill

import android.graphics.Bitmap

enum class PillMode {
    NORMAL,
    LIQUID_GLASS,
    HIDDEN,
    EXPANDED,
    NOTIFICATION
}

data class PillUiState(
    val mode: PillMode = PillMode.NORMAL,
    val text: String = "",
    val subText: String = "",
    val isPlaying: Boolean = false,
    val albumArt: Bitmap? = null // Nueva propiedad
)