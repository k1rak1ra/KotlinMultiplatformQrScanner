package net.k1ra.kotlin_qr_scanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.TimeUnit

open class BarcodeAnalyser(
    private val onQrCodeScanned: (result: String?) -> Unit
) : ImageAnalysis.Analyzer {
    private var lastAnalyzedTimeStamp = 0L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimeStamp >= TimeUnit.SECONDS.toMillis(1)) {
            image.image?.let { imageToAnalyze ->
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(options)
                val imageToProcess =
                    InputImage.fromMediaImage(imageToAnalyze, image.imageInfo.rotationDegrees)

                barcodeScanner.process(imageToProcess)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            barcodes.forEach { barcode ->
                                barcode.rawValue?.let { barcodeValue ->
                                    onQrCodeScanned(barcodeValue)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        exception.printStackTrace()
                    }
            }
            lastAnalyzedTimeStamp = currentTimestamp
        }
    }
}