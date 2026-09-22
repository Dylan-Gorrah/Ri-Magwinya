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
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.component.RmBottomSheet
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmCard
import com.rimagwinya.app.core.designsystem.component.RmTextField
import com.rimagwinya.app.core.designsystem.component.SectionLabel
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.util.resolve

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

            val done = state.done
            if (done != null) {
                Banner(
                    text = stringResource(R.string.topup_done, done.fullName, done.balance.format()),
                    tone = BannerTone.Success,
                )
                RmButton(stringResource(R.string.topup_another), viewModel::reset)
                return@Column
            }

            RmTextField(
                value = state.number,
                onValueChange = viewModel::onNumber,
                label = stringResource(R.string.topup_student_number),
                keyboardType = KeyboardType.Ascii,
            )
            RmButton(
                text = stringResource(if (state.finding) R.string.topup_finding else R.string.topup_find),
                onClick = viewModel::find,
                enabled = state.canFind,
                style = RmButtonStyle.Secondary,
            )

            state.student?.let { student ->
                RmCard {
                    Text(student.fullName, style = RmTheme.type.bodyStrong, color = RmTheme.colors.text)
                    Text(student.studentNumber, style = RmTheme.type.secondary, color = RmTheme.colors.text2)
                    Text(
                        stringResource(R.string.topup_current, student.balance.format()),
                        style = RmTheme.type.secondary,
                        color = RmTheme.colors.text2,
                    )
                }

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
