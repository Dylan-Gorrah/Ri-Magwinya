package com.rimagwinya.app.feature.staff

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.rimagwinya.app.R
import com.rimagwinya.app.domain.model.SalesSummary

/**
 * Orders by hour.
 *
 * The busiest hour is named in text beside the chart as well, because a
 * highlighted bar on its own says nothing to someone who cannot see it.
 */
@Composable
fun OrdersByHourChart(summary: SalesSummary) {
    val producer = remember { CartesianChartModelProducer() }

    LaunchedEffect(summary) {
        producer.runTransaction {
            columnSeries {
                series(summary.byHour.map { it.hour }, summary.byHour.map { it.orders })
            }
        }
    }

    val peak = summary.peak
    val description = stringResource(
        R.string.sales_chart_description,
        peak?.label ?: "",
        peak?.orders ?: 0,
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { _, value, _ -> "%02d".format(value.toInt()) },
            ),
        ),
        modelProducer = producer,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = description },
    )
}
