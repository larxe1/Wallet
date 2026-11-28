package com.moneytracker.app

import com.moneytracker.app.data.remote.SupabaseClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionPolicyTest {

    @Test
    fun ownerEmail_isCanonicalSingleUserAddress() {
        assertEquals("ec.mataac@gmail.com", SupabaseClient.OWNER_EMAIL)
    }

    @Test
    fun isAllowedOwnerEmail_validatesTrimmedAndCaseInsensitiveMatch() {
        assertTrue(SupabaseClient.isAllowedOwnerEmail("ec.mataac@gmail.com"))
        assertTrue(SupabaseClient.isAllowedOwnerEmail("  EC.MATAAC@GMAIL.COM  "))

        assertFalse(SupabaseClient.isAllowedOwnerEmail("other@gmail.com"))
        assertFalse(SupabaseClient.isAllowedOwnerEmail(""))
        assertFalse(SupabaseClient.isAllowedOwnerEmail("   "))
    }

    @Test
    fun shouldClearSessionOnRefreshFailure_preservesSessionOnTransientAndNonAuthErrors() {
        val permanentLookingJson = """{"error":"invalid_grant","error_code":"refresh_token_not_found"}"""

        assertFalse(SupabaseClient.shouldClearSessionOnRefreshFailure(500, permanentLookingJson))
        assertFalse(SupabaseClient.shouldClearSessionOnRefreshFailure(502, ""))
        assertFalse(SupabaseClient.shouldClearSessionOnRefreshFailure(503, "Service Unavailable"))
        assertFalse(SupabaseClient.shouldClearSessionOnRefreshFailure(504, ""))
        assertFalse(SupabaseClient.shouldClearSessionOnRefreshFailure(429, """{"message":"Too many requests"}"""))
        assertFalse(SupabaseClient.shouldClearSessionOnRefreshFailure(408, "Request Timeout"))

        // Non-JSON proxy or WAF 403 page must never wipe saved device session
        assertFalse(
            SupabaseClient.shouldClearSessionOnRefreshFailure(
                403,
                "<html><body><h1>403 Forbidden</h1><p>Cloudflare</p></body></html>"
            )
        )

        // Unrelated JSON 400 must preserve saved session
        assertFalse(
            SupabaseClient.shouldClearSessionOnRefreshFailure(
                400,
                """{"message":"Temporary upstream error"}"""
            )
        )
    }

    @Test
    fun shouldClearSessionOnRefreshFailure_clearsOnlyOnExplicitPermanentAuthErrors() {
        assertTrue(
            SupabaseClient.shouldClearSessionOnRefreshFailure(
                400,
                """{"error":"invalid_grant","error_description":"Invalid Refresh Token: Refresh Token Not Found"}"""
            )
        )
        assertTrue(
            SupabaseClient.shouldClearSessionOnRefreshFailure(
                400,
                """{"error_code":"refresh_token_already_used"}"""
            )
        )
        assertTrue(
            SupabaseClient.shouldClearSessionOnRefreshFailure(
                401,
                """{"error_code":"session_not_found"}"""
            )
        )
        assertTrue(
            SupabaseClient.shouldClearSessionOnRefreshFailure(
                403,
                """{"error_code":"user_banned"}"""
            )
        )
    }
}
