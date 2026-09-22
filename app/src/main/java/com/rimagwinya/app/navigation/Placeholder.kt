package com.rimagwinya.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.EmptyState
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space

/**
 * A titled stand-in for a screen a later phase builds.
 *
 * Deliberately not a blank Composable: having every route reachable and
 * labelled from Phase 1 means navigation is testable before any of the
 * screens exist.
 */
@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    @androidx.annotation.DrawableRes icon: Int = R.drawable.ic_box,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Space.screen),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(titleRes),
            style = RmTheme.type.largeTitle,
            color = RmTheme.colors.text,
        )
        EmptyState(
            title = stringResource(titleRes),
            body = stringResource(R.string.placeholder_coming),
            icon = icon,
        )
    }
}
