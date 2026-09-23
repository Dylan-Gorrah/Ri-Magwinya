package com.rimagwinya.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Every PostgREST endpoint the app uses, as an explicit interface.
 *
 * The brief puts all REST traffic through Retrofit rather than supabase-kt's
 * own query builder for exactly this reason: each endpoint is a named,
 * typed, testable method instead of a chain built at the call site.
 */
interface SupabaseApi {

    /**
     * The whole menu in one round trip. PostgREST builds the nesting from the
     * foreign keys, so items, their option groups and every option arrive
     * together rather than as N+1 requests.
     */
    @GET("rest/v1/menu_items")
    suspend fun menu(
        @Query("select") select: String = MENU_SELECT,
        @Query("order") order: String = "sort_order.asc",
    ): List<MenuItemDto>

    @GET("rest/v1/collection_slots")
    suspend fun slots(
        @Query("service_date") serviceDate: String,
        @Query("order") order: String = "starts_at.asc",
    ): List<CollectionSlotDto>

    /** Creates the day's three breaks if they are not there yet. */
    @POST("rest/v1/rpc/ensure_slots")
    suspend fun ensureSlots(@Body body: EnsureSlotsBody): List<CollectionSlotDto>

    /**
     * The signed-in person's profile. Row Level Security scopes it to the
     * caller, so this can only ever return your own row even though the id
     * is in the query.
     */
    @GET("rest/v1/profiles")
    suspend fun profile(
        @Query("id") id: String,
        @Query("select") select: String = "*",
    ): List<ProfileDto>

    /** Name, language and FCM token. The only columns a student may change. */
    @PATCH("rest/v1/profiles")
    @Headers("Prefer: return=representation")
    suspend fun patchProfile(
        @Query("id") id: String,
        @Body patch: ProfilePatch,
    ): List<ProfileDto>

    /**
     * Sets the student number once, for accounts made by Google sign-in.
     * A function rather than a column grant, so nobody can rewrite theirs to
     * take a number that belongs to someone else.
     */
    @POST("rest/v1/rpc/claim_student_number")
    suspend fun claimStudentNumber(@Body body: ClaimStudentNumberBody): ProfileDto

    /**
     * Orders with their lines, slot and (for staff) who they are for. RLS
     * decides whose: a student gets their own, staff get everyone's.
     */
    @GET("rest/v1/orders")
    suspend fun orders(
        @Query("select") select: String = ORDER_SELECT,
        @Query("order") order: String = "placed_at.desc",
        @Query("status") status: String? = null,
        @Query("placed_at") placedSince: String? = null,
        @Query("limit") limit: Int? = null,
    ): List<OrderDto>

    @GET("rest/v1/orders")
    suspend fun order(
        @Query("id") id: String,
        @Query("select") select: String = ORDER_SELECT,
    ): List<OrderDto>

    /**
     * Whether the caller has ever been topped up. Pay-at-counter needs one:
     * it means staff have seen this person at the speed point.
     */
    @GET("rest/v1/wallet_transactions")
    suspend fun topUps(
        @Query("type") type: String = "eq.topup",
        @Query("select") select: String = "id",
        @Query("limit") limit: Int = 1,
    ): List<IdOnly>

    /** The bell: the caller's notifications, newest first. */
    @GET("rest/v1/notifications")
    suspend fun notifications(
        @Query("select") select: String = "*",
        @Query("order") order: String = "sent_at.desc",
        @Query("limit") limit: Int = 50,
    ): List<NotificationDto>

    /** Marking as read is the only change a user may make to one. */
    @PATCH("rest/v1/notifications")
    suspend fun markNotificationsRead(
        @Query("is_read") isRead: String = "eq.false",
        @Body body: MarkReadBody = MarkReadBody(),
    )

    /** The caller's loyalty stamps. RLS scopes it to them. */
    @GET("rest/v1/loyalty_stamps")
    suspend fun loyaltyStamps(
        @Query("select") select: String = "id",
        @Query("limit") limit: Int = 200,
    ): List<IdOnly>

    // --- Staff -----------------------------------------------------------------

    /** Adds or removes stock as a change, never a total. Staff only. */
    @POST("rest/v1/rpc/adjust_stock")
    suspend fun adjustStock(@Body body: AdjustStockBody): MenuItemDto

    /** Revenue, counts, orders by hour and top items, aggregated in SQL. */
    @POST("rest/v1/rpc/sales_summary")
    suspend fun salesSummary(@Body body: SalesRangeBody): SalesSummaryDto

    @POST("rest/v1/menu_items")
    @Headers("Prefer: return=representation")
    suspend fun createItem(@Body item: kotlinx.serialization.json.JsonObject): List<MenuItemDto>

    @PATCH("rest/v1/menu_items")
    @Headers("Prefer: return=representation")
    suspend fun updateItem(@Query("id") id: String, @Body item: kotlinx.serialization.json.JsonObject): List<MenuItemDto>

    @retrofit2.http.DELETE("rest/v1/menu_items")
    suspend fun deleteItem(@Query("id") id: String)

    /**
     * Staff searching students by part of a number or a name, e.g.
     * `or=(student_number.ilike.*ST10*,full_name.ilike.*ST10*)`.
     */
    @GET("rest/v1/profiles")
    suspend fun searchStudents(
        @Query("or") or: String,
        @Query("role") role: String = "eq.student",
        @Query("order") order: String = "full_name",
        @Query("limit") limit: Int = 20,
        @Query("select") select: String = "*",
    ): List<ProfileDto>

    /** A student's latest wallet movements, for the staff top-up screen. */
    @GET("rest/v1/wallet_transactions")
    suspend fun walletHistory(
        @Query("user_id") userId: String,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 10,
        @Query("select") select: String = "amount,type,created_at",
    ): List<WalletTransactionDto>

    companion object {
        const val MENU_SELECT = "*,option_groups(*,options(*))"
        const val ORDER_SELECT =
            "*,order_items(*),collection_slots(name,starts_at,ends_at),profiles(full_name,student_number)"
    }
}

@Serializable
data class IdOnly(val id: String)

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    @kotlinx.serialization.SerialName("is_read") val isRead: Boolean = false,
    @kotlinx.serialization.SerialName("order_id") val orderId: String? = null,
    @kotlinx.serialization.SerialName("sent_at") val sentAt: String,
)

@Serializable
data class MarkReadBody(@kotlinx.serialization.SerialName("is_read") val isRead: Boolean = true)

@Serializable
data class AdjustStockBody(val p_item_id: String, val p_delta: Int)

@Serializable
data class SalesRangeBody(val p_from: String, val p_to: String)

@Serializable
data class SalesSummaryDto(
    val revenue: Double = 0.0,
    @kotlinx.serialization.SerialName("order_count") val orderCount: Int = 0,
    @kotlinx.serialization.SerialName("average_order") val averageOrder: Double = 0.0,
    @kotlinx.serialization.SerialName("orders_by_hour") val ordersByHour: List<HourCountDto> = emptyList(),
    @kotlinx.serialization.SerialName("top_items") val topItems: List<TopItemDto> = emptyList(),
)

@Serializable
data class HourCountDto(val hour: Int, val orders: Int)

@Serializable
data class TopItemDto(
    @kotlinx.serialization.SerialName("item_name") val itemName: String,
    val quantity: Int,
    val revenue: Double,
)

@Serializable
data class EnsureSlotsBody(
    val p_date: String,
)
