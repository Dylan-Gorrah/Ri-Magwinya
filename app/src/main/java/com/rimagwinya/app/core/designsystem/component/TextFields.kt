package com.rimagwinya.app.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.designsystem.theme.Stroke

/**
 * A text field with its label above and its error below.
 *
 * The error sits under the field it belongs to rather than being collected
 * into one message at the bottom of the form, so there is never any doubt
 * about which box is wrong.
 */
@Composable
fun RmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    @androidx.annotation.DrawableRes leadingIcon: Int? = null,
) {
    val c = RmTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var revealed by remember { mutableStateOf(false) }

    val borderColor = when {
        error != null -> c.error
        focused -> c.sky
        else -> c.border
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.x8)) {
        Text(label.uppercase(), style = RmTheme.type.label, color = c.text3)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.r12))
                .background(c.surface)
                .border(Stroke.hairline, borderColor, RoundedCornerShape(Radius.r12))
                .defaultMinSize(minHeight = Space.touchTarget)
                .padding(horizontal = Space.x16, vertical = Space.x12),
            horizontalArrangement = Arrangement.spacedBy(Space.x12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    tint = c.text3,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = RmTheme.type.body, color = c.text3)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    interactionSource = interaction,
                    textStyle = RmTheme.type.body.copy(color = c.text),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(c.navy),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (isPassword && !revealed) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (isPassword) {
                Icon(
                    painter = painterResource(R.drawable.ic_eye),
                    contentDescription = null,
                    tint = if (revealed) c.navy else c.text3,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(Radius.r8))
                        .clickable { revealed = !revealed },
                )
            }
        }

        AnimatedVisibility(visible = error != null) {
            Text(error.orEmpty(), style = RmTheme.type.caption, color = c.error)
        }
    }
}

@Preview(name = "Text fields", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun RmTextFieldPreview() = RimagwinyaTheme {
    var email by remember { mutableStateOf("thabo@gmail.com") }
    var number by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("hunter22") }

    Column(
        Modifier.padding(Space.x20),
        verticalArrangement = Arrangement.spacedBy(Space.x20),
    ) {
        RmTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            leadingIcon = R.drawable.ic_mail,
            keyboardType = KeyboardType.Email,
        )
        RmTextField(
            value = number,
            onValueChange = { number = it },
            label = "Student number",
            placeholder = "ST2024001",
            error = "That student number is already registered.",
            leadingIcon = R.drawable.ic_user,
        )
        RmTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            leadingIcon = R.drawable.ic_lock,
            keyboardType = KeyboardType.Password,
        )
    }
}
