package com.kalotracker.app.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseModule {
    // Configurable project URL and Anon Key. In production, these can be set via BuildConfig.
    var supabaseUrl: String = "https://your-project.supabase.co"
    var supabaseAnonKey: String = "your-anon-key-here"

    val isConfigured: Boolean
        get() = !supabaseUrl.contains("your-project") && supabaseAnonKey != "your-anon-key-here"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseAnonKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }
}
