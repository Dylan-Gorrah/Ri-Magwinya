package com.rimagwinya.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The menu exactly as PostgREST returns it, options nested inside items.
 *
 * These are wire types and nothing else. They keep the server's snake_case
 * and its numeric prices, and the mapping into the domain model (where money
 * is integer cents) happens in one place, in MenuMapper.
 */
@Serializable
data class MenuItemDto(
    val id: String,
    val slug: String,
    val name: String,
    val description: String? = null,
    val category: String,
    val price: Double,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("icon_key") val iconKey: String,
    @SerialName("stock_quantity") val stockQuantity: Int,
    @SerialName("reorder_level") val reorderLevel: Int,
    @SerialName("is_available") val isAvailable: Boolean,
    @SerialName("temperature_tag") val temperatureTag: String? = null,
    @SerialName("base_step_label") val baseStepLabel: String? = null,
    @SerialName("base_step_singular") val baseStepSingular: String? = null,
    @SerialName("base_step_min") val baseStepMin: Int? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("option_groups") val optionGroups: List<OptionGroupDto> = emptyList(),
)

@Serializable
data class OptionGroupDto(
    val id: String,
    val key: String,
    val label: String,
    val type: String,
    @SerialName("replaces_price") val replacesPrice: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val options: List<OptionDto> = emptyList(),
)

@Serializable
data class OptionDto(
    val id: String,
    val key: String,
    val name: String,
    val price: Double = 0.0,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class CollectionSlotDto(
    val id: String,
    val name: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val capacity: Int,
    @SerialName("orders_taken") val ordersTaken: Int,
    @SerialName("service_date") val serviceDate: String,
)
