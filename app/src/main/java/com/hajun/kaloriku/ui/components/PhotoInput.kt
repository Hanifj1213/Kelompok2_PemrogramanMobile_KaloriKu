package com.hajun.kaloriku.ui.components

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hajun.kaloriku.util.ImageUtils
import java.io.IOException

class PhotoInputActions(val takePhoto: () -> Unit, val pickGallery: () -> Unit)

@Composable
fun rememberPhotoInput(
    onSelected: (Uri) -> Unit,
    onError: (String) -> Unit
): PhotoInputActions {
    val context = LocalContext.current
    val selected by rememberUpdatedState(onSelected)
    val reportError by rememberUpdatedState(onError)
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingUri?.let(Uri::parse)
        pendingUri = null
        if (success && uri != null) selected(uri)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selected(uri)
    }

    fun launchCamera() {
        try {
            val uri = ImageUtils.createCameraUri(context)
            pendingUri = uri.toString()
            camera.launch(uri)
        } catch (_: SecurityException) {
            pendingUri = null
            showSettings = true
        } catch (_: ActivityNotFoundException) {
            pendingUri = null
            reportError("Aplikasi kamera tidak ditemukan. Pilih foto dari galeri.")
        } catch (_: IOException) {
            pendingUri = null
            reportError("Tidak bisa menyiapkan foto. Periksa ruang penyimpanan perangkat.")
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            reportError("Izin kamera belum diberikan. Kamu tetap bisa memakai Galeri atau Cari makanan.")
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Izin kamera diperlukan") },
            text = { Text("Aktifkan izin Kamera di pengaturan aplikasi untuk mengambil foto. Galeri dan pencarian tetap bisa digunakan tanpa izin kamera.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettings = false
                    try {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    } catch (_: ActivityNotFoundException) {
                        reportError("Buka Pengaturan perangkat > Aplikasi > KaloriKu > Izin > Kamera.")
                    }
                }) { Text("Buka pengaturan") }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Batal") } }
        )
    }

    return PhotoInputActions(
        takePhoto = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val activity = context.findActivity()
            when {
                granted -> launchCamera()
                permissionRequested && activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) -> showSettings = true
                else -> {
                    permissionRequested = true
                    permission.launch(Manifest.permission.CAMERA)
                }
            }
        },
        pickGallery = {
            try {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (_: ActivityNotFoundException) {
                reportError("Pemilih foto tidak tersedia di perangkat ini.")
            } catch (_: SecurityException) {
                reportError("Foto tidak dapat diakses. Coba pilih foto lain.")
            }
        }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
