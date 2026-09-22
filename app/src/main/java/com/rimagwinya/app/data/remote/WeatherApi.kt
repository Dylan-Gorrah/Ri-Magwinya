package com.rimagwinya.app.data.remote

import com.rimagwinya.app.core.config.AppConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo. Free, no key, and nothing about a student goes to it — only
 * the tuckshop's fixed coordinates.
 */
interface WeatherApi {

    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double = AppConfig.LATITUDE,
        @Query("longitude") longitude: Double = AppConfig.LONGITUDE,
        @Query("current") current: String = "temperature_2m,weather_code,precipitation",
        @Query("daily") daily: String =
            "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code",
        @Query("timezone") timezone: String = AppConfig.TIMEZONE,
        @Query("forecast_days") forecastDays: Int = 2,
    ): ForecastDto
}

@Serializable
data class ForecastDto(
    val current: CurrentDto? = null,
    val daily: DailyDto? = null,
)

@Serializable
data class CurrentDto(
    @SerialName("temperature_2m") val temperature: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
    val precipitation: Double = 0.0,
)

/** Arrays, one entry per day: today first, tomorrow second. */
@Serializable
data class DailyDto(
    @SerialName("temperature_2m_max") val maxTemperature: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val minTemperature: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val rainProbability: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
)
