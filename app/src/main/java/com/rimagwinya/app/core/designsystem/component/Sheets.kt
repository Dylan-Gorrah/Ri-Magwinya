package com.rimagwinya.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.SheetShape
import com.rimagwinya.app.core.designsystem.theme.Space

/**
 * The app's bottom sheet: grab handle, scrim, rounded on the top corners
 * only. The item builder, the notifications list and the confirm dialogs all
 * sit in one of these rather than in a centred dialog, because a sheet keeps
 * the thumb near the controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RmBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = RmTheme.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = SheetShape,
        containerColor = c.background,
        contentColor = c.text,
        dragHandle = { GrabHandle() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            content = content,
        )
    }
}

@Composable
private fun GrabHandle() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Space.x12),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(RmTheme.colors.borderStrong)
        )
    }
}

@Preview(name = "Sheet handle", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun GrabHandlePreview() = RimagwinyaTheme {
    GrabHandle()
}
