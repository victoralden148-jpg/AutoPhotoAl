package com.autophoto.ai.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autophoto.ai.ui.camera.CameraScreen
import com.autophoto.ai.ui.components.FloatingNavBar
import com.autophoto.ai.ui.components.NavTab
import com.autophoto.ai.ui.detail.DetailScreen
import com.autophoto.ai.ui.edit.EditScreen
import com.autophoto.ai.ui.gallery.GalleryScreen
import com.autophoto.ai.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val EDIT = "edit/{name}"
    const val GALLERY = "gallery"
    const val DETAIL = "detail/{id}"

    fun edit(captureFileName: String) = "edit/${Uri.encode(captureFileName)}"
    fun detail(id: Long) = "detail/$id"
}

private fun NavHostController.navigateTab(route: String) = navigate(route) {
    popUpTo(Routes.HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.fillMaxSize()) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenCamera = { nav.navigate(Routes.CAMERA) { launchSingleTop = true } },
                    onOpenGallery = { nav.navigateTab(Routes.GALLERY) },
                    onOpenPhoto = { nav.navigate(Routes.detail(it)) }
                )
            }
            composable(Routes.CAMERA) {
                CameraScreen(
                    onBack = { nav.popBackStack() },
                    onCaptured = { name -> nav.navigate(Routes.edit(name)) }
                )
            }
            composable(Routes.EDIT, arguments = listOf(navArgument("name") { type = NavType.StringType })) {
                EditScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { id -> nav.navigate(Routes.detail(id)) { popUpTo(Routes.HOME) } }
                )
            }
            composable(Routes.GALLERY) {
                GalleryScreen(onOpenPhoto = { nav.navigate(Routes.detail(it)) })
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                DetailScreen(onBack = { nav.popBackStack() })
            }
        }

        // Bottom nav hanya tampil di Home & Galeri
        AnimatedVisibility(
            visible = route == Routes.HOME || route == Routes.GALLERY,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            FloatingNavBar(
                selected = if (route == Routes.GALLERY) NavTab.GALLERY else NavTab.HOME,
                onHome = { nav.navigateTab(Routes.HOME) },
                onCamera = { nav.navigate(Routes.CAMERA) { launchSingleTop = true } },
                onGallery = { nav.navigateTab(Routes.GALLERY) },
                modifier = Modifier.navigationBarsPadding().padding(bottom = 12.dp)
            )
        }
    }
}
