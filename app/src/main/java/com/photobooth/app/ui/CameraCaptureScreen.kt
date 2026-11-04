package com.photobooth.app.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraCaptureScreen(
    totalShots: Int = 3,
    countdownSeconds: Int = 3,
    onSetFlashBrightness: (Boolean) -> Unit,
    onAllShotsCaptured: (List<Bitmap>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    var currentShotIndex by remember { mutableIntStateOf(1) } // 1..3
    var countdownValue by remember { mutableIntStateOf(countdownSeconds) }
    var isWhiteFlashActive by remember { mutableStateOf(false) }
    val capturedBitmaps = remember { mutableListOf<Bitmap>() }

    // Start countdown and capture loop
    LaunchedEffect(currentShotIndex) {
        countdownValue = countdownSeconds
        while (countdownValue > 0) {
            delay(1000)
            countdownValue--
        }

        // 1. ACTIVATE SCREEN FLASH AT MAX BRIGHTNESS
        onSetFlashBrightness(true)
        isWhiteFlashActive = true

        // 2. Allow 250ms for screen backlight and auto-exposure to illuminate subject
        delay(250)

        // 3. Capture photo
        imageCapture?.let { capture ->
            capture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotation = image.imageInfo.rotationDegrees
                        val bitmap = imageProxyToBitmap(image, rotation)
                        image.close()

                        capturedBitmaps.add(bitmap)

                        // 4. RESTORE NORMAL BRIGHTNESS
                        onSetFlashBrightness(false)
                        isWhiteFlashActive = false

                        if (currentShotIndex < totalShots) {
                            currentShotIndex++
                        } else {
                            onAllShotsCaptured(capturedBitmaps.toList())
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraCapture", "Capture failed: ${exception.message}", exception)
                        onSetFlashBrightness(false)
                        isWhiteFlashActive = false
                        if (currentShotIndex < totalShots) {
                            currentShotIndex++
                        } else {
                            onAllShotsCaptured(capturedBitmaps.toList())
                        }
                    }
                }
            )
        } ?: run {
            onSetFlashBrightness(false)
            isWhiteFlashActive = false
            if (currentShotIndex < totalShots) {
                currentShotIndex++
            } else {
                onAllShotsCaptured(capturedBitmaps.toList())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onSetFlashBrightness(false)
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // CameraX Preview View (Landscape)
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .setTargetRotation(previewView.display?.rotation ?: 0)
                            .build()

                        imageCapture = capture

                        // Front camera for photobooth selfie experience, fallback to back
                        val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraCapture", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Bar: Shot progress
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0x99000000)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..totalShots) {
                    val isDone = i < currentShotIndex
                    val isCurrent = i == currentShotIndex

                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 18.dp else 14.dp)
                            .background(
                                color = when {
                                    isDone -> Color(0xFF4CAF50)
                                    isCurrent -> Color(0xFFE91E63)
                                    else -> Color.White.copy(alpha = 0.4f)
                                },
                                shape = CircleShape
                            )
                    )
                    if (i < totalShots) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Foto $currentShotIndex di $totalShots",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Countdown Display
        val countdownScale by animateFloatAsState(
            targetValue = if (countdownValue > 0) 1.25f else 0.85f,
            animationSpec = tween(400),
            label = "countdownScale"
        )

        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            if (countdownValue > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x77000000),
                    modifier = Modifier.size(170.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$countdownValue",
                            color = Color(0xFFFFD700),
                            fontSize = 86.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.scale(countdownScale)
                        )
                    }
                }
            } else {
                Text(
                    text = "SORRIDI! 📸",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // INTERNAL SCREEN WHITE FLASH (Covers the entire screen with pure white at 100% brightness)
        AnimatedVisibility(
            visible = isWhiteFlashActive,
            enter = fadeIn(animationSpec = tween(30)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int): Bitmap {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    var bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    if (bitmap == null) {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            image.width,
            image.height,
            null
        )
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 95, out)
        val jpegBytes = out.toByteArray()
        bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    if (rotationDegrees != 0 && bitmap != null) {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        // Mirror horizontally so front selfie camera feels like a mirror
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    return bitmap ?: Bitmap.createBitmap(600, 450, Bitmap.Config.ARGB_8888)
}
