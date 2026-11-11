package com.autophoto.ai.ui.edit

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autophoto.ai.data.repository.PhotoRepository
import com.autophoto.ai.domain.PhotoProcessor
import com.autophoto.ai.domain.model.MoodResult
import com.autophoto.ai.domain.model.PhotoFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class EditUiState(
    val isProcessing: Boolean = true,
    val original: Bitmap? = null,
    val result: Bitmap? = null,
    val mood: MoodResult? = null,
    val filter: PhotoFilter? = null,
    val subjectFound: Boolean = false,
    val showOriginal: Boolean = false,
    val isSaving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
)

class EditViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
    private val repository: PhotoRepository,
    private val processor: PhotoProcessor
) : ViewModel() {

    // Nama file capture sementara (dikirim lewat argumen navigasi)
    private val captureFile = File(app.cacheDir, checkNotNull(savedStateHandle.get<String>("name")))

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init { process() }

    /** Jalankan pipeline auto crop -> zoom -> filter -> enhance. */
    fun process() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            try {
                val r = processor.process(captureFile.absolutePath)
                _state.update {
                    it.copy(
                        isProcessing = false, original = r.original, result = r.result,
                        mood = r.mood, filter = r.filter, subjectFound = r.subjectFound
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(isProcessing = false, error = "Gagal memproses foto") }
            }
        }
    }

    fun toggleOriginal() = _state.update { it.copy(showOriginal = !it.showOriginal) }

    /** Simpan ke internal storage + Room. */
    fun save() {
        val s = _state.value
        val bitmap = s.result ?: return
        val mood = s.mood ?: return
        val filter = s.filter ?: return
        if (s.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val id = repository.savePhoto(bitmap, mood, filter)
                captureFile.delete()
                _state.update { it.copy(isSaving = false, savedId = id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(isSaving = false, error = "Gagal menyimpan foto") }
            }
        }
    }

    fun discard() { captureFile.delete() }
}
