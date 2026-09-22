package com.rimagwinya.app.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.FoodIcon
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.StockPill
import com.rimagwinya.app.core.designsystem.component.StockState
import com.rimagwinya.app.core.designsystem.component.Stepper
import com.rimagwinya.app.core.designsystem.theme.RimagwinyaTheme
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.designsystem.theme.Stroke
import com.rimagwinya.app.domain.model.ItemOption
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.OptionGroup
import com.rimagwinya.app.domain.model.OptionGroupType
import com.rimagwinya.app.core.designsystem.component.MenuIcons
import com.rimagwinya.app.domain.pricing.PriceCalculator
import com.rimagwinya.app.domain.pricing.Selection

/**
 * The option engine on screen.
 *
 * One sheet drives every item, because the behaviour is data: a base step, a
 * qty group, a single-select group, or a single-select group that replaces
 * the price. Nothing here is special-cased per item.
 */
@Composable
fun ItemSheetContent(
    item: MenuItem,
    onAdd: (Selection) -> Unit,
    modifier: Modifier = Modifier,
    initial: Selection = Selection.initial(item),
) {
    val c = RmTheme.colors
    var selection by remember(item.id) { mutableStateOf(initial) }

    val canAdd = PriceCalculator.canAddToCart(item, selection) && !item.isSoldOut

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp),
    ) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            // ---- Header ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.x16),
                verticalAlignment = Alignment.Top,
            ) {
                FoodIcon(MenuIcons.drawableFor(item.iconKey), size = 64.dp, iconSize = 36.dp)
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Space.x8),
                ) {
                    Text(item.name, style = RmTheme.type.screenTitle, color = c.text)
                    Text(item.category.serverName, style = RmTheme.type.caption, color = c.text3)
                    StockPill(
                        StockState.of(item.stockQuantity, item.reorderLevel, item.isAvailable)
                    )
                }
            }

            if (!item.description.isNullOrBlank()) {
                Text(
                    item.description,
                    style = RmTheme.type.secondary,
                    color = c.text2,
                    modifier = Modifier.padding(top = Space.x16),
                )
            }

            // ---- Base step ----
            item.baseStep?.let { step ->
                GroupHeading(step.label)
                GroupedList {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.x16, vertical = Space.x12),
                        horizontalArrangement = Arrangement.spacedBy(Space.x12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FoodIcon(MenuIcons.drawableFor(item.iconKey), size = 36.dp, iconSize = 20.dp)
                        Column(Modifier.weight(1f)) {
                            Text(step.label, style = RmTheme.type.bodyStrong, color = c.text)
                            Text(
                                stringResource(R.string.sheet_each, item.price.format()),
                                style = RmTheme.type.caption,
                                color = c.text3,
                            )
                        }
                        Stepper(
                            value = selection.baseQty,
                            onChange = { selection = selection.copy(baseQty = it) },
                            min = step.min,
                        )
                    }
                }
                if (selection.baseQty == 0) {
                    Text(
                        stringResource(R.string.sheet_fillings_only),
                        style = RmTheme.type.caption,
                        color = c.text3,
                        modifier = Modifier.padding(top = Space.x8),
                    )
                }
            }

            // ---- Option groups ----
            item.optionGroups.forEach { group ->
                GroupHeading(group.label)
                GroupedList {
                    group.options.forEachIndexed { index, option ->
                        when (group.type) {
                            OptionGroupType.Qty -> QtyOptionRow(
                                option = option,
                                count = selection.countOf(option.id),
                                onChange = { selection = selection.withCount(option.id, it) },
                            )

                            OptionGroupType.Single -> SingleOptionRow(
                                option = option,
                                group = group,
                                selected = selection.singles[group.id] == option.id,
                                onSelect = { selection = selection.withSingle(group.id, option.id) },
                            )
                        }
                        if (index < group.options.lastIndex) ListDivider()
                    }
                }
            }

            // ---- Outer quantity, plain items only ----
            if (!item.isBuildItem) {
                GroupHeading(stringResource(R.string.sheet_quantity))
                GroupedList {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.x16, vertical = Space.x12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.sheet_quantity),
                            style = RmTheme.type.bodyStrong,
                            color = c.text,
                            modifier = Modifier.weight(1f),
                        )
                        Stepper(
                            value = selection.quantity,
                            onChange = { selection = selection.copy(quantity = it) },
                            min = 1,
                            max = maxOf(item.stockQuantity, 1),
                        )
                    }
                }
            }
        }

        // ---- The button, which always shows the live total ----
        Box(Modifier.padding(top = Space.x24)) {
            RmButton(
                text = if (canAdd) {
                    stringResource(
                        R.string.sheet_add_to_cart,
                        PriceCalculator.lineTotal(item, selection).format(),
                    )
                } else if (item.isSoldOut) {
                    stringResource(R.string.sheet_sold_out)
                } else {
                    stringResource(R.string.sheet_choose_something)
                },
                onClick = { onAdd(selection) },
                enabled = canAdd,
            )
        }
    }
}

@Composable
private fun GroupHeading(text: String) {
    Text(
        text = text.uppercase(),
        style = RmTheme.type.label,
        color = RmTheme.colors.text3,
        modifier = Modifier.padding(top = Space.x24, bottom = Space.x8),
    )
}

@Composable
private fun QtyOptionRow(
    option: ItemOption,
    count: Int,
    onChange: (Int) -> Unit,
) {
    val c = RmTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x16, vertical = Space.x12),
        horizontalArrangement = Arrangement.spacedBy(Space.x12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(option.name, style = RmTheme.type.bodyStrong, color = c.text)
            Text(
                stringResource(R.string.sheet_each, option.price.format()),
                style = RmTheme.type.caption,
                color = c.text3,
            )
        }
        Stepper(value = count, onChange = onChange)
    }
}

@Composable
private fun SingleOptionRow(
    option: ItemOption,
    group: OptionGroup,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val c = RmTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = Space.x16, vertical = Space.x12)
            .heightIn(min = Space.touchTarget - Space.x8),
        horizontalArrangement = Arrangement.spacedBy(Space.x12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) c.navy else c.surface)
                .border(
                    if (selected) 0.dp else Stroke.hairline,
                    if (selected) c.navy else c.borderStrong,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(c.surface))
            }
        }

        Text(
            option.name,
            style = RmTheme.type.bodyStrong,
            color = c.text,
            modifier = Modifier.weight(1f),
        )

        // A free option would only add noise; a size that sets the price
        // needs to show what it costs.
        if (group.replacesPrice || option.price.isPositive) {
            Text(option.price.format(), style = RmTheme.type.price, color = c.text2)
        }
    }
}

@Preview(name = "Vetkoek sheet", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun VetkoekSheetPreview() = RimagwinyaTheme {
    Box(Modifier.padding(Space.screen)) {
        ItemSheetContent(item = PreviewMenu.vetkoek, onAdd = {})
    }
}

@Preview(name = "Chips sheet", showBackground = true, backgroundColor = 0xFFF2F5F9)
@Composable
private fun ChipsSheetPreview() = RimagwinyaTheme {
    Box(Modifier.padding(Space.screen)) {
        ItemSheetContent(item = PreviewMenu.chips, onAdd = {})
    }
}
