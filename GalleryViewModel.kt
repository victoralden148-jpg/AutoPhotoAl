package com.autophoto.ai.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autophoto.ai.data.local.PhotoEntity
import com.autophoto.ai.data.repository.PhotoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(private val repository: PhotoRepository) : ViewModel() {
    /** null = masih memuat (supaya empty state tidak berkedip). */
    val photos: StateFlow<List<PhotoEntity>?> = repository.photos
        .map<List<PhotoEntity>, List<PhotoEntity>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(photo: PhotoEntity) {
        viewModelScope.launch { repository.delete(photo) }
    }
}
