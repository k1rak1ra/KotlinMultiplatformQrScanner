package net.k1ra.kotlin_qr_scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureFocusModeContinuousAutoFocus
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.focusMode
import platform.AVFoundation.hasTorch
import platform.AVFoundation.isFocusModeSupported
import platform.AVFoundation.torchMode
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CALayer
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class, DelicateCoroutinesApi::class)
@Composable
fun RealDeviceCamera(
    camera: AVCaptureDevice,
    flashlightOn: Boolean,
    onQrCodeScanned: (String) -> Unit,
) {
    val coordinator = remember {
        ScannerCameraCoordinator(
            camera = camera,
            onScanned = onQrCodeScanned
        )
    }
    val screenWidth = CGRectGetWidth(UIScreen.mainScreen.bounds)
    val screenHeight = CGRectGetHeight(UIScreen.mainScreen.bounds)
    val rect = CGRectMake(0.0, 0.0, screenWidth, screenHeight)

    LaunchedEffect(flashlightOn) {
        camera.lockForConfiguration(null)
        if (camera.hasTorch()) {
            camera.torchMode = if (flashlightOn) AVCaptureTorchModeOn else AVCaptureTorchModeOff
        }
        camera.unlockForConfiguration()
    }

    // Configure autofocus
    if (camera.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
        camera.lockForConfiguration(null)
        camera.focusMode = AVCaptureFocusModeContinuousAutoFocus
        camera.unlockForConfiguration()
    }

    DisposableEffect(Unit) {
        val listener = OrientationListener { orientation ->
            coordinator.setCurrentOrientation(orientation)
        }

        listener.register()

        onDispose {
            listener.unregister()
            GlobalScope.launch(Dispatchers.IO) {
                coordinator.captureSession.stopRunning()
            }
        }
    }

    androidx.compose.ui.viewinterop.UIKitView(
        factory = {
            val previewContainer = UIView()
            previewContainer.setFrame(rect)
            coordinator.prepare(previewContainer.layer)
            coordinator.setFrame(previewContainer.frame)
            previewContainer
        },
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black),
        update = {

        }
    )
}

@OptIn(ExperimentalForeignApi::class)
class ScannerCameraCoordinator(
    val camera: AVCaptureDevice,
    val onScanned: (String) -> Unit
) : AVCaptureMetadataOutputObjectsDelegateProtocol, NSObject() {

    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    lateinit var captureSession: AVCaptureSession

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, DelicateCoroutinesApi::class)
    fun prepare(layer: CALayer) {
        captureSession = AVCaptureSession()

        val videoInput = memScoped {
            val error: ObjCObjectVar<NSError?> = alloc<ObjCObjectVar<NSError?>>()
            val videoInput = AVCaptureDeviceInput(device = camera, error = error.ptr)
            if (error.value != null) {
                println("Video input error:- ${error.value}")
                null
            } else {
                videoInput
            }
        }

        if (videoInput != null && captureSession.canAddInput(videoInput)) {
            captureSession.addInput(videoInput)
        } else {
            return
        }

        val metadataOutput = AVCaptureMetadataOutput()

        if (captureSession.canAddOutput(metadataOutput)) {
            captureSession.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(this, queue = dispatch_get_main_queue())
            metadataOutput.metadataObjectTypes = metadataOutput.availableMetadataObjectTypes
        } else {
            return
        }

        previewLayer = AVCaptureVideoPreviewLayer(session = captureSession).also {
            it.frame = layer.bounds
            it.videoGravity = AVLayerVideoGravityResizeAspectFill
            setCurrentOrientation(newOrientation = UIDevice.currentDevice.orientation)
            layer.addSublayer(it)
        }

        GlobalScope.launch(Dispatchers.Default) {
            captureSession.startRunning()
        }
    }

    fun setCurrentOrientation(newOrientation: UIDeviceOrientation) {
        when (newOrientation) {
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationLandscapeRight

            UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationLandscapeLeft

            UIDeviceOrientation.UIDeviceOrientationPortrait ->
                previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationPortrait

            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                previewLayer?.connection?.videoOrientation =
                    AVCaptureVideoOrientationPortraitUpsideDown

            else ->
                previewLayer?.connection?.videoOrientation = AVCaptureVideoOrientationPortrait
        }
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        didOutputMetadataObjects.firstOrNull()?.let { metadataObject ->
            if (metadataObject is AVMetadataMachineReadableCodeObject) {
                metadataObject.stringValue?.let { onFound(it) }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun onFound(code: String) {
        // kSystemSoundID_UserPreferredAlert = 0x00001000
//        AudioServicesPlaySystemSound(0x1000u) // Mail-Sound 1108 wäre der Photo Sound
        if (code.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                onScanned.invoke(code)
            }
        }
    }

    fun setFrame(rect: CValue<CGRect>) {
        previewLayer?.setFrame(rect)
    }
}

@OptIn(ExperimentalForeignApi::class)
class OrientationListener(
    val orientationChanged: (UIDeviceOrientation) -> Unit
) : NSObject() {
    private val notificationName = UIDeviceOrientationDidChangeNotification

    @OptIn(BetaInteropApi::class)
    @Suppress("UNUSED_PARAMETER")
    @ObjCAction
    fun orientationDidChange(arg: NSNotification) {
        orientationChanged(UIDevice.currentDevice.orientation)
    }

    fun register() {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(
                OrientationListener::orientationDidChange.name + ":"
            ),
            name = notificationName,
            `object` = null
        )
    }

    fun unregister() {
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = this,
            name = notificationName,
            `object` = null
        )
    }
}