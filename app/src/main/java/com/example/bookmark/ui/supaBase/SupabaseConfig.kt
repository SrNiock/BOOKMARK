package com.example.bookmark.ui.supaBase

import androidx.work.Configuration
import com.google.android.gms.fitness.data.Value


@Configuration
class SupabaseConfig {

    @Value("\${supabase.url}")
    lateinit var url: String

    @Value("\${supabase.key}")
    lateinit var key: String

    @Bean
    fun supabaseClient() = createSupabaseClient(url, key) {
        install(Postgrest)
    }
}