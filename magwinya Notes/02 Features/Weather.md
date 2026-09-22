# Weather

Back to [[Ri-magwinya]]

The menu reorders itself based on what it's doing outside. Small feature, and
the one people notice.

## Why

Nobody wants a Coke on a cold wet morning in Bloemfontein, and nobody wants
tea when it's 32°C. On a cold day the thing most students want is near the
bottom of a list sorted by something arbitrary.

So on a cold day, hot things move up.

## Where the data comes from

[Open-Meteo](https://open-meteo.com). Free, no API key, no account, no
registration. That last part matters — it's one fewer checkpoint for Dylan and
one fewer secret to keep out of git.

```http
GET https://api.open-meteo.com/v1/forecast
  ?latitude=-29.12&longitude=26.21
  &current=temperature_2m,weather_code,precipitation
  &daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max
  &timezone=Africa/Johannesburg
  &forecast_days=2
```

Those coordinates are Bloemfontein, hardcoded in `AppConfig`. It's one campus.

`forecast_days=2` because today drives the menu and tomorrow drives the staff
stock hint.

## The rules

| Condition | What happens |
|---|---|
| Current temp **below 15°C**, or today's rain chance **50% or more** | `hot` items sort first |
| Current temp **27°C or above** | `cold` items sort first |
| Anything else | Normal `sort_order` |

Cold is checked first, so a cold *and* rainy day is a hot-drinks day.

Every menu item has a `temperature_tag` of `hot`, `cold`, or nothing. Tea and
coffee and the boerewors roll are hot; every can and bottle is cold; Doritos
are neither and never move.

It's a **re-sort, not a filter**. Nothing disappears and nothing is hidden —
you can still buy a Coke in the rain, it's just further down.

## On screen

**The menu strip**, a thin line under the greeting:

> 14°C · Light rain · Hot drinks first today

When the rule didn't fire it's just the reading, with no claim about ordering.

**The rain banner**, at 50% or more:

> Rain expected. Order ahead so you're not queueing outside.

Which is the actual sales pitch for the whole app, delivered on the one day
it's most obviously true.

**The staff forecast card**, on the sales screen, showing tomorrow:

> Cold tomorrow. Check tea and coffee stock.

Same thresholds, one day forward. It's a nudge, not a stock order — it tells
staff what to look at, not what to buy.

## WMO codes

Open-Meteo returns a number, not a description. The full WMO table is about
thirty entries and most of them never happen in the Free State, so they map
down to seven short labels:

| Codes | Label |
|---|---|
| 0, 1 | Clear |
| 2, 3 | Partly cloudy |
| 45, 48 | Fog |
| 51, 53, 55 | Drizzle |
| 61, 63, 65 | Rain |
| 80, 81, 82 | Showers |
| 95, 96, 99 | Thunderstorm |

Anything unmapped falls back to the temperature alone rather than showing a
number nobody can read.

## The cache

Room, **60 minutes**.

The weather doesn't change meaningfully inside an hour, and the menu gets
opened a dozen times a day per student. Without the cache that's a dozen
network calls for information that's identical each time.

It also means the strip still appears offline, showing whatever was last
fetched. See [[Offline mode]].

## It must never hold up the menu

The most important rule here.

The weather call is fired off **alongside** the menu load, not before it. If
it's slow, fails, times out, or Open-Meteo is down, the menu renders on its
own schedule in default order and the strip simply isn't there.

No spinner waiting on it. No error banner. No retry prompt. A student opening
the app to buy a vetkoek should never wait on a weather API, and should
certainly never see an error about one.

## Testing it

The thresholds, the code mapping and the cache expiry are all unit tested with
a fake repository, which is the only sane way to test "what happens when it's
28°C" in September.

The Phase 14 acceptance test is exactly that: feed the menu a fake 12°C and
assert tea is at the top. See [[Testing and CI]].
