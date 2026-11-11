package com.autophoto.ai.ui.camera

import android.net.Uri
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import com.autophoto.ai.domain.live.Guidance
import com.autophoto.ai.domain.model.PhotoFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

data class CameraUiState(
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val zoom: Float = 1f,
    val isCapturing: Boolean = false,

    // --- AI ---
    val aiGuide: Boolean = true,
    val autoZoom: Boolean = true,
    val autoShutter: Boolean = false,
    val guidance: Guidance = Guidance(),
    /** null = filter otomatis dari AI. */
    val filter: PhotoFilter? = null,
    val autoFilter: PhotoFilter? = null,

    // --- Tampilan & pengaturan ala kamera pro ---
    val showGrid: Boolean = true,
    val showLevel: Boolean = true,
    val timerSeconds: Int = 0,
    val ev: Float = 0f,
    val wb: WhiteBalance = WhiteBalance.AUTO,
    val manual: Boolean = false,
    val iso: Int = 200,
    val shutterIdx: Int = 3,

    // --- Pose ---
    val pose: PoseGuide? = null,
    val refImage: Uri? = null,
    val showPoseOverlay: Boolean = true,
    val suggestedPose: PoseGuide? = null,

    // --- Sheet ---
    val showSettings: Boolean = false,
    val showPoses: Boolean = false
)

class CameraViewModel : ViewModel() {
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    private var zoomMin = 1f
    private var zoomMax = 1f
    private var lastUserZoom = 0L
    private var pendingFilter: PhotoFilter? = null
    private var pendingCount = 0

    fun edit(block: (CameraUiState) -> CameraUiState) = _state.update(block)

    fun setZoomLimits(min: Float, max: Float) { zoomMin = min; zoomMax = max }

    /** OFF -> AUTO -> ON -> OFF */
    fun cycleFlash() = _state.update {
        it.copy(
            flashMode = when (it.flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                else -> ImageCapture.FLASH_MODE_OFF
            }
        )
    }

    fun cycleTimer() = _state.update {
        it.copy(timerSeconds = when (it.timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 })
    }

    fun flipLens() = _state.update {
        it.copy(
            lensFacing = if (it.lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK,
            zoom = 1f
        )
    }

    /** Zoom dari pengguna (pinch / chip): AI berhenti auto-zoom selama 4 detik. */
    fun onUserZoom(z: Float) {
        lastUserZoom = SystemClock.elapsedRealtime()
        _state.update { it.copy(zoom = z.coerceIn(zoomMin, maxOf(zoomMin, zoomMax))) }
    }

    fun setCapturing(value: Boolean) = _state.update { it.copy(isCapturing = value) }

    /** Dipanggil dari thread analisis setiap ada hasil baru. */
    fun onGuidance(g: Guidance) {
        // Filter otomatis: ganti hanya bila saran sama 2x berturut-turut (anti kedip)
        val sf = g.suggestedFilter
        if (sf == pendingFilter) pendingCount++ else { pendingFilter = sf; pendingCount = 1 }
        val userIdle = SystemClock.elapsedRealtime() - lastUserZoom > 4000

        _state.update { s ->
            var zoom = s.zoom
            if (s.aiGuide && s.autoZoom && g.hasSubject && userIdle && abs(g.zoomFactor - 1f) > 0.12f) {
                val f = g.zoomFactor.coerceIn(0.7f, 1.6f)
                zoom = (s.zoom * (1f + (f - 1f) * 0.5f)).coerceIn(zoomMin, minOf(maxOf(zoomMin, zoomMax), 8f))
            }
            val applied = if (sf != null && pendingCount >= 2) sf else s.autoFilter ?: sf
            val pose = when {
                !g.hasFace || !g.hasSubject -> null
                (g.bottom - g.top) > (g.right - g.left) * 1.3f -> PoseGuide.STANDING
                else -> PoseGuide.SIT
            }
            s.copy(guidance = g, zoom = zoom, autoFilter = applied, suggestedPose = pose)
        }
    }
}
