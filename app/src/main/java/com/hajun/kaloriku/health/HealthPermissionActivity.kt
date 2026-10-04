package com.hajun.kaloriku.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Halaman penjelasan izin Health Connect. Wajib disediakan aplikasi yang memakai
 * Health Connect supaya pengguna paham data apa yang dibaca dan ditulis.
 */
class HealthPermissionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Rationale()
                }
            }
        }
    }
}

@Composable
private fun Rationale() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Izin Health Connect", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "KaloriKu memakai Health Connect untuk dua hal:\n\n" +
                "1. Membaca jumlah langkah harian agar kamu tahu seberapa aktif hari ini.\n" +
                "2. Menulis data gizi makanan yang kamu catat, supaya riwayat kesehatanmu lengkap " +
                "di satu tempat.\n\n" +
                "Data hanya dibaca dan ditulis atas persetujuanmu, dan bisa dicabut kapan saja " +
                "lewat pengaturan Health Connect.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
