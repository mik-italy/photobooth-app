package com.photobooth.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.photobooth.app.composer.PhotoStripComposer
import com.photobooth.app.data.AppSettings
import com.photobooth.app.data.SettingsData
import com.photobooth.app.printer.CanonSelphyPrinter
import com.photobooth.app.printer.PrinterState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MainPhotoboothScreen(
    onSetFlashBrightness: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appSettings = remember { AppSettings(context) }
    val printer = remember { CanonSelphyPrinter(context) }

    var settings by remember { mutableStateOf(appSettings.loadSettings()) }
    var screenState by remember { mutableStateOf<PhotoboothScreenState>(PhotoboothScreenState.Idle) }
    var printerState by remember { mutableStateOf(PrinterState.UNKNOWN) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Camera permission check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Toast.makeText(context, "Il permesso fotocamera è necessario per il Photobooth!", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Periodic printer status check when on Idle screen
    LaunchedEffect(screenState, settings.printerIp, settings.printerPort) {
        if (screenState is PhotoboothScreenState.Idle) {
            while (isActive) {
                val res = printer.getPrinterStatus(settings.printerIp, settings.printerPort)
                printerState = res.state
                delay(10000)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = screenState) {
            is PhotoboothScreenState.Idle -> {
                IdleScreen(
                    settings = settings,
                    printerState = printerState,
                    onStartSession = {
                        if (hasCameraPermission) {
                            screenState = PhotoboothScreenState.Countdown(
                                shotIndex = 1,
                                totalShots = settings.shotsPerSession,
                                remainingSeconds = settings.countdownSeconds
                            )
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onOpenSettings = {
                        showSettingsDialog = true
                    }
                )
            }

            is PhotoboothScreenState.Countdown -> {
                CameraCaptureScreen(
                    totalShots = settings.shotsPerSession,
                    countdownSeconds = settings.countdownSeconds,
                    onSetFlashBrightness = onSetFlashBrightness,
                    onAllShotsCaptured = { capturedBitmaps ->
                        screenState = PhotoboothScreenState.Composing
                        scope.launch {
                            val doubleStripBitmap = PhotoStripComposer.createDoubleStrip(
                                photos = capturedBitmaps,
                                settings = settings
                            )
                            screenState = PhotoboothScreenState.Preview(doubleStripBitmap)
                        }
                    },
                    onCancel = {
                        screenState = PhotoboothScreenState.Idle
                    }
                )
            }

            is PhotoboothScreenState.Composing -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFE91E63))
                    Text(
                        text = "Composizione striscia in corso...",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            is PhotoboothScreenState.Preview -> {
                PreviewScreen(
                    composedBitmap = state.composedBitmap,
                    printerState = printerState,
                    onPrintClicked = {
                        val bitmapToPrint = state.composedBitmap
                        val jpegBytes = PhotoStripComposer.toJpegByteArray(bitmapToPrint)

                        if (settings.waitForPrinterFinish) {
                            screenState = PhotoboothScreenState.Printing(
                                remainingSeconds = settings.printTimeoutSeconds,
                                statusMessage = "Invio lavoro alla Canon SELPHY CP1500...",
                                isCompleted = false
                            )

                            scope.launch {
                                val printResult = printer.printDirect(
                                    ip = settings.printerIp,
                                    port = settings.printerPort,
                                    jpegBytes = jpegBytes,
                                    jobName = "Photobooth ${settings.eventTitle}"
                                )

                                if (printResult.isFailure) {
                                    printer.printViaAndroidPrintManager(bitmapToPrint)
                                }

                                printer.waitForPrintCompletion(
                                    ip = settings.printerIp,
                                    port = settings.printerPort,
                                    maxWaitSeconds = settings.printTimeoutSeconds
                                ) { remaining, msg ->
                                    screenState = PhotoboothScreenState.Printing(
                                        remainingSeconds = remaining,
                                        statusMessage = msg,
                                        isCompleted = false
                                    )
                                }

                                screenState = PhotoboothScreenState.Printing(
                                    remainingSeconds = 0,
                                    statusMessage = "Stampa completata!",
                                    isCompleted = true
                                )

                                delay(4500)
                                screenState = PhotoboothScreenState.Idle
                            }
                        } else {
                            scope.launch {
                                printer.printDirect(
                                    ip = settings.printerIp,
                                    port = settings.printerPort,
                                    jpegBytes = jpegBytes
                                )
                                delay(2000)
                                screenState = PhotoboothScreenState.Idle
                            }
                        }
                    },
                    onRetakeClicked = {
                        screenState = PhotoboothScreenState.Countdown(
                            shotIndex = 1,
                            totalShots = settings.shotsPerSession,
                            remainingSeconds = settings.countdownSeconds
                        )
                    },
                    onTimeout = {
                        screenState = PhotoboothScreenState.Idle
                    }
                )
            }

            is PhotoboothScreenState.Printing -> {
                PrintingScreen(
                    remainingSeconds = state.remainingSeconds,
                    totalTimeoutSeconds = settings.printTimeoutSeconds,
                    statusMessage = state.statusMessage,
                    isCompleted = state.isCompleted
                )
            }
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                currentSettings = settings,
                printer = printer,
                onSave = { updated ->
                    settings = updated
                    appSettings.saveSettings(updated)
                    showSettingsDialog = false
                    Toast.makeText(context, "Impostazioni salvate!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    showSettingsDialog = false
                }
            )
        }
    }
}
