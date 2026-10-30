package com.crisanfitos.stitchmesh3d.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseConfigTest {

    @Test
    fun `default config resolves valid Supabase project URL and endpoints`() {
        val config = SupabaseConfig(
            projectId = "odgjwekjazwcyyqbgiqi",
            supabaseUrl = "https://odgjwekjazwcyyqbgiqi.supabase.co",
            supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"
        )

        assertTrue(config.isReadyForNetwork)
        assertEquals("https://odgjwekjazwcyyqbgiqi.supabase.co/rest/v1", config.restEndpointUrl)
        assertEquals("https://odgjwekjazwcyyqbgiqi.supabase.co/auth/v1", config.authEndpointUrl)
    }

    @Test
    fun `config with blank anon key reports not ready for network`() {
        val config = SupabaseConfig(
            projectId = "odgjwekjazwcyyqbgiqi",
            supabaseUrl = "https://odgjwekjazwcyyqbgiqi.supabase.co",
            supabaseAnonKey = ""
        )

        assertFalse(config.isReadyForNetwork)
    }

    @Test
    fun `default instance without configuration is not ready for network`() {
        val config = SupabaseConfig(
            projectId = "",
            supabaseUrl = "",
            supabaseAnonKey = ""
        )

        assertFalse(config.isReadyForNetwork)
    }
}
