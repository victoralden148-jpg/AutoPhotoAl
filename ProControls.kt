@file:OptIn(ExperimentalCamera2Interop::class)

package com.autophoto.ai.ui.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera

enum class WhiteBalance(val label: String, val awbMode: Int) {
    AUTO("Auto", CameraMetadata.CONTROL_AWB_MODE_AUTO),
    DAYLIGHT("Siang", CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT),
    CLOUDY("Mendung", CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    INCANDESCENT("Lampu", CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT("Neon", CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT)
}

/** Penyebut kecepatan rana: 1/1000 ... 1/8 detik. */
val ShutterDenominators = listOf(1000, 500, 250, 125, 60, 30, 15, 8)

object ProControls {

    data class Ranges(
        val manualSupported: Boolean = false,
        val isoMin: Int = 100,
        val isoMax: Int = 800,
        val expMinNs: Long = 1_000_000L,
        val expMaxNs: Long = 125_000_000L
    )

    fun ranges(camera: Camera): Ranges = try {
        val info = Camera2CameraInfo.from(camera.cameraInfo)
        val iso: Range<Int>? = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exp: Range<Long>? = info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        if (iso == null || exp == null) Ranges() else Ranges(
            manualSupported = true,
            isoMin = iso.lower, isoMax = iso.upper.coerceAtMost(3200),
            expMinNs = exp.lower, expMaxNs = exp.upper
        )
    } catch (e: Exception) {
        Ranges()
    }

    /** Terapkan white balance + ISO/rana manual lewat Camera2 interop. */
    fun apply(camera: Camera, s: CameraUiState, r: Ranges) {
        try {
            val b = CaptureRequestOptions.Builder()
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, s.wb.awbMode)
            if (s.manual && r.manualSupported) {
                val ns = (1_000_000_000L / ShutterDenominators[s.shutterIdx.coerceIn(0, ShutterDenominators.lastIndex)])
                    .coerceIn(r.expMinNs, r.expMaxNs)
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
                b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, s.iso.coerceIn(r.isoMin, r.isoMax))
                b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, ns)
            }
            Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(b.build())
        } catch (e: Exception) {
            // perangkat tidak mendukung: abaikan
        }
    }
}
