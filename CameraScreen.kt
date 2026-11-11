package com.autophoto.ai.ui.camera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autophoto.ai.ui.AppViewModelProvider
import com.autophoto.ai.ui.components.CircleIconButton
import com.autophoto.ai.ui.components.GlassPill
import com.autophoto.ai.ui.theme.AppColors
import com.autophoto.ai.ui.theme.StatusBarIcons
import com.autophoto.ai.utils.PermissionUtils
import com.autophoto.ai.utils.awaitCameraProvider
import java.io.File

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onCaptured: (String) -> Unit,
    vm: CameraViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    StatusBarIcons(darkIcons = false)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by vm.state.collectAsStateWithLifecycle()

    // --- Permission kamera ---
    var hasPermission by remember { mutableStateOf(PermissionUtils.hasCameraPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    // --- CameraX ---
    val resolutionSelector = remember {
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()
    }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE // agar bisa di-clip rounded
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolutionSelector)
            .build()
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Bind kamera saat izin diberikan / lensa diganti
    LaunchedEffect(hasPermission, state.lensFacing) {
        if (!hasPermission) return@LaunchedEffect
        val p = context.awaitCameraProvider()
        provider = p
        val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val selector = CameraSelector.Builder().requireLensFacing(state.lensFacing).build()
        try {
            p.unbindAll()
            camera = p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        } catch (e: Exception) {
            Toast.makeText(context, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(state.flashMode) { imageCapture.flashMode = state.flashMode }
    LaunchedEffect(camera, state.zoom) {
        camera?.let { c ->
            val zs = c.cameraInfo.zoomState.value
            c.cameraControl.setZoomRatio(state.zoom.coerceIn(zs?.minZoomRatio ?: 1f, zs?.maxZoomRatio ?: 1f))
        }
    }
    // Lepas kamera saat layar ditinggalkan (mis. pindah ke layar Edit)
    DisposableEffect(Unit) { onDispose { provider?.unbindAll() } }

    fun capture() {
        if (state.isCapturing || camera == null) return
        vm.setCapturing(true)
        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    vm.setCapturing(false)
                    onCaptured(file.name)
                }

                override fun onError(exc: ImageCaptureException) {
                    vm.setCapturing(false)
                    Toast.makeText(context, "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // --- UI ---
    Box(Modifier.fillMaxSize().background(AppColors.Dark)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(Icons.Rounded.ArrowBack, onBack, contentDescription = "Kembali")
                GlassPill("AUTO CROP · ZOOM · FILTER", icon = Icons.Rounded.AutoAwesome)
                CircleIconButton(Icons.Rounded.FlipCameraAndroid, vm::flipLens, contentDescription = "Balik kamera")
            }

            // Preview 3:4 dengan sudut membulat
            Box(
                Modifier.padding(horizontal = 12.dp).fillMaxWidth().aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(32.dp)).background(Color.Black)
            ) {
                if (hasPermission) {
                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                    GlassPill(
                        "Subjek dideteksi otomatis",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                    )
                } else {
                    PermissionPrompt(
                        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", context.packageName, null))
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Panel kontrol bawah: flash | shutter | zoom
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val flashIcon = when (state.flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto
                    else -> Icons.Rounded.FlashOff
                }
                CircleIconButton(
                    flashIcon, vm::cycleFlash, size = 56.dp,
                    container = AppColors.DarkPanel, contentDescription = "Flash"
                )
                ShutterButton(isCapturing = state.isCapturing, onClick = ::capture)
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(AppColors.DarkPanel)
                        .clickable { vm.toggleZoom() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("${state.zoom.toInt()}x", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** Tombol shutter: cincin gelap + lingkaran putih (seperti referensi). */
@Composable
private fun ShutterButton(isCapturing: Boolean, onClick: () -> Unit) {
    val inner by animateDpAsState(if (isCapturing) 54.dp else 68.dp, label = "shutter")
    Box(
        Modifier.size(88.dp).clip(CircleShape).background(AppColors.DarkPanel).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Box(Modifier.size(inner).clip(CircleShape).background(Color.White)) }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Izin kamera dibutuhkan", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(8.dp))
        Text(
            "AutoPhoto AI memakai kamera untuk mengambil foto.",
            color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)) {
            Text("Beri Izin")
        }
        TextButton(onClick = onOpenSettings) { Text("Buka Pengaturan", color = Color.White) }
    }
}
