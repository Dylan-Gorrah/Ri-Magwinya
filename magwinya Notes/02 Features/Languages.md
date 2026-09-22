# Languages

Back to [[Ri-magwinya]]

English, Afrikaans and Sesotho. Switchable inside the app, without touching
the phone's own language. Phase 13, and one of the four the PoE marks
separately.

## Why those three

Bloemfontein. English and Afrikaans are everywhere, and Sesotho is the
majority home language in the Free State. Three covers nearly everyone on that
campus.

## The rule that makes it possible

**No user-facing string is ever typed into the code.** Everything goes into
`strings.xml`, from the very first screen in Phase 1.

```kotlin
// no
Text("Add to cart")

// yes
Text(stringResource(R.string.cart_add))
```

This is enforced from Phase 1 even though translation doesn't happen until
Phase 13, and that ordering is the entire point. Done this way, Phase 13 is
"translate 200 strings". Done the other way, it's "find 200 strings scattered
through 40 files, extract them, then translate them" — the same work plus a
refactor plus the bugs the refactor introduces.

Three files by the end:

```
res/values/strings.xml      English
res/values-af/strings.xml   Afrikaans
res/values-st/strings.xml   Sesotho
```

## Per-app language

Android 13 added a proper per-app language setting. The app uses it:

```kotlin
AppCompatDelegate.setApplicationLocales(
    LocaleListCompat.forLanguageTags("af")
)
```

The whole UI switches immediately — no restart, no "changes take effect next
time".

Picking Afrikaans in this app doesn't change the phone. The student's WhatsApp
stays as it was.

### Making it work below Android 13

Min SDK is 24, so most of the phones this runs on don't have the system
feature. AppCompat backports it, but it needs four things wired up:

1. `res/xml/locales_config.xml` listing `en`, `af`, `st`
2. `android:localeConfig="@xml/locales_config"` in the manifest
3. `AppLocalesMetadataHolderService` declared with `autoStoreLocales`, which
   is what persists the choice on old versions
4. **`MainActivity` extends `AppCompatActivity`** with an AppCompat DayNight
   theme

Number 4 is why the app uses `AppCompatActivity` rather than
`ComponentActivity`, in an app that is otherwise entirely Compose. It looks
like a mistake in the architecture until you know this is the reason.

It also has a sharp edge: **`AppCompatActivity` crashes at startup without an
AppCompat theme.** One of the listed gotchas in the brief, and the kind of
thing that costs an afternoon.

## Picking a language

Profile → Settings → Language. Three options, current one ticked.

Saved in two places:

- **DataStore**, so the app knows on next launch before any network call
- **`profiles.language`** on the server, so it follows the student to a new
  phone and so future server-sent notifications can be in the right language

```http
PATCH /rest/v1/profiles?id=eq.<id>
{ "language": "af" }
```

The column is one of only three a student may update on their own profile.
See [[Security rules]].

## Plurals and formatting

Not string concatenation. Concatenating words works in English and falls apart
everywhere else.

```xml
<plurals name="cart_items">
    <item quantity="one">%d item</item>
    <item quantity="other">%d items</item>
</plurals>
```

Prices go through the shared `Money` formatter rather than being glued
together, so `R12.50` is produced in one place and can be adjusted for all
three languages at once if it ever needs to be. See
[[The rules that matter]].

Numbers to watch: the stock pills (*4 left*), the slot chips (*12 of 40
taken*), the loyalty counter (*7 of 10*), and the option labels (*2 vetkoeks,
2 Polony*).

## The checkpoint nobody should skip

**A first-language speaker has to read the Afrikaans and Sesotho.**

Machine translation will be wrong. Not obviously wrong — subtly wrong, in the
way that makes an app feel like it wasn't made for you. Food words especially:
*vetkoek*, *boerewors* and *magwinya* all carry meaning that a translator will
happily mangle.

A Phase 13 checkpoint. See [[What Dylan has to do himself]].

## The name

*Magwinya* is what vetkoek is called in Sesotho, Setswana and Zulu. The app is
named in one of the languages it's translated into, which is a better reason
to get the Sesotho right than any of the above.
