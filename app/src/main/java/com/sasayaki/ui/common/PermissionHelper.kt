package com.sasayaki.ui.common

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sasayaki.service.TextInjectorService

data class PermissionStatus(
    val name: String,
    val description: String,
    val granted: Boolean,
    val actionLabel: String,
    val onRequest: () -> Unit
)

fun checkOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun requestOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun checkAccessibilityPermission(context: Context): Boolean {
    val expectedService = ComponentName(context, TextInjectorService::class.java).flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.split(':').any { it.equals(expectedService, ignoreCase = true) }
}

/**
 * Whether Android is blocking accessibility for this app because it was sideloaded
 * (Android 13+ "restricted settings"). The app-op has no public constant, so an
 * unknown op on an unusual build reads as not blocked.
 */
fun isRestrictedSettingsBlocked(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
    return try {
        val mode = appOps.checkOpNoThrow(OP_ACCESS_RESTRICTED_SETTINGS, Process.myUid(), context.packageName)
        mode == AppOpsManager.MODE_ERRORED || mode == AppOpsManager.MODE_IGNORED
    } catch (_: IllegalArgumentException) {
        false
    }
}

private const val OP_ACCESS_RESTRICTED_SETTINGS = "android:access_restricted_settings"

@Composable
fun rememberRestrictedSettingsBlocked(): Boolean = rememberCheckedOnResume { isRestrictedSettingsBlocked(it) }

fun openAppInfo(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun requestAccessibilityPermission(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@Composable
fun rememberOverlayPermissionState(): PermissionStatus {
    val context = LocalContext.current

    val granted = rememberCheckedOnResume { checkOverlayPermission(it) }

    return PermissionStatus(
        name = "Display over other apps",
        description = "Lets the bubble float above your keyboard.",
        granted = granted,
        actionLabel = "Allow",
        onRequest = { requestOverlayPermission(context) }
    )
}

@Composable
fun rememberAccessibilityPermissionState(): PermissionStatus {
    val context = LocalContext.current

    val granted = rememberCheckedOnResume { checkAccessibilityPermission(it) }

    return PermissionStatus(
        name = "Text insertion",
        description = "Types the result into the field you are using. Turn on Sasayaki under Accessibility.",
        granted = granted,
        actionLabel = "Allow",
        onRequest = { requestAccessibilityPermission(context) }
    )
}

@Composable
fun rememberMicrophonePermissionState(): PermissionStatus {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.RECORD_AUDIO))
    }

    observeOnResume {
        granted = context.hasPermission(Manifest.permission.RECORD_AUDIO)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }

    return PermissionStatus(
        name = "Microphone",
        description = "Records your voice while the bubble is listening.",
        granted = granted,
        actionLabel = "Allow",
        onRequest = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
    )
}

@Composable
fun rememberNotificationPermissionState(): PermissionStatus {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        )
    }

    observeOnResume {
        granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }

    return PermissionStatus(
        name = "Notifications",
        description = "Keeps the bubble running reliably in the background.",
        granted = granted,
        actionLabel = "Allow",
        onRequest = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )
}

@Composable
private fun rememberCheckedOnResume(
    checkPermission: (Context) -> Boolean
): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(checkPermission(context)) }

    observeOnResume {
        granted = checkPermission(context)
    }

    return granted
}

@Composable
private fun observeOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
