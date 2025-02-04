package net.k1ra.kotlin_qr_scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import net.k1ra.kotlin_qr_scanner.CameraLens
import net.k1ra.kotlin_qr_scanner.OverlayShape
import net.k1ra.kotlin_qr_scanner.QRCodeComposable

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
    QRCodeComposable(
        modifier = modifier,
        flashlightOn = flashlightOn,
        cameraLens = cameraLens,
        onCompletion = onCompletion,
        overlayShape = overlayShape,
        overlayColor = overlayColor,
        overlayBorderColor = overlayBorderColor,
        customOverlay = customOverlay,
        permissionDeniedView = permissionDeniedView
    )
}





