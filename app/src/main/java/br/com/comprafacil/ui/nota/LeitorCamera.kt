package br.com.comprafacil.ui.nota

import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Câmera traseira lendo códigos com o ML Kit. [formatos]: constantes de
 * [Barcode] (QR code da nota, EAN dos produtos). Chama [onCodigo] a cada leitura.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun LeitorCamera(
    formatos: List<Int>,
    lanterna: Boolean,
    onCodigo: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSemLanterna: () -> Unit = {},
) {
    val context = LocalContext.current
    val dono = LocalLifecycleOwner.current
    val aoLer by rememberUpdatedState(onCodigo)
    val previa = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(dono) {
        val executor = Executors.newSingleThreadExecutor()
        val opcoes = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(formatos.first(), *formatos.drop(1).toIntArray())
            .build()
        val leitor = BarcodeScanning.getClient(opcoes)
        val futuro = ProcessCameraProvider.getInstance(context)

        futuro.addListener({
            val provedor = futuro.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previa.surfaceProvider }
            val analise = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analise.setAnalyzer(executor) { quadro ->
                val imagem = quadro.image
                if (imagem == null) {
                    quadro.close()
                    return@setAnalyzer
                }
                val entrada = InputImage.fromMediaImage(imagem, quadro.imageInfo.rotationDegrees)
                leitor.process(entrada)
                    .addOnSuccessListener { codigos ->
                        codigos.firstNotNullOfOrNull { it.rawValue }?.let { aoLer(it) }
                    }
                    .addOnCompleteListener { quadro.close() }
            }
            runCatching {
                provedor.unbindAll()
                camera = provedor.bindToLifecycle(dono, CameraSelector.DEFAULT_BACK_CAMERA, preview, analise)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { futuro.get().unbindAll() }
            leitor.close()
            executor.shutdown()
        }
    }

    LaunchedEffect(camera, lanterna) {
        val c = camera ?: return@LaunchedEffect
        if (c.cameraInfo.hasFlashUnit()) c.cameraControl.enableTorch(lanterna)
        else if (lanterna) onSemLanterna()
    }

    AndroidView(factory = { previa }, modifier = modifier)
}

val FORMATO_QR = listOf(Barcode.FORMAT_QR_CODE)
val FORMATOS_PRODUTO = listOf(
    Barcode.FORMAT_EAN_13,
    Barcode.FORMAT_EAN_8,
    Barcode.FORMAT_UPC_A,
    Barcode.FORMAT_UPC_E,
)
