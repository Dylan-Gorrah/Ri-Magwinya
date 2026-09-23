package com.rimagwinya.app.feature.staff

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.config.AppConfig
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.Chip
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.component.RmBottomSheet
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmTextField
import com.rimagwinya.app.core.designsystem.component.SectionLabel
import com.rimagwinya.app.core.designsystem.component.SkeletonRow
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.util.resolve
import com.rimagwinya.app.domain.model.StudentAccount
import com.rimagwinya.app.domain.model.WalletEntry
import com.rimagwinya.app.domain.model.WalletEntryType
import java.time.format.DateTimeFormatter

@Composable
fun TopUpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        RmAppBar(title = stringResource(R.string.topup_title), onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            verticalArrangement = Arrangement.spacedBy(Space.x20),
        ) {
            // The speed point and the app are separate systems. Say so.
            Banner(text = stringResource(R.string.topup_explainer), tone = BannerTone.Info)

            val student = state.student
            if (student == null) {
                SearchStep(state, viewModel)
            } else {
                StudentStep(student, state, viewModel)
            }

            state.error?.let { Banner(text = it.resolve(), tone = BannerTone.Error) }
        }
    }

    if (state.confirming) {
        RmBottomSheet(onDismiss = viewModel::dismissConfirm) {
            Column(Modifier.padding(Space.screen), verticalArrangement = Arrangement.spacedBy(Space.x16)) {
                Text(
                    stringResource(R.string.topup_confirm_title, state.amount?.format().orEmpty(), state.student?.fullName.orEmpty()),
                    style = RmTheme.type.screenTitle,
                    color = RmTheme.colors.text,
                )
                Text(stringResource(R.string.topup_confirm_body), style = RmTheme.type.body, color = RmTheme.colors.text2)
                RmButton(stringResource(R.string.topup_confirm), viewModel::confirm)
                RmButton(stringResource(R.string.topup_not_yet), viewModel::dismissConfirm, style = RmButtonStyle.Secondary)
            }
        }
    }
}

/** Type a number or a name, then pick from what matches. */
@Composable
private fun SearchStep(state: TopUpUiState, viewModel: TopUpViewModel) {
    RmTextField(
        value = state.query,
        onValueChange = viewModel::onQuery,
        label = stringResource(R.string.topup_student_number),
        keyboardType = KeyboardType.Ascii,
        leadingIcon = R.drawable.ic_search,
    )
    RmButton(
        text = stringResource(if (state.searching) R.string.topup_finding else R.string.topup_find),
        onClick = viewModel::search,
        enabled = state.canSearch,
        style = RmButtonStyle.Secondary,
    )

    val results = state.results ?: return
    if (results.isEmpty()) {
        Banner(text = stringResource(R.string.topup_no_results), tone = BannerTone.Warning)
        return
    }
    Column {
        SectionLabel(stringResource(R.string.topup_results))
        GroupedList {
            results.forEachIndexed { index, student ->
                if (index > 0) ListDivider()
                ListRow(
                    title = student.fullName,
                    subtitle = student.studentNumber,
                    onClick = { viewModel.select(student) },
                    trailing = {
                        Text(student.balance.format(), style = RmTheme.type.secondary, color = RmTheme.colors.text2)
                    },
                )
            }
        }
    }
}

/** Who the student is, what moved through their wallet, and the top-up. */
@Composable
private fun StudentStep(student: StudentAccount, state: TopUpUiState, viewModel: TopUpViewModel) {
    state.added?.let { added ->
        Banner(text = stringResource(R.string.topup_added, added.format(), student.balance.format()), tone = BannerTone.Success)
    }

    Column {
        SectionLabel(stringResource(R.string.topup_details))
        GroupedList {
            ListRow(title = student.fullName, subtitle = student.studentNumber, showChevron = false)
            ListDivider()
            DetailRow(stringResource(R.string.topup_balance), student.balance.format())
            ListDivider()
            DetailRow(stringResource(R.string.topup_email), student.email)
            ListDivider()
            DetailRow(stringResource(R.string.topup_phone), student.phone ?: stringResource(R.string.profile_phone_none))
            ListDivider()
            DetailRow(stringResource(R.string.topup_no_shows), student.noShowCount.toString())
        }
    }
    RmButton(stringResource(R.string.topup_change), viewModel::changeStudent, style = RmButtonStyle.Quiet)

    Column(verticalArrangement = Arrangement.spacedBy(Space.x12)) {
        SectionLabel(stringResource(R.string.topup_amount))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.x8)) {
            AppConfig.TOP_UP_PRESETS.forEach { preset ->
                Chip(
                    text = preset.format().removeSuffix(".00"),
                    selected = state.preset == preset && state.custom.isBlank(),
                    onClick = { viewModel.choosePreset(preset) },
                )
            }
        }
        RmTextField(
            value = state.custom,
            onValueChange = viewModel::onCustom,
            label = stringResource(R.string.topup_custom),
            keyboardType = KeyboardType.Decimal,
            error = if (state.custom.isNotBlank() && !state.amountInRange) {
                stringResource(R.string.topup_range, AppConfig.MIN_TOP_UP.format(), AppConfig.MAX_TOP_UP.format())
            } else {
                null
            },
        )
    }

    RmButton(
        text = if (state.adding) {
            stringResource(R.string.topup_adding)
        } else {
            stringResource(R.string.topup_add, state.amount?.format() ?: "")
        },
        onClick = viewModel::askConfirm,
        enabled = state.canAdd,
    )

    Column {
        SectionLabel(stringResource(R.string.topup_history))
        when {
            state.historyLoading && state.history.isEmpty() -> GroupedList { SkeletonRow(); SkeletonRow() }
            state.history.isEmpty() -> Text(
                stringResource(R.string.topup_history_empty),
                style = RmTheme.type.secondary,
                color = RmTheme.colors.text3,
                modifier = Modifier.padding(start = Space.x4),
            )
            else -> GroupedList {
                state.history.forEachIndexed { index, entry ->
                    if (index > 0) ListDivider()
                    HistoryRow(entry)
                }
            }
        }
    }

    RmButton(stringResource(R.string.topup_another), viewModel::reset, style = RmButtonStyle.Secondary)
}

@Composable
private fun DetailRow(label: String, value: String) {
    ListRow(
        title = label,
        showChevron = false,
        trailing = { Text(value, style = RmTheme.type.secondary, color = RmTheme.colors.text2) },
    )
}

private val HISTORY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@Composable
private fun HistoryRow(entry: WalletEntry) {
    val label = when (entry.type) {
        WalletEntryType.TopUp -> R.string.wallet_entry_topup
        WalletEntryType.Payment -> R.string.wallet_entry_payment
        WalletEntryType.Refund -> R.string.wallet_entry_refund
    }
    val amount = entry.amount.format().let { if (entry.amount.isPositive) "+$it" else it }
    ListRow(
        title = stringResource(label),
        subtitle = HISTORY_TIME.format(entry.at.atZone(AppConfig.zone)),
        showChevron = false,
        trailing = {
            Text(
                amount,
                style = RmTheme.type.bodyStrong,
                // Money in is green; the sign says the same thing in text.
                color = if (entry.amount.isPositive) RmTheme.colors.success else RmTheme.colors.text,
            )
        },
    )
}
