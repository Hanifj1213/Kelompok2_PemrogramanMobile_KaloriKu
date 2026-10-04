package com.hajun.kaloriku.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ketukan tanpa riak dan tanpa latar belakang, dipakai untuk area kecil di dalam kartu
 * supaya tidak ada dua lapisan riak yang bertumpuk.
 */
fun Modifier.tapNoRipple(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = null,
        indication = null,
        enabled = enabled,
        onClick = onClick
    )

/** Bentuk bulat penuh untuk bilah kemajuan dan pil. */
fun Modifier.clipPill(): Modifier = this.clip(RoundedCornerShape(50))

/** Bentuk kustom dengan sudut membulat. */
fun Modifier.clipRounded(radius: Dp = 16.dp): Modifier = this.clip(RoundedCornerShape(radius))

/** Bentuk kustom dari objek Shape. */
fun Modifier.clipShape(shape: Shape): Modifier = this.clip(shape)