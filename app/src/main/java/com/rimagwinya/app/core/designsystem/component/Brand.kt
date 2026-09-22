package com.rimagwinya.app.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.theme.Radius
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space

/**
 * The vetkoek face inside its navy ring, with the split sky and gold arc.
 * Converted from the prototype's <symbol id="brand">, so it keeps its own
 * colours rather than taking a tint.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Image(
        painter = painterResource(R.drawable.ic_brand_mark),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

/** One of the ten food icons, tinted and sat on a wash background. */
@Composable
fun FoodIcon(
    @androidx.annotation.DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 26.dp,
    tint: Color = RmTheme.colors.sky,
    background: Color = RmTheme.colors.skyWash,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(Radius.r12))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview(name = "Brand", showBackground = true)
@Composable
private fun BrandMarkPreview() = RimagwinyaTheme {
    Box(Modifier.padding(Space.x16)) { BrandMark(size = 64.dp) }
}

@Preview(name = "Food icons", showBackground = true)
@Composable
private fun FoodIconPreview() = RimagwinyaTheme {
    Row(
        Modifier.padding(Space.x16),
        horizontalArrangement = Arrangement.spacedBy(Space.x8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodIcon(R.drawable.ic_food_vetkoek)
        FoodIcon(R.drawable.ic_food_wors)
        FoodIcon(R.drawable.ic_food_cup)
        FoodIcon(R.drawable.ic_food_can)
    }
}
