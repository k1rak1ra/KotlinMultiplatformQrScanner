package net.k1ra.kotlin_qr_scanner

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

actual fun scanImage(
    image: ImageBitmap?,
    onCompletion: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    image?.let {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        val barcodeScanner = BarcodeScanning.getClient(options)

        val bitmap = convertImageBitmapToBitmap(image)

        val imageToProcess =
            InputImage.fromBitmap(bitmap, 0)

        barcodeScanner.process(imageToProcess)
            .addOnSuccessListener { barcodes ->
                var isBardCodeAvailable = false
                barcodes.forEach { barcode ->
                    barcode.rawValue?.let { barcodeValue ->
                        isBardCodeAvailable = true
                        onCompletion(barcodeValue)
                    }
                }
                if (!isBardCodeAvailable) {
                    onFailure("")
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                onFailure(exception.message.toString())
            }
            .addOnCompleteListener {
            }
    }
}

fun convertImageBitmapToBitmap(imageBitmap: ImageBitmap): Bitmap {
    val bitmap = Bitmap.createBitmap(imageBitmap.width, imageBitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    imageBitmap.asAndroidBitmap().let { canvas.drawBitmap(it, 0f, 0f, null) }
    return bitmap
}