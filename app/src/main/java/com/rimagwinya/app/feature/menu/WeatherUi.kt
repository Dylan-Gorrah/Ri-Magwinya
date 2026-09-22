package com.rimagwinya.app.feature.menu

import androidx.annotation.StringRes
import com.rimagwinya.app.R
import com.rimagwinya.app.domain.model.Sky
import com.rimagwinya.app.domain.model.Weather
import com.rimagwinya.app.domain.model.WeatherMood

@StringRes
fun Sky.labelRes(): Int = when (this) {
    Sky.Clear -> R.string.weather_sky_clear
    Sky.PartlyCloudy -> R.string.weather_sky_partly
    Sky.Fog -> R.string.weather_sky_fog
    Sky.Drizzle -> R.string.weather_sky_drizzle
    Sky.Rain -> R.string.weather_sky_rain
    Sky.Showers -> R.string.weather_sky_showers
    Sky.Thunderstorm -> R.string.weather_sky_storm
}

/** "Hot food first today", and why the menu looks reordered. */
@StringRes
fun WeatherMood.menuHintRes(): Int = when (this) {
    WeatherMood.Cold -> R.string.weather_hot_first
    WeatherMood.Hot -> R.string.weather_cold_first
    WeatherMood.Neutral -> R.string.weather_normal
}

/** The stock hint on the staff sales screen. */
@StringRes
fun Weather.tomorrowHintRes(): Int = when {
    tomorrowRainProbability >= com.rimagwinya.app.core.config.AppConfig.RAIN_PROBABILITY ->
        R.string.weather_hint_rain
    tomorrowMood == WeatherMood.Cold -> R.string.weather_hint_cold
    tomorrowMood == WeatherMood.Hot -> R.string.weather_hint_hot
    else -> R.string.weather_hint_none
}
