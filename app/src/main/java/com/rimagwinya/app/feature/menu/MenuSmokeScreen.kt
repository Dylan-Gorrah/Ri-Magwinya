package com.rimagwinya.app.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.EmptyState
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.SkeletonRow
import com.rimagwinya.app.core.designsystem.component.StockPill
import com.rimagwinya.app.core.designsystem.component.StockState
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.domain.model.MenuItem

/**
 * Phase 2's proof that the backend works: the real menu, from Supabase, on
 * the phone.
 *
 * Deliberately plain — no hero, no search, no categories, no weather. Phase 4
 * replaces this file with the real menu screen. What it does exercise is the
 * whole chain: Retrofit, the auth headers, Row Level Security letting an
 * anonymous read through, the nested option query, and the mapping into
 * domain types with prices as integer cents.
 */
@Composable
fun MenuSmokeScreen(
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Space.screen),
        verticalArrangement = Arrangement.spacedBy(Space.x16),
    ) {
        Text(
            text = stringResource(R.string.title_menu),
            style = RmTheme.type.largeTitle,
            color = RmTheme.colors.text,
            modifier = Modifier.padding(top = Space.x20),
        )

        when {
            state.notConfigured -> Banner(
                tone = BannerTone.Warning,
                title = "Backend not configured",
                text = "Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties, " +
                    "then rebuild.",
            )

            state.isLoading -> GroupedList {
                repeat(6) { index ->
                    SkeletonRow()
                    if (index < 5) ListDivider()
                }
            }

            state.error != null -> Column(
                verticalArrangement = Arrangement.spacedBy(Space.x12)
            ) {
                Banner(tone = BannerTone.Error, text = state.error.orEmpty())
                RmButton(
                    text = stringResource(R.string.action_retry),
                    onClick = viewModel::load,
                    style = RmButtonStyle.Secondary,
                )
            }

            state.isEmpty -> EmptyState(
                title = "No items on the menu",
                body = "Nothing has been added yet.",
            )

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Space.x16),
            ) {
                items(state.items, key = { it.id }) { item ->
                    GroupedList { MenuRow(item) }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItem) {
    ListRow(
        title = item.name,
        subtitle = item.description,
        enabled = !item.isSoldOut,
        showChevron = false,
        trailing = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    // "from" pricing arrives properly in Phase 4 with the
                    // option engine; this is the plain item price.
                    text = item.price.format(),
                    style = RmTheme.type.price,
                    color = RmTheme.colors.text,
                )
                StockPill(
                    StockState.of(
                        quantity = item.stockQuantity,
                        reorderLevel = item.reorderLevel,
                        available = item.isAvailable,
                    )
                )
            }
        },
    )
}
