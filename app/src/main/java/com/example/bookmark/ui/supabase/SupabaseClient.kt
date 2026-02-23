package com.example.bookmark.ui.supabase

import com.example.bookmark.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    // Aquí creamos el cliente usando las claves ocultas
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
        // install(Auth) // Si usas autenticación
    }
}