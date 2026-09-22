package com.rimagwinya.app.feature.staff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.Banner
import com.rimagwinya.app.core.designsystem.component.BannerTone
import com.rimagwinya.app.core.designsystem.component.Chip
import com.rimagwinya.app.core.designsystem.component.FoodIcon
import com.rimagwinya.app.core.designsystem.component.GroupedList
import com.rimagwinya.app.core.designsystem.component.IconAction
import com.rimagwinya.app.core.designsystem.component.ListDivider
import com.rimagwinya.app.core.designsystem.component.ListRow
import com.rimagwinya.app.core.designsystem.component.MenuIcons
import com.rimagwinya.app.core.designsystem.component.RmAppBar
import com.rimagwinya.app.core.designsystem.component.RmBottomSheet
import com.rimagwinya.app.core.designsystem.component.RmButton
import com.rimagwinya.app.core.designsystem.component.RmButtonStyle
import com.rimagwinya.app.core.designsystem.component.RmTextField
import com.rimagwinya.app.core.designsystem.component.SectionLabel
import com.rimagwinya.app.core.designsystem.component.SkeletonRow
import com.rimagwinya.app.core.designsystem.component.Stepper
import com.rimagwinya.app.core.designsystem.component.StockPill
import com.rimagwinya.app.core.designsystem.component.StockState
import com.rimagwinya.app.core.designsystem.component.ToggleRow
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.util.resolve
import com.rimagwinya.app.domain.model.MenuCategory
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.Temperature

@Composable
fun StockScreen(
    modifier: Modifier = Modifier,
    viewModel: StockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        RmAppBar(title = stringResource(R.string.title_stock)) {
            IconAction(icon = R.drawable.ic_plus, label = stringResource(R.string.stock_add), onClick = viewModel::openNew)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(bottom = Space.x32),
            verticalArrangement = Arrangement.spacedBy(Space.x16),
        ) {
            state.error?.let { Banner(text = it.resolve(), tone = BannerTone.Error) }

            if (state.loading && state.items.isEmpty()) {
                GroupedList { repeat(6) { i -> SkeletonRow(); if (i < 5) ListDivider() } }
                return@Column
            }

            val flagged = state.needsAttention
            if (flagged.isNotEmpty()) {
                Banner(
                    text = stringResource(
                        R.string.stock_attention,
                        pluralStringResource(R.plurals.stock_attention_count, flagged.size, flagged.size),
                        flagged.joinToString { it.name },
                    ),
                    tone = BannerTone.Warning,
                )
            } else {
                Banner(text = stringResource(R.string.stock_all_good), tone = BannerTone.Success)
            }

            GroupedList {
                state.items.forEachIndexed { index, item ->
                    StockRow(
                        item = item,
                        onAdjust = { delta -> viewModel.adjust(item, delta) },
                        onEdit = { viewModel.openEdit(item) },
                    )
                    if (index < state.items.lastIndex) ListDivider()
                }
            }

            Text(
                stringResource(R.string.stock_footer),
                style = RmTheme.type.caption,
                color = RmTheme.colors.text3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    state.editor?.let { form ->
        RmBottomSheet(onDismiss = viewModel::closeEditor) {
            ItemEditor(
                form = form,
                onChange = viewModel::editForm,
                onSave = viewModel::save,
                onAskDelete = viewModel::askDelete,
                onDelete = viewModel::delete,
            )
        }
    }
}

@Composable
private fun StockRow(item: MenuItem, onAdjust: (Int) -> Unit, onEdit: () -> Unit) {
    val stock = StockState.of(item.stockQuantity, item.reorderLevel, item.isAvailable)
    ListRow(
        title = item.name,
        // Status in words as well as colour.
        subtitle = when {
            !item.isAvailable -> stringResource(R.string.stock_switched_off)
            stock is StockState.SoldOut -> stringResource(R.string.stock_hidden)
            stock is StockState.Low -> stringResource(R.string.stock_below_reorder)
            else -> stringResource(R.string.stock_price_category, item.price.format(), item.category.serverName)
        },
        onClick = onEdit,
        showChevron = false,
        leading = { FoodIcon(MenuIcons.drawableFor(item.iconKey)) },
        trailing = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Stepper(
                    value = item.stockQuantity,
                    onChange = { new -> onAdjust(new - item.stockQuantity) },
                    min = 0,
                    max = 9999,
                )
                if (stock !is StockState.InStock) StockPill(stock)
            }
        },
    )
}

@Composable
private fun ItemEditor(
    form: EditorForm,
    onChange: ((EditorForm) -> EditorForm) -> Unit,
    onSave: () -> Unit,
    onAskDelete: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen)
            .padding(bottom = Space.x24),
        verticalArrangement = Arrangement.spacedBy(Space.x16),
    ) {
        Text(
            stringResource(if (form.isNew) R.string.editor_new_title else R.string.editor_edit_title),
            style = RmTheme.type.screenTitle,
            color = RmTheme.colors.text,
        )
        RmTextField(form.name, { v -> onChange { it.copy(name = v) } }, label = stringResource(R.string.editor_name))
        RmTextField(
            form.description,
            { v -> onChange { it.copy(description = v) } },
            label = stringResource(R.string.editor_description),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.x12)) {
            RmTextField(
                form.price,
                { v -> onChange { it.copy(price = v) } },
                label = stringResource(R.string.editor_price),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            RmTextField(
                form.reorderLevel,
                { v -> onChange { it.copy(reorderLevel = v) } },
                label = stringResource(R.string.editor_reorder),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        if (form.isNew) {
            RmTextField(
                form.openingStock,
                { v -> onChange { it.copy(openingStock = v) } },
                label = stringResource(R.string.editor_stock),
                keyboardType = KeyboardType.Number,
            )
        }

        Column {
            SectionLabel(stringResource(R.string.editor_category))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x8)) {
                MenuCategory.entries.forEach { category ->
                    Chip(
                        text = stringResource(category.labelRes()),
                        selected = form.category == category,
                        onClick = { onChange { it.copy(category = category) } },
                    )
                }
            }
        }

        Column {
            SectionLabel(stringResource(R.string.editor_temperature))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x8)) {
                listOf(null, Temperature.Hot, Temperature.Cold).forEach { temp ->
                    Chip(
                        text = stringResource(
                            when (temp) {
                                null -> R.string.editor_temp_none
                                Temperature.Hot -> R.string.editor_temp_hot
                                Temperature.Cold -> R.string.editor_temp_cold
                            }
                        ),
                        selected = form.temperature == temp,
                        onClick = { onChange { it.copy(temperature = temp) } },
                    )
                }
            }
        }

        Column {
            SectionLabel(stringResource(R.string.editor_icon))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.x8),
            ) {
                MenuIcons.keys.forEach { key ->
                    val selected = form.iconKey == key
                    FoodIcon(
                        MenuIcons.drawableFor(key),
                        tint = if (selected) androidx.compose.ui.graphics.Color.White else RmTheme.colors.sky,
                        background = if (selected) RmTheme.colors.navy else RmTheme.colors.skyWash,
                        modifier = Modifier
                            .clickable { onChange { it.copy(iconKey = key) } }
                            .semantics { contentDescription = key },
                    )
                }
            }
        }

        ToggleRow(
            title = stringResource(R.string.editor_available),
            checked = form.isAvailable,
            onCheckedChange = { v -> onChange { it.copy(isAvailable = v) } },
        )

        Text(stringResource(R.string.editor_options_note), style = RmTheme.type.caption, color = RmTheme.colors.text3)

        form.error?.let { Banner(text = it.resolve(), tone = BannerTone.Error) }

        RmButton(
            text = stringResource(if (form.saving) R.string.editor_saving else R.string.editor_save),
            onClick = onSave,
            enabled = !form.saving,
        )
        if (!form.isNew) {
            if (form.confirmingDelete) {
                Banner(text = stringResource(R.string.editor_delete_confirm, form.name), tone = BannerTone.Warning)
                RmButton(stringResource(R.string.editor_delete), onDelete, style = RmButtonStyle.Danger, enabled = !form.saving)
            } else {
                RmButton(stringResource(R.string.editor_delete), onAskDelete, style = RmButtonStyle.Quiet, enabled = !form.saving)
            }
        }
    }
}

private fun MenuCategory.labelRes(): Int = when (this) {
    MenuCategory.Meals -> R.string.menu_category_meals
    MenuCategory.Snacks -> R.string.menu_category_snacks
    MenuCategory.Drinks -> R.string.menu_category_drinks
}
