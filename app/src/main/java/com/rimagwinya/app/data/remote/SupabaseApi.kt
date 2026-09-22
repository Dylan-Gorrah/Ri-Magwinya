package com.rimagwinya.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
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

    companion object {
        const val MENU_SELECT = "*,option_groups(*,options(*))"
    }
}

@Serializable
data class EnsureSlotsBody(
    val p_date: String,
)
