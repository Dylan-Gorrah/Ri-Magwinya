package com.rimagwinya.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.BrandMark
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.designsystem.theme.Stroke

/**
 * The first screen.
 *
 * The two role cards **choose which sign-in copy you see and nothing else.**
 * Your actual role comes from your row in the database when you sign in, so
 * tapping "I work at the tuckshop" with a student account lands you on the
 * student menu.
 *
 * The prototype set the role from the card. That was a demo shortcut, and so
 * was the version of this screen that Phase 1 shipped, which signed you
 * straight in. Both are gone.
 */
@Composable
fun WelcomeScreen(
    onContinue: (staffHint: Boolean) -> Unit,
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
        BrandMark(size = 76.dp)

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
            icon = R.drawable.ic_user,
            onClick = { onContinue(false) },
        )

        Column(Modifier.padding(top = Space.x12)) {
            RoleCard(
                title = stringResource(R.string.welcome_staff),
                body = stringResource(R.string.welcome_staff_body),
                icon = R.drawable.ic_store,
                onClick = { onContinue(true) },
            )
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    body: String,
    @androidx.annotation.DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    val c = RmTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.r16))
            .background(c.surface)
            .border(Stroke.hairline, c.border, RoundedCornerShape(Radius.r16))
            .clickable(onClick = onClick)
            .padding(Space.x20),
        horizontalArrangement = Arrangement.spacedBy(Space.x16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = c.sky,
            modifier = Modifier.size(22.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Space.x4)) {
            Text(title, style = RmTheme.type.bodyStrong, color = c.text)
            Text(body, style = RmTheme.type.secondary, color = c.text2)
        }
    }
}

@Preview(name = "Welcome", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun WelcomePreview() = RimagwinyaTheme {
    WelcomeScreen(onContinue = {})
}
