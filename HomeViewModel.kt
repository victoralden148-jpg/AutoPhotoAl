package com.autophoto.ai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autophoto.ai.data.local.PhotoEntity
import com.autophoto.ai.data.repository.PhotoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repository: PhotoRepository) : ViewModel() {
    /** null = masih memuat. */
    val photos: StateFlow<List<PhotoEntity>?> = repository.photos
        .map<List<PhotoEntity>, List<PhotoEntity>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
