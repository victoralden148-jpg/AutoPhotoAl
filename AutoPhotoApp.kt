package com.autophoto.ai

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.autophoto.ai.data.local.AppDatabase
import com.autophoto.ai.data.remote.SupabaseConfig
import com.autophoto.ai.data.repository.PhotoRepository
import com.autophoto.ai.domain.PhotoProcessor

/** DI manual sederhana: semua dependency dibuat sekali di sini. */
class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)
    val repository = PhotoRepository(context.applicationContext, database.photoDao())
    val photoProcessor = PhotoProcessor()

    /** Client Supabase (lazy: baru dibuat saat dipakai pertama kali). */
    val supabase by lazy { SupabaseConfig.createClient() }
}

class AutoPhotoApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Coil dengan cache memori + disk agar galeri mulus. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .crossfade(true)
        .build()
}
