package com.rimagwinya.app.domain.model

import com.rimagwinya.app.core.config.AppConfig

/** What today's weather should do to the menu. */
enum class WeatherMood {
    /** Cold or wet: hot items first. */
    Cold,

    /** Hot: cold items first. */
    Hot,

    /** Neither: the menu's own order. */
    Neutral,
}

/** WMO weather codes, in the few words a student needs. */
enum class Sky {
    Clear, PartlyCloudy, Fog, Drizzle, Rain, Showers, Thunderstorm;

    companion object {
        /** https://open-meteo.com/en/docs — WMO code table. */
        fun from(code: Int): Sky = when (code) {
            0, 1 -> Clear
            2, 3 -> PartlyCloudy
            45, 48 -> Fog
            51, 53, 55, 56, 57 -> Drizzle
            61, 63, 65, 66, 67 -> Rain
            80, 81, 82, 85, 86 -> Showers
            95, 96, 99 -> Thunderstorm
            else -> PartlyCloudy
        }
    }
}

data class Weather(
    val temperatureC: Double,
    val sky: Sky,
    val rainProbabilityToday: Int,
    val tomorrowMaxC: Double,
    val tomorrowMinC: Double,
    val tomorrowRainProbability: Int,
    val tomorrowSky: Sky,
    /** When it was fetched, for the 60-minute cache. */
    val fetchedAt: Long,
) {
    /**
     * Cold means cold **or** wet: standing in the rain to queue is the same
     * argument for a hot drink as a cold morning is.
     */
    val mood: WeatherMood
        get() = when {
            temperatureC < AppConfig.COLD_C || rainProbabilityToday >= AppConfig.RAIN_PROBABILITY -> WeatherMood.Cold
            temperatureC >= AppConfig.HOT_C -> WeatherMood.Hot
            else -> WeatherMood.Neutral
        }

    val expectsRain: Boolean get() = rainProbabilityToday >= AppConfig.RAIN_PROBABILITY

    /** Rounded for display: "14°C". */
    val roundedC: Int get() = Math.round(temperatureC).toInt()

    val tomorrowMood: WeatherMood
        get() = when {
            tomorrowMaxC < AppConfig.COLD_C || tomorrowRainProbability >= AppConfig.RAIN_PROBABILITY -> WeatherMood.Cold
            tomorrowMaxC >= AppConfig.HOT_C -> WeatherMood.Hot
            else -> WeatherMood.Neutral
        }

    fun isFresh(now: Long): Boolean = now - fetchedAt < AppConfig.WEATHER_CACHE_MINUTES * 60_000
}

/**
 * The menu in weather order: hot food first on a cold or wet day, cold
 * drinks first on a hot one, otherwise exactly as the tuckshop arranged it.
 *
 * Stable within each group, so the sort never scrambles the menu — it only
 * lifts one group above the other.
 */
fun List<MenuItem>.sortedForWeather(mood: WeatherMood): List<MenuItem> {
    val wanted = when (mood) {
        WeatherMood.Cold -> Temperature.Hot
        WeatherMood.Hot -> Temperature.Cold
        WeatherMood.Neutral -> return sortedBy { it.sortOrder }
    }
    return sortedWith(compareBy({ if (it.temperature == wanted) 0 else 1 }, { it.sortOrder }))
}
