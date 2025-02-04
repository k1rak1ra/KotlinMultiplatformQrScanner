package net.k1ra.kotlin_qr_scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDualCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDualWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDuoCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInUltraWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType

private sealed interface CameraAccess {
    data object Undefined : CameraAccess
    data object Denied : CameraAccess
    data object Authorized : CameraAccess
}

private val deviceTypes = listOf(
    AVCaptureDeviceTypeBuiltInWideAngleCamera,
    AVCaptureDeviceTypeBuiltInDualWideCamera,
    AVCaptureDeviceTypeBuiltInDualCamera,
    AVCaptureDeviceTypeBuiltInUltraWideCamera,
    AVCaptureDeviceTypeBuiltInDuoCamera
)

@Composable
actual fun QrCodeScanner(
    modifier: Modifier,
    flashlightOn: Boolean,
    cameraLens: CameraLens,
    onCompletion: (String) -> Unit,
    overlayShape: OverlayShape,
    overlayColor: Color,
    overlayBorderColor: Color,
    customOverlay: (ContentDrawScope.() -> Unit)?,
    permissionDeniedView: @Composable (() -> Unit?)?
) {
    var cameraAccess: CameraAccess by remember { mutableStateOf(CameraAccess.Undefined) }
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> {
                cameraAccess = CameraAccess.Authorized
            }

            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> {
                cameraAccess = CameraAccess.Denied
            }

            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(
                    mediaType = AVMediaTypeVideo
                ) { success ->
                    cameraAccess = if (success) CameraAccess.Authorized else CameraAccess.Denied
                }
            }
        }
    }
    Box(
        modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (cameraAccess) {
            CameraAccess.Undefined -> {
                // Waiting for the user to accept permission
            }

            CameraAccess.Denied -> {
                Text("Camera access denied", color = Color.White)
            }

            CameraAccess.Authorized -> {
                AuthorizedCamera(flashlightOn = flashlightOn, cameraLens, onCompletion, overlayShape, overlayColor, overlayBorderColor, customOverlay)
            }
        }
    }
}

@Composable
private fun BoxScope.AuthorizedCamera(
    flashlightOn: Boolean,
    cameraLens: CameraLens,
    onQrCodeScanned: (String) -> Unit,
    overlayShape: OverlayShape,
    overlayColor: Color,
    overlayBorderColor: Color,
    customOverlay: (ContentDrawScope.() -> Unit)? = null
) {
    val cameraPosition = if (cameraLens == CameraLens.Front) {
        AVCaptureDevicePositionFront
    } else {
        AVCaptureDevicePositionBack
    }
    val camera: AVCaptureDevice? = remember {
        AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = deviceTypes,
            mediaType = AVMediaTypeVideo,
            position = cameraPosition
        ).devices.firstOrNull() as? AVCaptureDevice
    }

    if (camera != null) {
        RealDeviceCamera(
            camera = camera,
            flashlightOn = flashlightOn,
            onQrCodeScanned = onQrCodeScanned
        )
    }

    OverlayView(
        overlayShape = overlayShape,
        overlayColor = overlayColor,
        overlayBorderColor = overlayBorderColor,
        customOverlay = customOverlay
    )
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
