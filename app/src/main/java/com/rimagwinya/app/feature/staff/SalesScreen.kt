package com.rimagwinya.app.feature.staff

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmCard
import com.rimagwinya.app.core.designsystem.component.SectionLabel
import com.rimagwinya.app.core.designsystem.component.SkeletonRow
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.util.resolve
import com.rimagwinya.app.domain.model.SalesSummary
import com.rimagwinya.app.feature.menu.labelRes
import com.rimagwinya.app.feature.menu.tomorrowHintRes
import java.io.File
import java.time.LocalDate

@Composable
fun SalesScreen(
    modifier: Modifier = Modifier,
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier.fillMaxSize()) {
        RmAppBar(title = stringResource(R.string.title_sales)) {
            Text(stringResource(R.string.sales_today), style = RmTheme.type.secondary, color = RmTheme.colors.text2)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            verticalArrangement = Arrangement.spacedBy(Space.x20),
        ) {
            // Tomorrow, with the one thing staff can do about it.
            state.weather?.let { weather ->
                RmCard {
                    Text(
                        stringResource(R.string.weather_tomorrow),
                        style = RmTheme.type.label,
                        color = RmTheme.colors.text3,
                    )
                    Text(
                        stringResource(
                            R.string.weather_tomorrow_line,
                            Math.round(weather.tomorrowMinC).toInt(),
                            Math.round(weather.tomorrowMaxC).toInt(),
                            stringResource(weather.tomorrowSky.labelRes()),
                        ),
                        style = RmTheme.type.bodyStrong,
                        color = RmTheme.colors.text,
                    )
                    Text(
                        stringResource(weather.tomorrowHintRes()),
                        style = RmTheme.type.secondary,
                        color = RmTheme.colors.text2,
                    )
                }
            }

            val summary = state.summary
            when {
                summary == null && state.loading -> GroupedList { repeat(3) { i -> SkeletonRow(); if (i < 2) ListDivider() } }

                summary == null -> Column(verticalArrangement = Arrangement.spacedBy(Space.x12)) {
                    Banner(text = state.error?.resolve().orEmpty(), tone = BannerTone.Error)
                    RmButton(stringResource(R.string.action_retry), viewModel::load, style = RmButtonStyle.Secondary)
                }

                else -> SalesBody(
                    summary = summary,
                    onExport = { state.day?.let { share(context, it, summary) } },
                )
            }
        }
    }
}

@Composable
private fun SalesBody(summary: SalesSummary, onExport: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.x12)) {
        RmCard(Modifier.weight(1f)) {
            Text(stringResource(R.string.sales_revenue), style = RmTheme.type.caption, color = RmTheme.colors.text2)
            Text(summary.revenue.format(), style = RmTheme.type.screenTitle, color = RmTheme.colors.text)
            Text(
                pluralStringResource(R.plurals.sales_paid_orders, summary.orderCount, summary.orderCount),
                style = RmTheme.type.caption,
                color = RmTheme.colors.success,
            )
        }
        RmCard(Modifier.weight(1f)) {
            Text(stringResource(R.string.sales_average), style = RmTheme.type.caption, color = RmTheme.colors.text2)
            Text(summary.average.format(), style = RmTheme.type.screenTitle, color = RmTheme.colors.text)
            Text(stringResource(R.string.sales_all_slots), style = RmTheme.type.caption, color = RmTheme.colors.text2)
        }
    }

    RmCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.sales_by_hour),
                style = RmTheme.type.bodyStrong,
                color = RmTheme.colors.text,
                modifier = Modifier.weight(1f),
            )
            // The peak in words, not only as the highlighted bar.
            summary.peak?.let {
                Text(stringResource(R.string.sales_peak, it.label), style = RmTheme.type.secondary, color = RmTheme.colors.sky)
            }
        }
        OrdersByHourChart(summary)
    }

    Column {
        SectionLabel(stringResource(R.string.sales_top))
        if (summary.topItems.isEmpty()) {
            Text(stringResource(R.string.sales_none), style = RmTheme.type.secondary, color = RmTheme.colors.text2)
        } else {
            GroupedList {
                summary.topItems.take(5).forEachIndexed { index, item ->
                    ListRow(
                        title = "${index + 1}. ${item.name}",
                        showChevron = false,
                        trailing = {
                            Text(
                                stringResource(R.string.sales_sold, item.quantity),
                                style = RmTheme.type.secondary,
                                // Gold for the top seller only.
                                color = if (index == 0) RmTheme.colors.gold else RmTheme.colors.text2,
                            )
                        },
                    )
                    if (index < minOf(summary.topItems.size, 5) - 1) ListDivider(inset = false)
                }
            }
        }
    }

    RmButton(stringResource(R.string.sales_export), onExport, style = RmButtonStyle.Secondary)
}

/** Writes the CSV to the cache and opens the share sheet with it. */
private fun share(context: Context, day: LocalDate, summary: SalesSummary) {
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, "rimagwinya-sales-$day.csv").apply { writeText(SalesViewModel.csv(day, summary)) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/csv")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, file.name)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.sales_export_title)))
}
