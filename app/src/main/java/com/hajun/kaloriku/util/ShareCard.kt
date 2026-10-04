package com.hajun.kaloriku.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.hajun.kaloriku.data.DailyGoals
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole
import java.io.File

/** Membagikan ringkasan harian sebagai gambar atau catatan sebagai berkas CSV. */
object ShareCard {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    /**
     * Menggambar kartu ringkasan harian memakai Canvas biasa, tanpa library tambahan.
     * Ukurannya 1080x1350 agar pas untuk dibagikan ke media sosial.
     */
    fun renderSummary(
        context: Context,
        date: String,
        totals: FoodItem,
        target: Int,
        goals: DailyGoals,
        water: Int,
        streak: Int,
        itemCount: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#F5F8F6"))

        val green = Color.parseColor("#10894E")
        val darkText = Color.parseColor("#1B1B1B")
        val muted = Color.parseColor("#5F6B5F")

        val titlePaint = paint(Color.WHITE, 56f, bold = true)
        val labelPaint = paint(muted, 34f)
        val bigPaint = paint(darkText, 96f, bold = true)
        val mediumPaint = paint(darkText, 42f, bold = true)
        val bodyPaint = paint(darkText, 34f)

        // Kepala hijau
        canvas.drawRoundRect(RectF(0f, 0f, WIDTH.toFloat(), 240f), 0f, 0f, paint(green))

        canvas.drawText("KaloriKu", 60f, 110f, titlePaint)
        canvas.drawText(date, 60f, 175f, paint(Color.parseColor("#CDEBD9"), 36f))

        var y = 320f
        canvas.drawText("Kalori hari ini", 60f, y, labelPaint)
        y += 110f
        canvas.drawText("${totals.calories.formatWhole()}", 60f, y, bigPaint)
        val numberWidth = bigPaint.measureText("${totals.calories.formatWhole()}")
        canvas.drawText(" / $target kkal", 60f + numberWidth, y, paint(muted, 40f))

        // Batang kemajuan
        y += 50f
        val trackRect = RectF(60f, y, (WIDTH - 60).toFloat(), y + 26f)
        canvas.drawRoundRect(trackRect, 13f, 13f, paint(Color.parseColor("#D9EDE0")))
        val ratio = (totals.calories / target).coerceIn(0.0, 1.0).toFloat()
        if (ratio > 0f) {
            val fillRect = RectF(60f, y, 60f + (WIDTH - 120) * ratio, y + 26f)
            canvas.drawRoundRect(fillRect, 13f, 13f, paint(if (totals.calories > target) Color.parseColor("#C62828") else green))
        }

        y += 110f
        canvas.drawText("Rincian gizi", 60f, y, mediumPaint)
        y += 70f
        y = drawMacro(canvas, y, "Protein", totals.proteinG, goals.proteinG, bodyPaint, paint(darkText, 34f, bold = true), paint(green))
        y = drawMacro(canvas, y, "Karbohidrat", totals.carbsG, goals.carbsG, bodyPaint, paint(darkText, 34f, bold = true), paint(green))
        y = drawMacro(canvas, y, "Lemak", totals.fatG, goals.fatG, bodyPaint, paint(darkText, 34f, bold = true), paint(green))

        y += 40f
        canvas.drawText("Air minum: $water gelas", 60f, y, bodyPaint)
        y += 60f
        canvas.drawText("Jumlah makanan tercatat: $itemCount", 60f, y, bodyPaint)
        y += 60f
        if (streak > 0) {
            canvas.drawText("Rentetan mencatat: $streak hari", 60f, y, bodyPaint)
        }

        canvas.drawText(
            "Dibuat dengan KaloriKu • nilai gizi berupa perkiraan",
            60f,
            HEIGHT - 70f,
            paint(muted, 28f)
        )
        return bitmap
    }

    private fun drawMacro(
        canvas: Canvas,
        y: Float,
        label: String,
        value: Double,
        target: Int,
        bodyPaint: Paint,
        valuePaint: Paint,
        barPaint: Paint
    ): Float {
        canvas.drawText(label, 60f, y, bodyPaint)
        val text = "${value.formatDecimal()} g / $target g"
        canvas.drawText(text, WIDTH - 60f - bodyPaint.measureText(text), y, valuePaint)
        val barY = y + 18f
        val track = RectF(60f, barY, (WIDTH - 60).toFloat(), barY + 14f)
        canvas.drawRoundRect(track, 7f, 7f, paint(Color.parseColor("#E4EBE6")))
        val ratio = if (target <= 0) 0f else (value / target).coerceIn(0.0, 1.0).toFloat()
        if (ratio > 0f) {
            val fill = RectF(60f, barY, 60f + (WIDTH - 120) * ratio, barY + 14f)
            canvas.drawRoundRect(fill, 7f, 7f, barPaint)
        }
        return barY + 86f
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
        textSize = size
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    private fun paint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    /** Membuka lembar berbagi Android untuk mengirim gambar ringkasan. */
    fun shareBitmap(context: Context, bitmap: Bitmap) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "ringkasan-kaloriku.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Ringkasan kalori harianku dari KaloriKu")
            // clipData wajib supaya izin baca berkas ikut tersampaikan ke aplikasi penerima.
            clipData = android.content.ClipData.newRawUri("ringkasan", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan ringkasan"))
    }

    /** Membuka lembar berbagi Android untuk mengirim berkas CSV. */
    fun shareCsv(context: Context, csv: String, fileName: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(csv, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Catatan KaloriKu")
            clipData = android.content.ClipData.newRawUri("catatan", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Ekspor catatan"))
    }
}
