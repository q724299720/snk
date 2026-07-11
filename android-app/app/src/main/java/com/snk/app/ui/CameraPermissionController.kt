package com.snk.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class CameraDenialKind { FIRST, RETRYABLE, PERMANENT }

fun classifyCameraDenial(denialCount: Int, shouldShowRationale: Boolean): CameraDenialKind = when {
    denialCount == 0 -> CameraDenialKind.FIRST
    shouldShowRationale -> CameraDenialKind.RETRYABLE
    else -> CameraDenialKind.PERMANENT
}

data class CameraPermissionController(
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

@Composable
fun rememberCameraPermissionController(
    onGranted: () -> Unit,
    onDenied: (CameraDenialKind) -> Unit,
): CameraPermissionController {
    val context = LocalContext.current
    var denialCount by rememberSaveable { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onGranted()
        } else {
            val rationale = context.findActivity()?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            val kind = classifyCameraDenial(denialCount, rationale)
            denialCount++
            onDenied(kind)
        }
    }
    return remember(context, launcher, denialCount) {
        CameraPermissionController(
            request = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            },
            openSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
