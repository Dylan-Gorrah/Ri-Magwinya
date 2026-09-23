package com.rimagwinya.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST

/**
 * The three Edge Functions, in `supabase/functions/`.
 *
 * Each is a thin wrapper round a SQL function that does the real work in one
 * transaction. Failures arrive as `{ code, detail }` and
 * [com.rimagwinya.app.core.network.ErrorMappingInterceptor] turns them into
 * [com.rimagwinya.app.core.network.ApiError.Conflict] with a
 * [com.rimagwinya.app.core.network.ConflictCode] before they get here.
 */
interface FunctionsApi {

    /**
     * Prices, checks and places an order. Safe to retry: the same
     * [PlaceOrderBody.clientRef] returns the order it already made.
     *
     * 409: OUT_OF_STOCK, SLOT_FULL, INSUFFICIENT_FUNDS, COUNTER_BLOCKED,
     * EMPTY_SELECTION, SLOT_NOT_TODAY, STUDENT_NUMBER_REQUIRED
     */
    @POST("functions/v1/place-order")
    suspend fun placeOrder(@Body body: PlaceOrderBody): OrderDto

    /**
     * Moves an order along. Staff for everything except a student cancelling
     * their own order while it is still placed.
     *
     * 403 wrong role · 409 INVALID_TRANSITION
     */
    @PATCH("functions/v1/order-status")
    suspend fun setOrderStatus(@Body body: OrderStatusBody): OrderDto

    /**
     * Staff record a top-up paid on the speed point.
     *
     * 403 not staff · 404 STUDENT_NOT_FOUND · 400 AMOUNT_OUT_OF_RANGE
     */
    @POST("functions/v1/load-wallet")
    suspend fun loadWallet(@Body body: LoadWalletBody): TopUpResultDto
}

// --- Requests ----------------------------------------------------------------

@Serializable
data class PlaceOrderBody(
    @SerialName("slot_id") val slotId: String,
    /** "wallet" or "counter". */
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("client_ref") val clientRef: String,
    val lines: List<OrderLineBody>,
)

/**
 * Choices only, never prices. The server prices the line from its own rows,
 * so there is nothing here worth tampering with.
 */
@Serializable
data class OrderLineBody(
    @SerialName("item_id") val itemId: String,
    /** Base-step items only (vetkoek). May be 0. */
    @SerialName("base_qty") val baseQty: Int? = null,
    /** Non-build items only. Build items are always 1. */
    val quantity: Int? = null,
    val options: List<OrderOptionBody> = emptyList(),
)

@Serializable
data class OrderOptionBody(
    @SerialName("option_id") val optionId: String,
    /** How many, for qty groups. Omitted for single-select choices. */
    val count: Int? = null,
)

@Serializable
data class OrderStatusBody(
    @SerialName("order_id") val orderId: String,
    val status: String,
)

@Serializable
data class LoadWalletBody(
    @SerialName("student_number") val studentNumber: String,
    /** Rands, at most two decimals. The wire is decimal; the app is cents. */
    val amount: Double,
)

// --- Responses ---------------------------------------------------------------

@Serializable
data class OrderDto(
    val id: String,
    @SerialName("order_number") val orderNumber: Long,
    @SerialName("student_id") val studentId: String,
    @SerialName("collection_code") val collectionCode: String,
    @SerialName("slot_id") val slotId: String,
    @SerialName("total_amount") val totalAmount: Double,
    val status: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("client_ref") val clientRef: String,
    @SerialName("placed_at") val placedAt: String,
    @SerialName("prepared_at") val preparedAt: String? = null,
    @SerialName("ready_at") val readyAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    /** Present on place-order and on PostgREST reads with the embed. */
    @SerialName("order_items") val orderItems: List<OrderItemDto> = emptyList(),
    /** PostgREST embeds, when the select asks for them. */
    @SerialName("collection_slots") val slot: SlotEmbedDto? = null,
    @SerialName("profiles") val student: StudentEmbedDto? = null,
)

@Serializable
data class SlotEmbedDto(
    val name: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
)

@Serializable
data class StudentEmbedDto(
    @SerialName("full_name") val fullName: String,
    @SerialName("student_number") val studentNumber: String? = null,
)

@Serializable
data class OrderItemDto(
    val id: String,
    @SerialName("item_id") val itemId: String? = null,
    @SerialName("item_name") val itemName: String,
    @SerialName("options_label") val optionsLabel: String? = null,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("units_consumed") val unitsConsumed: Int,
    val subtotal: Double,
)

/** A row of `wallet_transactions`. Staff can read anyone's; RLS says so. */
@Serializable
data class WalletTransactionDto(
    val amount: Double,
    val type: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class TopUpResultDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_number") val studentNumber: String,
    @SerialName("wallet_balance") val walletBalance: Double,
)
