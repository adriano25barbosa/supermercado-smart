package com.example.supermercadosmart.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Preview de câmera reutilizável, usado tanto para captura de foto
 * quanto para leitura de código de barras (via barcodeAnalyzer opcional).
 * Se a câmera falhar ao iniciar, avisa quem chamou pelo onError.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptureReady: (ImageCapture) -> Unit = {},
    barcodeAnalyzer: BarcodeAnalyzer? = null,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageCapture = ImageCapture.Builder().build()
                    onImageCaptureReady(imageCapture)

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()

                    if (barcodeAnalyzer != null) {
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, barcodeAnalyzer)
                            }
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis
                        )
                    } else {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture
                        )
                    }
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Falha ao iniciar câmera: ${e.message}")
                    onError(e.message ?: "Falha ao iniciar a câmera")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSaved: (android.net.Uri) -> Unit,
    onError: (String) -> Unit
) {
    val photoFile = java.io.File(
        context.cacheDir,
        "images/produto_${System.currentTimeMillis()}.jpg"
    )
    photoFile.parentFile?.mkdirs()

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onSaved(android.net.Uri.fromFile(photoFile))
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Erro desconhecido ao capturar foto")
            }
        }
    )
}
