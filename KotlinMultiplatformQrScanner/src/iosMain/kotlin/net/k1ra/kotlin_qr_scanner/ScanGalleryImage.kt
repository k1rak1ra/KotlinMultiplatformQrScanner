package net.k1ra.kotlin_qr_scanner

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorAccuracy
import platform.CoreImage.CIDetectorAccuracyHigh
import platform.CoreImage.CIDetectorTypeQRCode
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
actual fun scanImage(image: ImageBitmap?, onCompletion: (String) -> Unit, onFailure: (String) -> Unit) {
    val detector = CIDetector.detectorOfType(
        CIDetectorTypeQRCode,
        context = null,
        options = mapOf(CIDetectorAccuracy to CIDetectorAccuracyHigh)
    )
    val ciImage = CIImage(image?.toUIImage()?.CGImage())
    var qrCodeValue = ""
    val features = detector?.featuresInImage(ciImage)

    val qrCodeFeatures: List<CIQRCodeFeature> = if (features is List<*>) {
        features.filterIsInstance<CIQRCodeFeature>()
    } else {
        emptyList()
    }

    for (feature in qrCodeFeatures) {
        qrCodeValue += feature.messageString ?: ""
    }

    if (qrCodeValue.isNotEmpty()) {
        onCompletion(qrCodeValue)
    } else {
        onFailure("")
    }
}

@OptIn(ExperimentalForeignApi::class)
fun ImageBitmap.toUIImage(): UIImage? {
    val width = this.width
    val height = this.height
    val buffer = IntArray(width * height)

    this.readPixels(buffer)

    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val context = CGBitmapContextCreate(
        data = buffer.refTo(0),
        width = width.toULong(),
        height = height.toULong(),
        bitsPerComponent = 8u,
        bytesPerRow = (4 * width).toULong(),
        space = colorSpace,
        bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
    )

    val cgImage = CGBitmapContextCreateImage(context)
    return cgImage?.let { UIImage.imageWithCGImage(it) }
}