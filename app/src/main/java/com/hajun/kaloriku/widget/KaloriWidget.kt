package com.hajun.kaloriku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.hajun.kaloriku.KaloriKuApp
import com.hajun.kaloriku.data.CalorieCalculator
import com.hajun.kaloriku.data.Stats
import com.hajun.kaloriku.util.formatWhole
import kotlinx.coroutines.flow.first

/**
 * Widget layar utama: sisa kalori hari ini dan jumlah gelas air.
 * Datanya dibaca langsung dari penyimpanan aplikasi.
 */
class KaloriWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KaloriKuApp
        val repository = app.container.repository

        val entries = repository.entries.first()
        val profile = repository.profile.first()
        val water = repository.waterEntries.first()
        val goals = repository.goals.first()

        val today = java.time.LocalDate.now()
        val target = profile?.let(CalorieCalculator::dailyTarget) ?: CalorieCalculator.DEFAULT_TARGET
        val consumed = Stats.summarize(today, entries).calories
        val remaining = (target - consumed).coerceAtLeast(0.0)
        val glasses = Stats.waterGlasses(today, water)

        provideContent {
            WidgetContent(
                remaining = remaining.formatWhole(),
                target = target.formatWhole(),
                consumed = consumed.formatWhole(),
                glasses = glasses
            )
        }
    }
}

@Composable
private fun WidgetContent(remaining: String, target: String, consumed: String, glasses: Int) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF10894E))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sisa kalori hari ini",
            style = TextStyle(
                color = ColorProvider(Color(0xFFCDEBD9)),
                fontSize = 12.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = remaining,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "dari $target kkal",
            style = TextStyle(color = ColorProvider(Color(0xFFCDEBD9)), fontSize = 12.sp)
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "Masuk $consumed kkal",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp)
            )
        }
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "Air minum: $glasses gelas",
            style = TextStyle(color = ColorProvider(Color(0xFFCDEBD9)), fontSize = 12.sp)
        )
    }
}

class KaloriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KaloriWidget()
}
