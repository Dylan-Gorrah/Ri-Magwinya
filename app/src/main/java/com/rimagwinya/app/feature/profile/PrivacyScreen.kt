package com.rimagwinya.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space

/**
 * The POPIA page. The same words go in the store listing in Phase 16, so
 * they live in strings.xml and are written for a student to read rather than
 * for a lawyer to file.
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        RmAppBar(title = stringResource(R.string.privacy_title), onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            verticalArrangement = Arrangement.spacedBy(Space.x16),
        ) {
            Text(
                stringResource(R.string.privacy_intro),
                style = RmTheme.type.body,
                color = RmTheme.colors.text2,
            )
            SECTIONS.forEach { (heading, body) ->
                Column(verticalArrangement = Arrangement.spacedBy(Space.x4)) {
                    Text(stringResource(heading), style = RmTheme.type.bodyStrong, color = RmTheme.colors.text)
                    Text(stringResource(body), style = RmTheme.type.body, color = RmTheme.colors.text2)
                }
            }
        }
    }
}

private val SECTIONS = listOf(
    R.string.privacy_h_what to R.string.privacy_b_what,
    R.string.privacy_h_why to R.string.privacy_b_why,
    R.string.privacy_h_cards to R.string.privacy_b_cards,
    R.string.privacy_h_who to R.string.privacy_b_who,
    R.string.privacy_h_long to R.string.privacy_b_long,
    R.string.privacy_h_rights to R.string.privacy_b_rights,
    R.string.privacy_h_security to R.string.privacy_b_security,
)
