package com.rimagwinya.app.data.repository

import com.rimagwinya.app.core.database.WeatherDao
import com.rimagwinya.app.core.database.WeatherEntity
import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.data.remote.ForecastDto
import com.rimagwinya.app.data.remote.WeatherApi
import com.rimagwinya.app.domain.model.Sky
import com.rimagwinya.app.domain.model.Weather
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The weather, cached hard.
 *
 * It decides how the menu is sorted and nothing more, so it is never worth
 * a student waiting for: the cache answers for an hour, a failure returns
 * whatever was last stored, and a completely empty cache returns null
 * rather than an error for a screen to handle.
 */
@Singleton
open class WeatherRepository @Inject constructor(
    private val api: WeatherApi,
    private val dao: WeatherDao,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    open suspend fun weather(): Weather? = withContext(io) {
        val cached = dao.get()?.toDomain()
        if (cached != null && cached.isFresh(clock.millis())) return@withContext cached

        runCatching { api.forecast() }
            .map { it.toDomain(clock.millis()) }
            .onSuccess { fresh -> dao.put(fresh.toEntity()) }
            // Stale beats nothing: an hour-old temperature still sorts the
            // menu better than no temperature at all.
            .getOrElse { cached }
    }
}

private fun ForecastDto.toDomain(now: Long): Weather = Weather(
    temperatureC = current?.temperature ?: 0.0,
    sky = Sky.from(current?.weatherCode ?: 0),
    rainProbabilityToday = daily?.rainProbability?.getOrNull(0) ?: 0,
    tomorrowMaxC = daily?.maxTemperature?.getOrNull(1) ?: 0.0,
    tomorrowMinC = daily?.minTemperature?.getOrNull(1) ?: 0.0,
    tomorrowRainProbability = daily?.rainProbability?.getOrNull(1) ?: 0,
    tomorrowSky = Sky.from(daily?.weatherCode?.getOrNull(1) ?: 0),
    fetchedAt = now,
)

private fun WeatherEntity.toDomain(): Weather = Weather(
    temperatureC = temperatureC,
    sky = Sky.from(weatherCode),
    rainProbabilityToday = rainProbabilityToday,
    tomorrowMaxC = tomorrowMaxC,
    tomorrowMinC = tomorrowMinC,
    tomorrowRainProbability = tomorrowRainProbability,
    tomorrowSky = Sky.from(tomorrowCode),
    fetchedAt = fetchedAt,
)

private fun Weather.toEntity(): WeatherEntity = WeatherEntity(
    temperatureC = temperatureC,
    weatherCode = skyCode(sky),
    rainProbabilityToday = rainProbabilityToday,
    tomorrowMaxC = tomorrowMaxC,
    tomorrowMinC = tomorrowMinC,
    tomorrowRainProbability = tomorrowRainProbability,
    tomorrowCode = skyCode(tomorrowSky),
    fetchedAt = fetchedAt,
)

/** One representative WMO code per label, so the cache round-trips. */
private fun skyCode(sky: Sky): Int = when (sky) {
    Sky.Clear -> 0
    Sky.PartlyCloudy -> 2
    Sky.Fog -> 45
    Sky.Drizzle -> 51
    Sky.Rain -> 61
    Sky.Showers -> 80
    Sky.Thunderstorm -> 95
}
