
import org.w3c.files.File
import kotlin.js.Promise

//New code to implement wasm support (c) k1rak1ra 2025
external class Html5Qrcode(elementId: String, configOrVerbosityFlag: Boolean) {
    fun scanFile(imageFile: File, showImage: Boolean) : Promise<JsString>
}