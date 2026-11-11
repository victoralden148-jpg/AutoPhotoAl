package com.autophoto.ai.ui.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CameraUiState(
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val zoom: Float = 1f,
    val isCapturing: Boolean = false
)

class CameraViewModel : ViewModel() {
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

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

    fun flipLens() = _state.update {
        it.copy(
            lensFacing = if (it.lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK,
            zoom = 1f
        )
    }

    fun toggleZoom() = _state.update { it.copy(zoom = if (it.zoom < 2f) 2f else 1f) }
    fun setCapturing(value: Boolean) = _state.update { it.copy(isCapturing = value) }
}
