package com.hajun.kaloriku.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hajun.kaloriku.data.DailyGoals
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole

/** Menampilkan kemajuan protein, karbohidrat, dan lemak terhadap target harian. */
@Composable
fun MacroProgressRow(totals: FoodItem, goals: DailyGoals) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Capaian gizi", style = MaterialTheme.typography.titleMedium)
            MacroProgress(
                label = "Protein",
                value = totals.proteinG,
                target = goals.proteinG.toDouble(),
                color = ProteinColor
            )
            MacroProgress(
                label = "Karbohidrat",
                value = totals.carbsG,
                target = goals.carbsG.toDouble(),
                color = CarbsColor
            )
            MacroProgress(
                label = "Lemak",
                value = totals.fatG,
                target = goals.fatG.toDouble(),
                color = FatColor
            )
        }
    }
}

@Composable
private fun MacroProgress(
    label: String,
    value: Double,
    target: Double,
    color: Color
) {
    val ratio = if (target <= 0.0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
    val over = value > target
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${value.formatDecimal()} / ${target.formatWhole()} g" +
                    if (over) " • berlebih" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (over) MaterialTheme.colorScheme.error else color
        )
    }
}

private val ProteinColor = Color(0xFF1E88E5)
private val CarbsColor = Color(0xFFF9A825)
private val FatColor = Color(0xFF8E24AA)
