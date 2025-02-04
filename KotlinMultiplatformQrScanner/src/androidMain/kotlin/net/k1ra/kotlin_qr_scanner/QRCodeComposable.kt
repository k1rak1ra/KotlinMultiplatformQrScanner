package net.k1ra.kotlin_qr_scanner

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import net.k1ra.kotlin_image_pick_n_crop.permissionsmanager.PermissionCallback
import net.k1ra.kotlin_image_pick_n_crop.permissionsmanager.PermissionCategory
import net.k1ra.kotlin_image_pick_n_crop.permissionsmanager.PermissionState
import net.k1ra.kotlin_image_pick_n_crop.permissionsmanager.createPermissionManager
import net.k1ra.kotlin_qr_scanner.kmpImagePicker.AlertMessageDialog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Composable
fun QRCodeComposable(
    modifier: Modifier,
    flashlightOn: Boolean,
    cameraLens: CameraLens,
    onCompletion: (String) -> Unit,
    overlayShape: OverlayShape,
    overlayColor: Color,
    overlayBorderColor: Color,
    customOverlay: (ContentDrawScope.() -> Unit)? = null,
    permissionDeniedView: @Composable (() -> Unit?)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview by remember { mutableStateOf<Preview?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    var hasCamPermission by remember { mutableStateOf(value = false) }
    var permissionRationalDialog by remember { mutableStateOf(value = false) }
    var launchSetting by remember { mutableStateOf(value = false) }

    val permissionsManager = createPermissionManager(object : PermissionCallback {
        override fun onPermissionResult(
            permissionCategory: PermissionCategory,
            status: PermissionState
        ) {
            when (status) {
                PermissionState.GRANTED -> {
                    hasCamPermission = true
                }

                else -> {
                    hasCamPermission = false
                    permissionRationalDialog = true
                }
            }
        }
    })

    permissionsManager.requestPermission(PermissionCategory.CAMERA)

    if (launchSetting) {
        permissionsManager.openSettings()
        launchSetting = false
    }

    if (permissionRationalDialog) {
        if (permissionDeniedView == null) {
            AlertMessageDialog(title = "Permission Required",
                message = "Please grant permission to access your camera. You can manage permissions in your device settings.",
                positiveButtonText = "Settings",
                negativeButtonText = "Cancel",
                onPositiveClick = {
                    permissionRationalDialog = false
                    launchSetting = true

                },
                onNegativeClick = {
                    permissionRationalDialog = false
                })
        } else {
            permissionDeniedView()
        }
    }

    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
        ProcessCameraProvider.getInstance(context)

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }

    if (hasCamPermission) {

        val lensCamera = if(cameraLens == CameraLens.Front) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        Box(modifier = modifier) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { androidViewContext ->
                    PreviewView(androidViewContext).apply {
                        this.scaleType = PreviewView.ScaleType.FILL_CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                update = { previewView ->
                    val cameraSelector: CameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensCamera)
                        .build()
                    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        preview = Preview.Builder()
                            .build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                        val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { result ->
                                    result?.let { onCompletion(it) }
                                })
                            }

                        val imageCapture = ImageCapture.Builder()
                            .setFlashMode(ImageCapture.FLASH_MODE_ON)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("qr code", e.message ?: "")
                        }

                        if (camera?.cameraInfo?.hasFlashUnit() == true) {
                            camera?.cameraControl?.enableTorch(flashlightOn)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            // Pass the selected overlay shape to OverlayView
            OverlayView(
                overlayShape = overlayShape,
                overlayColor = overlayColor,
                overlayBorderColor = overlayBorderColor,
                customOverlay = customOverlay
            )
        }
    }
}

@Composable
fun OverlayView(
    overlayShape: OverlayShape,
    overlayColor: Color = Color(0x88000000),
    overlayBorderColor: Color = Color.White,
    customOverlay: (ContentDrawScope.() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                if (customOverlay == null) {
                    drawContent()

                    val scanAreaWidth = when (overlayShape) {
                        OverlayShape.Square -> size.width * 0.65f // Square overlay width
                        OverlayShape.Rectangle -> size.width * 0.85f // Rectangle overlay width
                    }
                    val scanAreaHeight = when (overlayShape) {
                        OverlayShape.Square -> scanAreaWidth
                        OverlayShape.Rectangle -> size.height * 0.2f
                    }
                    val left = (size.width - scanAreaWidth) / 2
                    val top = (size.height - scanAreaHeight) / 3

                    drawRect(
                        color = overlayColor,
                        topLeft = Offset.Zero,
                        size = Size(size.width, top)
                    )
                    drawRect(
                        color = overlayColor,
                        topLeft = Offset(left + scanAreaWidth, top),
                        size = Size(size.width - (left + scanAreaWidth), scanAreaHeight)
                    )
                    drawRect(
                        color = overlayColor,
                        topLeft = Offset(0f, top + scanAreaHeight),
                        size = Size(size.width, size.height - (top + scanAreaHeight))
                    )
                    drawRect(
                        color = overlayColor,
                        topLeft = Offset(0f, top),
                        size = Size(left, scanAreaHeight)
                    )

                    val borderStroke = 2.dp.toPx()
                    drawRect(
                        color = overlayBorderColor,
                        topLeft = Offset(left, top),
                        size = Size(scanAreaWidth, scanAreaHeight),
                        style = Stroke(width = borderStroke)
                    )
                } else {
                    customOverlay()
                }
            }
    )
}

