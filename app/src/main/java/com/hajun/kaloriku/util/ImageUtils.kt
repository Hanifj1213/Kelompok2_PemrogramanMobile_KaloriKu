package com.hajun.kaloriku.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

object ImageUtils {

    /**
     * Membaca gambar dan mengecilkannya supaya sisi terpanjang maksimal [maxSize] px.
     * Foto kecil lebih cepat diunggah dan sudah cukup untuk dikenali AI.
     */
    fun loadScaledBitmap(context: Context, uri: Uri, maxSize: Int = 1024): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val scale = minOf(1f, maxSize.toFloat() / max(width, height))
            decoder.setTargetSize(
                (width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1)
            )
            // Bitmap hardware tidak bisa di-compress ke JPEG.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    fun toJpeg(bitmap: Bitmap, quality: Int = 85): ByteArray =
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
        }

    /** Membuat Uri file sementara untuk menyimpan hasil foto dari aplikasi kamera. */
    fun createCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
