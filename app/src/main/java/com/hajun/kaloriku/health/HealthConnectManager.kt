package com.hajun.kaloriku.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.hajun.kaloriku.data.FoodItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Jembatan ke Health Connect: membaca jumlah langkah hari ini dan
 * menulis data gizi hasil analisis.
 *
 * Kalau Health Connect belum tersedia di perangkat, semua fungsi ini
 * mengembalikan null / false, sehingga aplikasi tetap berjalan normal.
 */
class HealthConnectManager(private val client: HealthConnectClient?) {

    val isAvailable: Boolean get() = client != null

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class)
    )

    /** Jumlah langkah hari ini, atau null kalau tidak tersedia. */
    suspend fun todaySteps(): Long? {
        val healthClient = client ?: return null
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant()
        val end = Instant.now()
        return try {
            val response = healthClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response[StepsRecord.COUNT_TOTAL]
        } catch (_: Exception) {
            null
        }
    }

    /** Menulis makanan yang baru dicatat ke Health Connect. */
    suspend fun writeNutrition(items: List<FoodItem>, mealTime: Instant, mealName: String): Boolean {
        val healthClient = client ?: return false
        return try {
            val record = NutritionRecord(
                startTime = mealTime,
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(mealTime),
                endTime = mealTime.plusSeconds(60),
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(mealTime),
                energy = androidx.health.connect.client.units.Energy.kilocalories(
                    items.sumOf { it.calories }
                ),
                protein = androidx.health.connect.client.units.Mass.grams(items.sumOf { it.proteinG }),
                totalCarbohydrate = androidx.health.connect.client.units.Mass.grams(items.sumOf { it.carbsG }),
                totalFat = androidx.health.connect.client.units.Mass.grams(items.sumOf { it.fatG }),
                name = mealName,
                metadata = Metadata.manualEntry()
            )
            healthClient.insertRecords(listOf(record))
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        /** Mengembalikan null kalau Health Connect tidak terpasang di perangkat. */
        fun create(context: android.content.Context): HealthConnectManager {
            val client = try {
                if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                    HealthConnectClient.getOrCreate(context)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
            return HealthConnectManager(client)
        }
    }
}
