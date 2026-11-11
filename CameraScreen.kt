package com.autophoto.ai.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.autophoto.ai.domain.AutoFilterSelector
import com.autophoto.ai.domain.CaptureHint
import com.autophoto.ai.domain.CaptureHints
import com.autophoto.ai.domain.live.LiveAnalyzer
import com.autophoto.ai.domain.model.PhotoFilter
import com.autophoto.ai.ui.AppViewModelProvider
import com.autophoto.ai.ui.components.CircleIconButton
import com.autophoto.ai.ui.components.GlassPill
import com.autophoto.ai.ui.theme.AppColors
import com.autophoto.ai.ui.theme.StatusBarIcons
import com.autophoto.ai.utils.PermissionUtils
import com.autophoto.ai.utils.awaitCameraProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onCaptured: (String) -> Unit,
    vm: CameraViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    StatusBarIcons(darkIcons = false)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
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
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE // agar bisa di-clip & diberi filter
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolutionSelector)
            .build()
    }
    val analysis = remember {
        ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
    }
    val analyzer = remember { LiveAnalyzer(onResult = { vm.onGuidance(it) }) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val filterSelector = remember { AutoFilterSelector() }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var ranges by remember { mutableStateOf(ProControls.Ranges()) }
    var zoomMin by remember { mutableFloatStateOf(1f) }
    var zoomMax by remember { mutableFloatStateOf(1f) }
    var tilt by remember { mutableFloatStateOf(0f) }
    var countdown by remember { mutableIntStateOf(0) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var lastAutoShot by remember { mutableStateOf(0L) }

    // Bind kamera saat izin diberikan / lensa diganti
    LaunchedEffect(hasPermission, state.lensFacing) {
        if (!hasPermission) return@LaunchedEffect
        val p = context.awaitCameraProvider()
        provider = p
        val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val selector = CameraSelector.Builder().requireLensFacing(state.lensFacing).build()
        analysis.setAnalyzer(analysisExecutor, analyzer)
        try {
            p.unbindAll()
            camera = try {
                p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, analysis)
            } catch (e: Exception) {
                // perangkat tidak kuat 3 use case: tetap jalan tanpa panduan AI
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            }
            camera?.let { c ->
                val zs = c.cameraInfo.zoomState.value
                zoomMin = zs?.minZoomRatio ?: 1f
                zoomMax = zs?.maxZoomRatio ?: 1f
                vm.setZoomLimits(zoomMin, zoomMax)
                analyzer.zoomMax = zoomMax
                ranges = ProControls.ranges(c)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(state.flashMode) { imageCapture.flashMode = state.flashMode }

    // Zoom halus (dipakai pinch, chip, dan auto-zoom AI)
    val animZoom by animateFloatAsState(state.zoom, tween(450), label = "zoom")
    LaunchedEffect(camera, animZoom) {
        camera?.let { c ->
            val zs = c.cameraInfo.zoomState.value
            c.cameraControl.setZoomRatio(animZoom.coerceIn(zs?.minZoomRatio ?: 1f, zs?.maxZoomRatio ?: 1f))
        }
    }
    // Kompensasi eksposur (EV)
    LaunchedEffect(camera, state.ev) {
        camera?.let { c ->
            val st = c.cameraInfo.exposureState
            if (st.isExposureCompensationSupported) {
                val step = st.exposureCompensationStep.toFloat()
                if (step > 0f) {
                    val idx = (state.ev / step).roundToInt()
                        .coerceIn(st.exposureCompensationRange.lower, st.exposureCompensationRange.upper)
                    c.cameraControl.setExposureCompensationIndex(idx)
                }
            }
        }
    }
    // White balance + ISO / rana manual
    LaunchedEffect(camera, state.wb, state.manual, state.iso, state.shutterIdx, ranges) {
        camera?.let { ProControls.apply(it, state, ranges) }
    }

    // Beri tahu analyzer kondisi terbaru
    SideEffect {
        analyzer.enabled = state.aiGuide
        analyzer.autoZoom = state.autoZoom
        analyzer.levelEnabled = state.showLevel
        analyzer.mirror = state.lensFacing == CameraSelector.LENS_FACING_FRONT
        analyzer.zoomRatio = state.zoom
        analyzer.tiltDeg = tilt
    }

    // Sensor kemiringan untuk garis level
    DisposableEffect(state.showLevel) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            private var smooth = 0f
            override fun onSensorChanged(e: SensorEvent) {
                val ax = e.values[0]
                val ay = e.values[1]
                if (ay < 5f) return // HP tidak tegak
                val deg = Math.toDegrees(atan2(ax.toDouble(), ay.toDouble())).toFloat()
                smooth += (deg - smooth) * 0.2f
                val r = smooth.roundToInt().toFloat()
                if (r != tilt) tilt = r
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null && state.showLevel) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    // Lepas kamera saat layar ditinggalkan (mis. pindah ke layar Edit)
    DisposableEffect(Unit) {
        onDispose {
            analysis.clearAnalyzer()
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    fun takePicture() {
        if (vm.state.value.isCapturing || camera == null) return
        vm.setCapturing(true)
        val s = vm.state.value
        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        CaptureHints.put(file.name, CaptureHint(framedLive = s.aiGuide, filter = s.filter ?: s.autoFilter))
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

    fun capture() {
        if (state.isCapturing || camera == null || countdown > 0) return
        val t = state.timerSeconds
        if (t > 0) {
            scope.launch {
                for (i in t downTo 1) {
                    countdown = i
                    delay(1000)
                }
                countdown = 0
                takePicture()
            }
        } else takePicture()
    }

    // Jepret otomatis saat komposisi pas ~1.4 detik
    LaunchedEffect(state.guidance.readyStreak) {
        if (state.autoShutter && state.aiGuide && state.guidance.readyStreak >= 3 &&
            !state.isCapturing && countdown == 0 &&
            SystemClock.elapsedRealtime() - lastAutoShot > 3000
        ) {
            lastAutoShot = SystemClock.elapsedRealtime()
            takePicture()
        }
    }
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1200)
            focusPoint = null
        }
    }

    val g = state.guidance
    val liveFilter: PhotoFilter? = state.filter ?: state.autoFilter

    // --- UI ---
    Box(Modifier.fillMaxSize().background(AppColors.Dark)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(Icons.Rounded.ArrowBack, onBack, contentDescription = "Kembali")
                GlassPill(
                    if (state.aiGuide) g.message else "Panduan AI mati",
                    icon = Icons.Rounded.AutoAwesome
                )
                CircleIconButton(Icons.Rounded.FlipCameraAndroid, vm::flipLens, contentDescription = "Balik kamera")
            }

            // Preview 3:4 dengan sudut membulat
            Box(
                Modifier.padding(horizontal = 12.dp).fillMaxWidth().aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(32.dp)).background(Color.Black)
            ) {
                if (hasPermission) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize(),
                        update = { v ->
                            val f = liveFilter
                            if (f == null) v.setLayerType(View.LAYER_TYPE_NONE, null) else {
                                val paint = Paint().apply {
                                    colorFilter = ColorMatrixColorFilter(filterSelector.colorMatrix(f))
                                }
                                v.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                            }
                        }
                    )
                    GuidanceOverlay(
                        g = g,
                        aiOn = state.aiGuide,
                        showGrid = state.showGrid,
                        showLevel = state.showLevel,
                        tilt = tilt,
                        pose = if (state.showPoseOverlay) state.pose else null,
                        focus = focusPoint,
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(camera) {
                                detectTapGestures { off ->
                                    focusPoint = off
                                    camera?.let { c ->
                                        val pt = previewView.meteringPointFactory.createPoint(off.x, off.y)
                                        c.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(pt).build())
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoomChange, _ ->
                                    if (zoomChange != 1f) vm.onUserZoom(vm.state.value.zoom * zoomChange)
                                }
                            }
                    )

                    // Ikon cepat di atas preview
                    Row(
                        Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val flashIcon = when (state.flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto
                            else -> Icons.Rounded.FlashOff
                        }
                        ToolChip(flashIcon, state.flashMode != ImageCapture.FLASH_MODE_OFF, vm::cycleFlash)
                        ToolChip(Icons.Rounded.AutoAwesome, state.aiGuide, { vm.edit { it.copy(aiGuide = !it.aiGuide) } }, "AI")
                        ToolChip(Icons.Rounded.GridOn, state.showGrid, { vm.edit { it.copy(showGrid = !it.showGrid) } })
                        ToolChip(
                            Icons.Rounded.Timer, state.timerSeconds > 0, vm::cycleTimer,
                            if (state.timerSeconds > 0) "${state.timerSeconds}s" else null
                        )
                    }

                    // Pilihan zoom
                    ZoomChips(
                        zoom = state.zoom, min = zoomMin, max = zoomMax,
                        onPick = { vm.onUserZoom(it) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
                    )

                    if (countdown > 0) {
                        Text(
                            "$countdown", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
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

            // Pilihan filter: Auto (AI) + 5 filter
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OptionChip(
                    if (state.filter == null) "Auto · ${state.autoFilter?.displayName ?: "AI"}" else "Auto AI",
                    state.filter == null, { vm.edit { it.copy(filter = null) } }
                )
                PhotoFilter.values().forEach { f ->
                    OptionChip(f.displayName, state.filter == f, { vm.edit { it.copy(filter = f) } })
                }
            }

            Spacer(Modifier.weight(1f))

            // Panel bawah: pose | shutter | pengaturan
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PoseThumb(state, onClick = { vm.edit { it.copy(showPoses = true) } })
                ShutterButton(isCapturing = state.isCapturing, onClick = ::capture)
                CircleIconButton(
                    Icons.Rounded.Tune, { vm.edit { it.copy(showSettings = true) } }, size = 56.dp,
                    container = AppColors.DarkPanel, contentDescription = "Pengaturan"
                )
            }
        }

        if (state.showSettings) {
            SettingsSheet(state, ranges, onChange = { vm.edit(it) }, onDismiss = { vm.edit { it.copy(showSettings = false) } })
        }
        if (state.showPoses) {
            PoseSheet(state, onChange = { vm.edit(it) }, onDismiss = { vm.edit { it.copy(showPoses = false) } })
        }
    }
}

@Composable
private fun ToolChip(icon: ImageVector, active: Boolean, onClick: () -> Unit, label: String? = null) {
    Row(
        Modifier.clip(CircleShape)
            .background(if (active) AppColors.Accent else Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        if (label != null) Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ZoomChips(zoom: Float, min: Float, max: Float, onPick: (Float) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(0.5f, 1f, 2f, 5f).filter { it >= min - 0.01f && it <= max + 0.01f }
    if (options.isEmpty()) return
    val nearest = options.minByOrNull { abs(it - zoom) }
    Row(
        modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { z ->
            val selected = z == nearest
            val label = if (selected) "${"%.1f".format(zoom).removeSuffix(".0")}x" else
                (if (z < 1f) ".5" else "${z.toInt()}")
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onPick(z) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color(0xFFFFD60A) else Color.White
                )
            }
        }
    }
}

/** Thumbnail kecil contoh pose (atau foto referensi); tap untuk memilih. */
@Composable
private fun PoseThumb(state: CameraUiState, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp, 72.dp).clip(RoundedCornerShape(14.dp))
            .background(AppColors.DarkPanel)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.refImage != null -> AsyncImage(
                model = state.refImage, contentDescription = "Referensi pose",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            state.pose != null -> PoseFigure(state.pose)
            else -> Icon(Icons.Rounded.Accessibility, "Pose", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(26.dp))
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
