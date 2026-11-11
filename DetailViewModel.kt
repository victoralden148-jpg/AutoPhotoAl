package com.autophoto.ai.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autophoto.ai.data.local.PhotoEntity
import com.autophoto.ai.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: PhotoRepository
) : ViewModel() {

    private val photoId: Long = checkNotNull(savedStateHandle.get<Long>("id"))
    private val _photo = MutableStateFlow<PhotoEntity?>(null)
    val photo: StateFlow<PhotoEntity?> = _photo.asStateFlow()

    init {
        viewModelScope.launch { _photo.value = repository.getById(photoId) }
    }

    fun delete(onDone: () -> Unit) {
        val p = _photo.value ?: return
        viewModelScope.launch {
            repository.delete(p)
            onDone()
        }
    }
}
