package com.rimagwinya.app.core.network


/**
 * Supplies the bearer token for outgoing requests.
 *
 * Returning null means "nobody is signed in", and the interceptor falls back
 * to the anon key — which Row Level Security allows to read the menu and
 * nothing else.
 *
 * It is an interface so the networking layer never has to know where a token
 * comes from. Phase 2 bound it to a stub that always returned null; Phase 3
 * rebound it to the live supabase-kt session, and no code in core/network
 * changed.
 */
interface TokenProvider {
    fun accessToken(): String?
}
