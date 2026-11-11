package com.autophoto.ai.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Konfigurasi Supabase (langsung di kode, tanpa local.properties).
 * Hanya pakai publishable/anon key. JANGAN pakai secret/service_role key.
 */
object SupabaseConfig {
    const val SUPABASE_URL = "https://acrkfkvwuzqthyecujep.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_4ifhFyLpMeoRSSSL7hsjjw_k3w7CLdt"

    fun createClient(): SupabaseClient =
        createSupabaseClient(supabaseUrl = SUPABASE_URL, supabaseKey = SUPABASE_KEY) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
}
