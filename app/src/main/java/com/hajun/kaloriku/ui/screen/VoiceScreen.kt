package com.hajun.kaloriku.ui.screen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hajun.kaloriku.R
import com.hajun.kaloriku.data.sumNutrition
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.VoiceState
import com.hajun.kaloriku.ui.charts.StackedMacroBar
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.KaloriTopBar
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.SecondaryButton
import com.hajun.kaloriku.ui.theme.Green600
import com.hajun.kaloriku.ui.theme.Green700
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole
import java.util.concurrent.atomic.AtomicBoolean

/** Mencatat makan dengan berbicara, misalnya "saya makan nasi goreng dan telur dadar". */
@Composable
fun VoiceScreen(
    viewModel: MainViewModel,
    onResult: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val screenActive = remember { AtomicBoolean(true) }
    val state = viewModel.voiceState
    var lastTranscript by remember { mutableStateOf("") }
    var recognizerUnavailable by remember { mutableStateOf(false) }

    DisposableEffect(screenActive) {
        screenActive.set(true)
        onDispose { screenActive.set(false) }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Results can arrive before ON_START after the recognizer closes; check screen disposal,
        // not the temporary lifecycle pause caused by the recognizer's own activity.
        if (screenActive.get() && result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                lastTranscript = spoken
                viewModel.analyzeVoice(spoken)
            }
        }
    }

    fun startListening() {
        if (!screenActive.get()) return
        recognizerUnavailable = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan makanan yang kamu makan")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            recognizerUnavailable = true
            Toast.makeText(
                context,
                "Pengenal suara tidak tersedia di perangkat ini. Pakai Cari Makanan untuk mencatat manual.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        KaloriTopBar(
            title = "Catat dengan suara",
            subtitle = "Bahasa Indonesia",
            onBack = {
                screenActive.set(false)
                onBack()
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MicHero(
                processing = state is VoiceState.Loading,
                enabled = state !is VoiceState.Loading,
                onStart = { startListening() }
            )

            if (recognizerUnavailable) {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    spacing = 6.dp
                ) {
                    Text("Pengenal suara tidak tersedia", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Pakai Cari Makanan untuk mencatat manual.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            val transcript = when {
                state is VoiceState.Success -> state.transcript
                lastTranscript.isNotBlank() -> lastTranscript
                else -> null
            }
            if (transcript != null) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    spacing = 6.dp
                ) {
                    Text(
                        text = "Yang terdengar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = "\"$transcript\"", style = MaterialTheme.typography.titleSmall)
                }
            }

            when (state) {
                VoiceState.Idle -> {
                    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 10.dp) {
                        Text("Cara pakai", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Tekan tombol mikrofon, lalu sebutkan makananmu dengan jelas. " +
                                "Contoh: \"saya makan nasi goreng dan telur dadar\".",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                listOf(
                                    "nasi goreng dan telur",
                                    "ayam bakar sama lalapan",
                                    "satu piring soto ayam"
                                )
                            ) { example ->
                                Text(
                                    text = example,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                VoiceState.Loading -> {
                    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text(
                                text = "Menghitung kalori dari ucapanmu…",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }

                is VoiceState.Error -> {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = false,
                        spacing = 12.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(
                                icon = R.drawable.ic_error,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                container = Color.White.copy(alpha = 0.45f)
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                    SecondaryButton(
                        text = "Ucapkan ulang",
                        onClick = {
                            viewModel.resetVoice()
                            startListening()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is VoiceState.Success -> {
                    val totals = state.items.sumNutrition()
                    AppCard(modifier = Modifier.fillMaxWidth(), spacing = 12.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ditemukan ${state.items.size} makanan",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${totals.calories.formatWhole()} kkal",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        StackedMacroBar(item = totals)
                        state.items.forEach { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${item.name} · ${item.grams.formatWhole()} g",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${item.calories.formatWhole()} kkal · " +
                                        "P ${item.proteinG.formatDecimal()} · K ${item.carbsG.formatDecimal()} · " +
                                        "L ${item.fatG.formatDecimal()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    PrimaryButton(
                        text = "Gunakan hasil ini",
                        onClick = {
                            if (screenActive.compareAndSet(true, false)) {
                                viewModel.addVoiceResult(state.items)
                                onResult()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryButton(
                        text = "Ucapkan ulang",
                        onClick = {
                            viewModel.resetVoice()
                            startListening()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text(
                text = "Angka dari AI tetap bisa kamu sunting sebelum disimpan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Tombol mikrofon besar dengan gradasi hijau. */
@Composable
private fun MicHero(processing: Boolean, enabled: Boolean, onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(Green600, Green700)))
            .padding(horizontal = 16.dp, vertical = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (processing) 0.32f else 0.18f))
                    .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Mulai bicara", onClick = onStart)
                    .semantics { contentDescription = if (processing) "Menghitung kalori" else "Mulai bicara" },
                contentAlignment = Alignment.Center
            ) {
                if (processing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Text(
                text = if (processing) "Menghitung kalori…" else "Ketuk untuk bicara",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            Text(
                text = if (processing) "AI sedang memperkirakan gizi dari ucapanmu" else "Sebutkan makanan yang baru kamu makan",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

