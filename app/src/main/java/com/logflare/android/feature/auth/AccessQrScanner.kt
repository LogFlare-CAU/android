package com.logflare.android.feature.auth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.logflare.core.designsystem.v2.Lf
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.logflare.android.R
import java.util.concurrent.Executors

/**
 * Camera preview that reads QR codes, filling its parent (the viewfinder). [onCode] gets a code
 * when it differs from the previous frame's: pointing at a different code after a rejected one
 * works, and the same rejected code is not re-reported every frame (it is again once the camera
 * has looked away). Without the camera permission the viewfinder shows why, with a button to ask
 * again, or to open app settings when Android will no longer show the prompt.
 * Camera failures stay on this screen as text-free logs; they do not throw.
 */
@Composable
fun AccessQrScanner(
    onCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var blockedForGood by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        // After a refusal, no rationale to show means Android will not prompt again.
        val activity = context.findActivity()
        blockedForGood = !ok && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }
    // Back from the settings screen the permission may have been granted.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasCameraPermission(context)) granted = true
    }
    if (!granted) {
        Column(
            modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.login_camera_denied),
                style = Lf.type.secondary,
                color = ScanColors.Muted,
                textAlign = TextAlign.Center,
            )
            ScanOutlinedButton(
                text = stringResource(if (blockedForGood) R.string.login_camera_settings else R.string.login_camera_allow),
                onClick = {
                    if (blockedForGood) context.openAppSettings() else launcher.launch(Manifest.permission.CAMERA)
                },
                height = 44.dp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        return
    }
    val onCodeState = rememberUpdatedState(onCode)
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    val provider = remember { arrayOfNulls<ProcessCameraProvider>(1) }
    DisposableEffect(Unit) {
        onDispose {
            // Stop the frames before the detector goes away.
            provider[0]?.unbindAll()
            executor.shutdown()
            scanner.close()
        }
    }
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val cameraProvider = try {
                    future.get()
                } catch (e: Exception) {
                    Log.w(TAG, "Camera unavailable", e)
                    return@addListener
                }
                provider[0] = cameraProvider
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                var lastRaw: String? = null
                analysis.setAnalyzer(executor) { imageProxy ->
                    val media = imageProxy.image
                    if (media == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    try {
                        val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(input)
                            .addOnSuccessListener { codes ->
                                val raw = codes.firstNotNullOfOrNull { it.rawValue?.takeIf { value -> value.isNotBlank() } }
                                // Listeners run on the main thread, so lastRaw needs no lock.
                                if (raw != lastRaw) {
                                    lastRaw = raw
                                    if (raw != null) onCodeState.value(raw)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } catch (e: Exception) {
                        // The detector was closed under a frame that was already in flight.
                        Log.w(TAG, "Reading a frame failed", e)
                        imageProxy.close()
                    }
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) {
                    Log.w(TAG, "Binding the camera failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier.fillMaxSize(),
        onRelease = { provider[0]?.unbindAll() },
    )
}

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (e: Exception) {
        Log.w(TAG, "Opening app settings failed", e)
    }
}

private const val TAG = "AccessQrScanner"
