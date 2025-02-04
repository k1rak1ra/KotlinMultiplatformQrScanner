package net.k1ra.kotlin_qr_scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.toPixelMap
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.awt.image.BufferedImage

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

}

actual fun scanImage(
    image: ImageBitmap?,
    onCompletion: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val bufferedImage = image?.toBufferedImage()
        val qrText = bufferedImage?.let { scanQRCode(it) }
        if (qrText != null) {
            onCompletion(qrText)
        } else {
            onFailure("")
        }
    } catch (e: Exception) {
        onFailure("")
    }
}

fun ImageBitmap.toBufferedImage(): BufferedImage {
    val width = this.width
    val height = this.height
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val pixelMap = this.toPixelMap()
    for (x in 0 until width) {
        for (y in 0 until height) {
            bufferedImage.setRGB(x, y, pixelMap[x, y].toArgb())
        }
    }
    return bufferedImage
}

fun Color.toArgb(): Int {
    val alpha = (alpha * 255.0f + 0.5f).toInt() shl 24
    val red = (red * 255.0f + 0.5f).toInt() shl 16
    val green = (green * 255.0f + 0.5f).toInt() shl 8
    val blue = (blue * 255.0f + 0.5f).toInt()
    return alpha or red or green or blue
}

fun scanQRCode(bufferedImage: BufferedImage): String? {
    val source = BufferedImageLuminanceSource(bufferedImage)
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    val reader = MultiFormatReader()
    return try {
        val result = reader.decode(bitmap)
        result.text
    } catch (e: NotFoundException) {
        null
    }
}
