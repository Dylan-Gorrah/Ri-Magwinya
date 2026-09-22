package com.rimagwinya.app.domain.model

import com.rimagwinya.app.core.database.WeatherDao
import com.rimagwinya.app.core.database.WeatherEntity
import com.rimagwinya.app.core.network.NetworkModule
import com.rimagwinya.app.data.remote.ForecastDto
import com.rimagwinya.app.data.remote.WeatherApi
import com.rimagwinya.app.data.repository.WeatherRepository
import com.rimagwinya.app.domain.pricing.MenuFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * The weather rules from section 9.4: when hot food goes first, when cold
 * drinks do, what the WMO codes mean, and the 60-minute cache.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherTest {

    private fun weather(
        temperature: Double = 20.0,
        rain: Int = 0,
        tomorrowMax: Double = 20.0,
        tomorrowRain: Int = 0,
        fetchedAt: Long = 0,
    ) = Weather(
        temperatureC = temperature,
        sky = Sky.Clear,
        rainProbabilityToday = rain,
        tomorrowMaxC = tomorrowMax,
        tomorrowMinC = 10.0,
        tomorrowRainProbability = tomorrowRain,
        tomorrowSky = Sky.Clear,
        fetchedAt = fetchedAt,
    )

    // --- The thresholds -------------------------------------------------------

    @Test
    fun `below fifteen degrees is a hot-food day`() {
        assertEquals(WeatherMood.Cold, weather(temperature = 14.9).mood)
        assertEquals(WeatherMood.Neutral, weather(temperature = 15.0).mood)
    }

    @Test
    fun `half a chance of rain counts as cold, however warm it is`() {
        assertEquals(WeatherMood.Cold, weather(temperature = 24.0, rain = 50).mood)
        assertEquals(WeatherMood.Neutral, weather(temperature = 24.0, rain = 49).mood)
        // Rain wins over heat: queueing outside in the wet is the point.
        assertEquals(WeatherMood.Cold, weather(temperature = 30.0, rain = 60).mood)
    }

    @Test
    fun `twenty-seven degrees and over is a cold-drinks day`() {
        assertEquals(WeatherMood.Hot, weather(temperature = 27.0).mood)
        assertEquals(WeatherMood.Neutral, weather(temperature = 26.9).mood)
    }

    @Test
    fun `the rain banner follows the same fifty per cent`() {
        assertTrue(weather(rain = 50).expectsRain)
        assertFalse(weather(rain = 49).expectsRain)
    }

    // --- Sorting --------------------------------------------------------------

    private val menu = listOf(MenuFixtures.coke, MenuFixtures.tea, MenuFixtures.vetkoek, MenuFixtures.sweets)

    @Test
    fun `a cold day lifts hot items, keeping each group in menu order`() {
        val sorted = menu.sortedForWeather(WeatherMood.Cold).map { it.slug }
        // vetkoek (sort 20) and tea (70) are hot; coke (90) is cold; sweets has neither.
        assertEquals(listOf("vetkoek", "tea", "sweets", "coke"), sorted)
    }

    @Test
    fun `a hot day lifts cold items`() {
        assertEquals("coke", menu.sortedForWeather(WeatherMood.Hot).first().slug)
    }

    @Test
    fun `an ordinary day leaves the tuckshop's own order alone`() {
        assertEquals(
            menu.sortedBy { it.sortOrder }.map { it.slug },
            menu.sortedForWeather(WeatherMood.Neutral).map { it.slug },
        )
    }

    // --- WMO codes ------------------------------------------------------------

    @Test
    fun `codes become the words a student reads`() {
        assertEquals(Sky.Clear, Sky.from(0))
        assertEquals(Sky.PartlyCloudy, Sky.from(3))
        assertEquals(Sky.Fog, Sky.from(48))
        assertEquals(Sky.Drizzle, Sky.from(55))
        assertEquals(Sky.Rain, Sky.from(65))
        assertEquals(Sky.Showers, Sky.from(81))
        assertEquals(Sky.Thunderstorm, Sky.from(99))
        // Anything unexpected is described vaguely rather than wrongly.
        assertEquals(Sky.PartlyCloudy, Sky.from(7734))
    }

    // --- The cache ------------------------------------------------------------

    private val json = NetworkModule.json()
    private val forecast = json.decodeFromString<ForecastDto>(
        """{"current":{"temperature_2m":13.4,"weather_code":61,"precipitation":0.4},
        "daily":{"temperature_2m_max":[16.0,29.5],"temperature_2m_min":[7.0,15.0],
        "precipitation_probability_max":[70,10],"weather_code":[61,0]}}"""
    )

    private class FakeWeatherDao : WeatherDao {
        var row: WeatherEntity? = null
        override suspend fun get() = row
        override suspend fun put(weather: WeatherEntity) {
            row = weather
        }
    }

    private fun repository(dao: WeatherDao, api: WeatherApi, nowMillis: Long) = WeatherRepository(
        api = api,
        dao = dao,
        clock = Clock.fixed(Instant.ofEpochMilli(nowMillis), ZoneOffset.UTC),
        io = UnconfinedTestDispatcher(),
    )

    @Test
    fun `a fetched forecast is read correctly and stored`() = runTest {
        val dao = FakeWeatherDao()
        val api = mockk<WeatherApi> { coEvery { forecast(any(), any(), any(), any(), any(), any()) } returns forecast }

        val result = repository(dao, api, nowMillis = 1_000_000).weather()!!

        assertEquals(13.4, result.temperatureC, 0.01)
        assertEquals(Sky.Rain, result.sky)
        assertEquals(70, result.rainProbabilityToday)
        assertEquals(WeatherMood.Cold, result.mood)
        assertEquals(29.5, result.tomorrowMaxC, 0.01)
        assertEquals(WeatherMood.Hot, result.tomorrowMood)
        assertEquals(13.4, dao.row!!.temperatureC, 0.01)
    }

    @Test
    fun `within the hour the cache answers and nothing is fetched`() = runTest {
        val dao = FakeWeatherDao().apply {
            row = WeatherEntity(
                temperatureC = 11.0, weatherCode = 0, rainProbabilityToday = 0,
                tomorrowMaxC = 20.0, tomorrowMinC = 9.0, tomorrowRainProbability = 0,
                tomorrowCode = 0, fetchedAt = 1_000_000,
            )
        }
        val api = mockk<WeatherApi>()

        // 59 minutes later.
        val result = repository(dao, api, 1_000_000 + 59 * 60_000).weather()!!

        assertEquals(11.0, result.temperatureC, 0.01)
        coVerify(exactly = 0) { api.forecast(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `after an hour it fetches again`() = runTest {
        val dao = FakeWeatherDao().apply {
            row = WeatherEntity(
                temperatureC = 11.0, weatherCode = 0, rainProbabilityToday = 0,
                tomorrowMaxC = 20.0, tomorrowMinC = 9.0, tomorrowRainProbability = 0,
                tomorrowCode = 0, fetchedAt = 1_000_000,
            )
        }
        val api = mockk<WeatherApi> { coEvery { forecast(any(), any(), any(), any(), any(), any()) } returns forecast }

        val result = repository(dao, api, 1_000_000 + 61 * 60_000).weather()!!
        assertEquals(13.4, result.temperatureC, 0.01)
    }

    @Test
    fun `a failed call falls back to the stale copy rather than nothing`() = runTest {
        val dao = FakeWeatherDao().apply {
            row = WeatherEntity(
                temperatureC = 11.0, weatherCode = 0, rainProbabilityToday = 0,
                tomorrowMaxC = 20.0, tomorrowMinC = 9.0, tomorrowRainProbability = 0,
                tomorrowCode = 0, fetchedAt = 1_000_000,
            )
        }
        val api = mockk<WeatherApi> {
            coEvery { forecast(any(), any(), any(), any(), any(), any()) } throws java.io.IOException("no signal")
        }

        val result = repository(dao, api, 1_000_000 + 120 * 60_000).weather()
        assertEquals(11.0, result!!.temperatureC, 0.01)
    }

    @Test
    fun `no cache and no signal is simply no weather, not an error`() = runTest {
        val api = mockk<WeatherApi> {
            coEvery { forecast(any(), any(), any(), any(), any(), any()) } throws java.io.IOException("no signal")
        }
        assertNull(repository(FakeWeatherDao(), api, 1_000_000).weather())
    }
}
