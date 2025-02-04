package net.k1ra.kotlin_qr_scanner

import Html5Qrcode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import kotlinx.browser.document
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image.Companion.makeFromBitmap
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.FilePropertyBag
import kotlin.js.Promise

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

//New code to implement wasm support (c) k1rak1ra 2025
actual fun scanImage(
    image: ImageBitmap?,
    onCompletion: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    if (image != null) {
        val div = document.createElement("div") as HTMLDivElement
        div.id = "reader"
        document.body?.appendChild(div)

        val qrCodeProcessor = Html5Qrcode(div.id, false)
        val imageFile = File(listOf(image.toByteArray().asJsArray().buffer).toJsArray(), "selectedFile.jpg", FilePropertyBag(type = "image/png"))

        qrCodeProcessor.scanFile(imageFile, false).then {
            document.body?.removeChild(div)
            onCompletion.invoke(it.toString())
            null
        }.catch {
            document.body?.removeChild(div)
            onFailure.invoke(it.toString())
            null
        }


    } else {
        onFailure.invoke("No image found")
    }
}

//New code to implement wasm support (c) k1rak1ra 2025
fun toJsArrayImpl(vararg x: Byte): Uint8Array = js("new Uint8Array(x)")

//New code to implement wasm support (c) k1rak1ra 2025
fun ByteArray.asJsArray(): Uint8Array = toJsArrayImpl(*this)

//New code to implement wasm support (c) k1rak1ra 2025
fun ImageBitmap.toByteArray(): ByteArray {
    val bitmap = this.asSkiaBitmap()
    val image = makeFromBitmap(bitmap)
    val pixelData = image.encodeToData(EncodedImageFormat.PNG)
    return pixelData!!.bytes
}
