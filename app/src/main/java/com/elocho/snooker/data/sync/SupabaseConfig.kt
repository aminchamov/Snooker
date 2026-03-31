package com.elocho.snooker.data.sync

import com.elocho.snooker.BuildConfig

object SupabaseConfig {
    val projectUrl: String = BuildConfig.SUPABASE_URL
    val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    const val adminUsername: String = "admin"
    const val adminPassword: String = "12345678"
    const val adminEmail: String = "admin@elocho.local"
}
