import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinmultiplatformqrscannerproject.kotlinmultiplatformqrscannerdemo.generated.resources.Res
import kotlinmultiplatformqrscannerproject.kotlinmultiplatformqrscannerdemo.generated.resources.ic_camera_switch
import kotlinmultiplatformqrscannerproject.kotlinmultiplatformqrscannerdemo.generated.resources.ic_gallery_icon
import kotlinmultiplatformqrscannerproject.kotlinmultiplatformqrscannerdemo.generated.resources.ic_rectangle
import kotlinmultiplatformqrscannerproject.kotlinmultiplatformqrscannerdemo.generated.resources.ic_square_border
import kotlinx.coroutines.launch
import net.k1ra.kotlin_qr_scanner.CameraLens
import net.k1ra.kotlin_qr_scanner.OverlayShape
import net.k1ra.kotlin_qr_scanner.QrScanner
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    var qrCodeURL by remember { mutableStateOf("") }
    var flashlightOn by remember { mutableStateOf(false) }
    var openImagePicker by remember { mutableStateOf(value = true) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var overlayShape by remember { mutableStateOf(OverlayShape.Square) }
    var cameraLens by remember { mutableStateOf(CameraLens.Back) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    MaterialTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .background(Color(0xFF1D1C22))
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    QrScanner(
                        modifier = Modifier,
                        flashlightOn = flashlightOn,
                        cameraLens = cameraLens,
                        openImagePicker = openImagePicker,
                        onCompletion = {
                            qrCodeURL = it
                        },
                        imagePickerHandler = {
                            openImagePicker = it
                        },
                        onFailure = {
                            coroutineScope.launch {
                                if (it.isEmpty()) {
                                    snackbarHostState.showSnackbar("Invalid qr code")
                                } else {
                                    snackbarHostState.showSnackbar(it)
                                }
                            }
                        },
                        overlayShape = overlayShape
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 150.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFF9F9F9),
                                shape = RoundedCornerShape(25.dp)
                            )
                            .height(35.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (flashlightOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                "flash",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        if (cameraLens == CameraLens.Front) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(message = "Flash not available in front camera")
                                            }
                                        } else {
                                            flashlightOn = !flashlightOn
                                        }
                                    }
                            )

                            VerticalDivider(
                                modifier = Modifier,
                                thickness = 1.dp,
                                color = Color(0xFFD8D8D8)
                            )

                            Image(
                                painter = painterResource(Res.drawable.ic_camera_switch),
                                "Camera Switch",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        cameraLens = if (cameraLens == CameraLens.Front) {
                                            CameraLens.Back
                                        } else {
                                            flashlightOn = false
                                            CameraLens.Front
                                        }
                                    }
                            )

                            VerticalDivider(
                                modifier = Modifier,
                                thickness = 1.dp,
                                color = Color(0xFFD8D8D8)
                            )

                            Image(
                                painter = painterResource(Res.drawable.ic_gallery_icon),
                                contentDescription = "gallery",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        openImagePicker = true
                                    }
                            )

                            VerticalDivider(
                                modifier = Modifier,
                                thickness = 1.dp,
                                color = Color(0xFFD8D8D8)
                            )

                            Icon(
                                painter = painterResource(Res.drawable.ic_square_border),
                                "Square",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        overlayShape = OverlayShape.Square
                                    }
                            )

                            VerticalDivider(
                                modifier = Modifier,
                                thickness = 1.dp,
                                color = Color(0xFFD8D8D8)
                            )

                            Icon(
                                painter = painterResource(Res.drawable.ic_rectangle),
                                "Rectangle",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        overlayShape = OverlayShape.Rectangle
                                    }
                            )
                        }
                    }
                }

                if (qrCodeURL.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 22.dp)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = qrCodeURL,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .weight(1f),
                            fontSize = 12.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        Icon(
                            Icons.Filled.CopyAll,
                            "CopyAll",
                            modifier = Modifier.size(20.dp).clickable {
                                clipboardManager.setText(AnnotatedString((qrCodeURL)))
                                scope.launch {
                                    snackbarHostState.showSnackbar(message = "Copied")
                                }
                            },
                            tint = Color.White
                        )
                    }
                }

            }
        }
    }
}
