package net.k1ra.kotlin_qr_scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import kotlinx.coroutines.launch
import net.k1ra.kotlin_image_pick_n_crop.CMPImagePickNCropDialog
import net.k1ra.kotlin_image_pick_n_crop.imagecropper.rememberImageCropper

@Composable
fun QrScanner(
    modifier: Modifier,
    flashlightOn: Boolean,
    cameraLens: CameraLens,
    openImagePicker: Boolean,
    onCompletion: (String) -> Unit,
    imagePickerHandler: (Boolean) -> Unit,
    onFailure: (String) -> Unit,
    overlayShape: OverlayShape = OverlayShape.Square,
    overlayColor: Color = Color(0x88000000),
    overlayBorderColor: Color = Color.White,
    customOverlay: (ContentDrawScope.() -> Unit)? = null,
    permissionDeniedView: @Composable (() -> Unit?)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val imageCropper = rememberImageCropper()

    CMPImagePickNCropDialog(
        imageCropper = imageCropper,
        openImagePicker = openImagePicker,
        cropEnable = false,
        showCameraOption = false,
        imagePickerDialogHandler = {
            imagePickerHandler(it)
        },
        selectedImageCallback = {
            coroutineScope.launch {
                scanImage(it, onCompletion, onFailure)
            }
        })

    QrCodeScanner(modifier, flashlightOn, cameraLens, onCompletion, overlayShape, overlayColor, overlayBorderColor, customOverlay, permissionDeniedView)
}

enum class OverlayShape {
    Square, Rectangle
}

enum class CameraLens {
    Front, Back
}