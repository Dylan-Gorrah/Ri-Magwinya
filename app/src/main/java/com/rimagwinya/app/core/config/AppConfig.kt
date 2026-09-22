package com.rimagwinya.app.core.config

import com.rimagwinya.app.BuildConfig
import com.rimagwinya.app.core.money.Money

/**
 * Settings that are the same for everyone, in one place so they are not
 * scattered as literals through the code.
 *
 * Note there is no ALLOWED_EMAIL_DOMAIN. Students use ordinary personal
 * accounts, so registration only checks that an address is well formed. The
 * student number is what ties an account to a person.
 */
object AppConfig {

    /** From local.properties via BuildConfig. Empty until Phase 2. */
    val supabaseUrl: String = BuildConfig.SUPABASE_URL
    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY

    /** The Google Web client id. Empty until Phase 12 is set up. */
    val googleWebClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    val isGoogleSignInConfigured: Boolean
        get() = googleWebClientId.isNotBlank()

    val isBackendConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    /** Bloemfontein, for the Open-Meteo forecast. */
    const val LATITUDE = -29.12
    const val LONGITUDE = 26.21
    const val TIMEZONE = "Africa/Johannesburg"

    /** Weather is a nice extra, so it is cached hard and never blocks the menu. */
    const val WEATHER_CACHE_MINUTES = 60L

    /** Hot items sort first below this, cold items first at or above HOT_C. */
    const val COLD_C = 15.0
    const val HOT_C = 27.0
    const val RAIN_PROBABILITY = 50

    /** Wallet top-up limits, enforced again in the Edge Function and in SQL. */
    val MIN_TOP_UP = Money.ofRands(10)
    val MAX_TOP_UP = Money.ofRands(1000)
    val TOP_UP_PRESETS = listOf(
        Money.ofRands(20),
        Money.ofRands(50),
        Money.ofRands(100),
        Money.ofRands(200),
    )

    /** Two no-shows and pay-at-counter is withdrawn. */
    const val NO_SHOW_LIMIT = 2

    /** Stamps needed for a full loyalty card. Display only, no redemption. */
    const val LOYALTY_TARGET = 10

    /**
     * Orders for a break close this many minutes before it ends, so the
     * kitchen has time to make the food. Breaks that have closed are hidden.
     */
    const val SLOT_CUTOFF_MINUTES = 5L

    /** The tuckshop's clock. "Today" and every time shown are in this zone. */
    val zone: java.time.ZoneId = java.time.ZoneId.of(TIMEZONE)

    const val PASSWORD_MIN_LENGTH = 8
    const val COLLECTION_CODE_LENGTH = 4
}
