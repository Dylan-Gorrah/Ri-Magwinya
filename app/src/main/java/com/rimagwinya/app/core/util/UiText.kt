package com.rimagwinya.app.core.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A message a ViewModel wants shown, as a resource plus arguments rather
 * than a finished sentence, so the words stay in strings.xml and follow the
 * app's language.
 */
data class UiText(@StringRes val res: Int, val args: List<Any> = emptyList()) {
    constructor(@StringRes res: Int, vararg args: Any) : this(res, args.toList())
}

@Composable
fun UiText.resolve(): String = stringResource(res, *args.toTypedArray())
