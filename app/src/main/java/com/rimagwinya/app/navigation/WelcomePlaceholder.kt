package com.rimagwinya.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.BrandMark
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.designsystem.theme.Stroke
import androidx.compose.ui.unit.dp

/**
 * A stand-in for the real welcome screen, which Phase 3 builds.
 *
 * The two role cards are part of the real design, but note what they do in
 * the finished app: they only choose which sign-in copy you see. The role
 * itself comes from the server. Here, with no auth yet, tapping one walks
 * straight into that side of the app so every placeholder is reachable —
 * and that shortcut disappears entirely in Phase 3.
 */
@Composable
fun WelcomePlaceholder(
    onSignedIn: (AppRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = RmTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Space.screen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(size = 72.dp)
        Column(
            Modifier.padding(top = Space.x20, bottom = Space.x40),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.x8),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = RmTheme.type.largeTitle,
                color = c.text,
            )
            Text(
                stringResource(R.string.welcome_tagline),
                style = RmTheme.type.secondary,
                color = c.text2,
            )
        }

        RoleCard(
            title = stringResource(R.string.welcome_student),
            body = stringResource(R.string.welcome_student_body),
            onClick = { onSignedIn(AppRole.Student) },
        )
        Column(Modifier.padding(top = Space.x12)) {
            RoleCard(
                title = stringResource(R.string.welcome_staff),
                body = stringResource(R.string.welcome_staff_body),
                onClick = { onSignedIn(AppRole.Staff) },
            )
        }
    }
}

@Composable
private fun RoleCard(title: String, body: String, onClick: () -> Unit) {
    val c = RmTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.r16))
            .background(c.surface)
            .border(Stroke.hairline, c.border, RoundedCornerShape(Radius.r16))
            .clickable(onClick = onClick)
            .padding(Space.x20),
        verticalArrangement = Arrangement.spacedBy(Space.x4),
    ) {
        Text(title, style = RmTheme.type.bodyStrong, color = c.text)
        Text(body, style = RmTheme.type.secondary, color = c.text2)
    }
}

@Preview(name = "Welcome", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun WelcomePreview() = RimagwinyaTheme {
    WelcomePlaceholder(onSignedIn = {})
}
