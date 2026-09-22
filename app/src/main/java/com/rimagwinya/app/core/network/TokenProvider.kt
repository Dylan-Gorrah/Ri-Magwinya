package com.rimagwinya.app.core.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the bearer token for outgoing requests.
 *
 * Phase 3 replaces the implementation with one that reads the live session
 * from supabase-kt. Until then it returns null and the interceptor falls
 * back to the anon key, which is enough to read the menu and nothing else —
 * exactly what Phase 2 needs to prove.
 *
 * It exists as an interface now so that swap is a binding change rather than
 * an edit to the networking layer.
 */
interface TokenProvider {
    fun accessToken(): String?
}

@Singleton
class AnonTokenProvider @Inject constructor() : TokenProvider {
    override fun accessToken(): String? = null
}
