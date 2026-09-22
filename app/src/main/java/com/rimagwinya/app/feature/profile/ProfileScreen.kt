package com.rimagwinya.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.rimagwinya.app.core.designsystem.component.RmCard
import com.rimagwinya.app.core.designsystem.component.RmTextField
import com.rimagwinya.app.core.designsystem.component.SectionLabel
import com.rimagwinya.app.core.designsystem.component.ToggleRow
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.designsystem.theme.ThemeChoice
import com.rimagwinya.app.core.util.resolve

@Composable
fun ProfileScreen(
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricsAvailable = rememberBiometricsAvailable(context)

    Column(modifier.fillMaxSize()) {
        RmAppBar(title = stringResource(R.string.title_profile))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            verticalArrangement = Arrangement.spacedBy(Space.x20),
        ) {
            state.message?.let {
                Banner(text = it.resolve(), tone = BannerTone.Success)
            }

            // Identity
            RmCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.x12)) {
                    Icon(
                        painterResource(if (state.isStaff) R.drawable.ic_store else R.drawable.ic_user),
                        contentDescription = null,
                        tint = if (state.isStaff) RmTheme.colors.gold else RmTheme.colors.sky,
                        modifier = Modifier.size(28.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(state.profile?.fullName.orEmpty(), style = RmTheme.type.bodyStrong, color = RmTheme.colors.text)
                        Text(state.profile?.email.orEmpty(), style = RmTheme.type.caption, color = RmTheme.colors.text2)
                        Text(
                            state.profile?.studentNumber?.let { stringResource(R.string.profile_student_number, it) }
                                ?: stringResource(R.string.profile_no_student_number),
                            style = RmTheme.type.caption,
                            color = RmTheme.colors.text2,
                        )
                    }
                    Text(
                        stringResource(if (state.isStaff) R.string.profile_staff else R.string.profile_student),
                        style = RmTheme.type.label,
                        color = if (state.isStaff) RmTheme.colors.gold else RmTheme.colors.sky,
                    )
                }
            }

            if (!state.isStaff) {
                RmCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.profile_wallet),
                            style = RmTheme.type.bodyStrong,
                            color = RmTheme.colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            state.profile?.walletBalance?.format().orEmpty(),
                            style = RmTheme.type.price,
                            color = RmTheme.colors.text,
                        )
                    }
                }
                RmCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.profile_loyalty), style = RmTheme.type.bodyStrong, color = RmTheme.colors.text)
                            Text(stringResource(R.string.profile_loyalty_note), style = RmTheme.type.caption, color = RmTheme.colors.text2)
                        }
                        Text(
                            stringResource(R.string.profile_loyalty_progress, state.stampProgress, AppConfig.LOYALTY_TARGET),
                            style = RmTheme.type.label,
                            color = RmTheme.colors.gold,
                        )
                    }
                }
            }

            // Preferences
            Column {
                SectionLabel(stringResource(R.string.profile_preferences))
                GroupedList {
                    ToggleRow(
                        title = stringResource(R.string.profile_notifications),
                        subtitle = stringResource(R.string.profile_notifications_body),
                        checked = state.settings.orderNotifications,
                        onCheckedChange = viewModel::setNotifications,
                    )
                    ListDivider()
                    ToggleRow(
                        title = stringResource(R.string.profile_biometric),
                        subtitle = if (biometricsAvailable) {
                            stringResource(R.string.profile_biometric_body)
                        } else {
                            stringResource(R.string.profile_biometric_unavailable)
                        },
                        checked = state.settings.biometricUnlock && biometricsAvailable,
                        onCheckedChange = viewModel::setBiometric,
                        enabled = biometricsAvailable,
                    )
                }
            }

            Column {
                SectionLabel(stringResource(R.string.profile_theme))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x8)) {
                    ThemeChoice.entries.forEach { choice ->
                        Chip(
                            text = stringResource(choice.labelRes()),
                            selected = state.settings.theme == choice,
                            onClick = { viewModel.setTheme(choice) },
                        )
                    }
                }
            }

            Column {
                SectionLabel(stringResource(R.string.profile_language))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x8)) {
                    LANGUAGES.forEach { (code, label) ->
                        Chip(
                            text = label,
                            selected = state.settings.language == code,
                            onClick = { viewModel.setLanguage(code) },
                        )
                    }
                }
            }

            // Account
            Column {
                SectionLabel(stringResource(R.string.profile_account))
                GroupedList {
                    ListRow(
                        title = stringResource(R.string.profile_change_password),
                        onClick = viewModel::openPasswordSheet,
                    )
                    ListDivider()
                    ListRow(title = stringResource(R.string.profile_privacy), onClick = onOpenPrivacy)
                }
            }

            RmButton(stringResource(R.string.profile_sign_out), { viewModel.signOut() }, style = RmButtonStyle.Danger)
        }
    }

    state.passwordForm?.let { form ->
        RmBottomSheet(onDismiss = viewModel::closePasswordSheet) {
            Column(Modifier.padding(Space.screen), verticalArrangement = Arrangement.spacedBy(Space.x16)) {
                Text(stringResource(R.string.profile_change_password), style = RmTheme.type.screenTitle, color = RmTheme.colors.text)
                RmTextField(
                    value = form.password,
                    onValueChange = { v -> viewModel.editPassword { it.copy(password = v) } },
                    label = stringResource(R.string.password_new),
                    isPassword = true,
                    leadingIcon = R.drawable.ic_lock,
                )
                RmTextField(
                    value = form.repeat,
                    onValueChange = { v -> viewModel.editPassword { it.copy(repeat = v) } },
                    label = stringResource(R.string.password_repeat),
                    isPassword = true,
                    leadingIcon = R.drawable.ic_lock,
                    error = if (form.repeat.isNotEmpty() && form.repeat != form.password) {
                        stringResource(R.string.password_mismatch)
                    } else {
                        null
                    },
                )
                form.error?.let { Banner(text = it.resolve(), tone = BannerTone.Error) }
                RmButton(
                    text = stringResource(if (form.saving) R.string.password_saving else R.string.password_save),
                    onClick = viewModel::savePassword,
                    enabled = form.canSave,
                )
            }
        }
    }
}

/** Language codes and the name each one calls itself. */
private val LANGUAGES = listOf("en" to "English", "af" to "Afrikaans", "st" to "Sesotho")

private fun ThemeChoice.labelRes(): Int = when (this) {
    ThemeChoice.System -> R.string.profile_theme_system
    ThemeChoice.Light -> R.string.profile_theme_light
    ThemeChoice.Dark -> R.string.profile_theme_dark
}

@Composable
private fun rememberBiometricsAvailable(context: android.content.Context): Boolean =
    androidx.compose.runtime.remember(context) { canUseBiometrics(context) }
