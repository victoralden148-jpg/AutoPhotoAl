package com.autophoto.ai.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.autophoto.ai.AutoPhotoApp
import com.autophoto.ai.ui.camera.CameraViewModel
import com.autophoto.ai.ui.detail.DetailViewModel
import com.autophoto.ai.ui.edit.EditViewModel
import com.autophoto.ai.ui.gallery.GalleryViewModel
import com.autophoto.ai.ui.home.HomeViewModel

/** Factory ViewModel: mengambil dependency dari AppContainer. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(app().container.repository) }
        initializer { GalleryViewModel(app().container.repository) }
        initializer { CameraViewModel() }
        initializer {
            EditViewModel(app(), createSavedStateHandle(), app().container.repository, app().container.photoProcessor)
        }
        initializer { DetailViewModel(createSavedStateHandle(), app().container.repository) }
    }
}

fun CreationExtras.app(): AutoPhotoApp = this[APPLICATION_KEY] as AutoPhotoApp
