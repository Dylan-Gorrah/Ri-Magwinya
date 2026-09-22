package com.rimagwinya.app.data.remote

import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.core.network.ErrorMappingInterceptor
import com.rimagwinya.app.core.network.NetworkModule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * The app's side of the Edge Function contract: what goes on the wire, what
 * comes back, and which typed error each refusal becomes. The response
 * bodies are the real ones the deployed functions returned.
 */
class FunctionsApiTest {

    private val json = NetworkModule.json()
    private lateinit var server: MockWebServer
    private lateinit var api: FunctionsApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(ErrorMappingInterceptor(json)).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FunctionsApi::class.java)
    }

    @After
    fun tearDown() = server.close()

    private fun respond(code: Int, body: String) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    private suspend fun refusal(call: suspend () -> Unit): ApiError =
        try {
            call()
            fail("expected a failure")
            throw AssertionError()
        } catch (e: ApiError) {
            e
        }

    private val order = PlaceOrderBody(
        slotId = "96e2fddc-39dc-4ee2-8db7-e9ec69671711",
        paymentMethod = "wallet",
        clientRef = "0b8e8d5e-4c55-4e0e-9a7a-111111111111",
        lines = listOf(
            OrderLineBody(
                itemId = "vetkoek-id",
                baseQty = 0,
                options = listOf(OrderOptionBody(optionId = "snoek-id", count = 1)),
            ),
            OrderLineBody(
                itemId = "chips-id",
                quantity = 1,
                options = listOf(OrderOptionBody(optionId = "large-id")),
            ),
        ),
    )

    @Test
    fun `place order sends choices in the server's field names`() = runTest {
        respond(200, PLACED)
        api.placeOrder(order)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/functions/v1/place-order", request.url.encodedPath)

        val body = json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("wallet", body["payment_method"]!!.jsonPrimitive.content)
        val lines = body["lines"]!!.jsonArray
        val vetkoek = lines[0].jsonObject
        // Zero is a real answer (fillings only) and must be sent, not dropped.
        assertEquals(0, vetkoek["base_qty"]!!.jsonPrimitive.content.toInt())
        assertFalse("build items send no quantity", "quantity" in vetkoek)
        val chips = lines[1].jsonObject
        assertFalse("chips send no base_qty", "base_qty" in chips)
        assertFalse(
            "a single-select choice sends no count",
            "count" in chips["options"]!!.jsonArray[0].jsonObject,
        )
    }

    @Test
    fun `a placed order parses with its lines`() = runTest {
        respond(200, PLACED)
        val placed = api.placeOrder(order)

        assertEquals(1L, placed.orderNumber)
        assertEquals("7306", placed.collectionCode)
        assertEquals(67.0, placed.totalAmount, 0.0)
        assertEquals("placed", placed.status)
        assertEquals(3, placed.orderItems.size)
        assertEquals("No vetkoek, Snoek", placed.orderItems[2].optionsLabel)
        assertEquals(0, placed.orderItems[2].unitsConsumed)
    }

    @Test
    fun `each 409 reason arrives as its code`() = runTest {
        mapOf(
            """{"code":"OUT_OF_STOCK","detail":"Score Energy 500ml"}""" to ConflictCode.OUT_OF_STOCK,
            """{"code":"SLOT_FULL","detail":"Second break"}""" to ConflictCode.SLOT_FULL,
            """{"code":"INSUFFICIENT_FUNDS","detail":"15.00"}""" to ConflictCode.INSUFFICIENT_FUNDS,
            """{"code":"COUNTER_BLOCKED","detail":"NO_TOPUP_YET"}""" to ConflictCode.COUNTER_BLOCKED,
            """{"code":"EMPTY_SELECTION","detail":"Vetkoek"}""" to ConflictCode.EMPTY_SELECTION,
        ).forEach { (body, expected) ->
            respond(409, body)
            val error = refusal { api.placeOrder(order) }
            assertTrue("$expected: got $error", error is ApiError.Conflict)
            assertEquals(expected, (error as ApiError.Conflict).code)
        }
    }

    @Test
    fun `the detail comes through for the screen to use`() = runTest {
        respond(409, """{"code":"COUNTER_BLOCKED","detail":"TOO_MANY_NO_SHOWS"}""")
        val error = refusal { api.placeOrder(order) } as ApiError.Conflict
        assertEquals("TOO_MANY_NO_SHOWS", error.detail)
    }

    @Test
    fun `an illegal status change is INVALID_TRANSITION`() = runTest {
        respond(409, """{"code":"INVALID_TRANSITION","detail":"preparing -> cancelled"}""")
        val error = refusal { api.setOrderStatus(OrderStatusBody("o", "cancelled")) }
        assertEquals(ConflictCode.INVALID_TRANSITION, (error as ApiError.Conflict).code)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/functions/v1/order-status", request.url.encodedPath)
    }

    @Test
    fun `a student moving an order is Forbidden`() = runTest {
        respond(403, """{"code":"FORBIDDEN","detail":null}""")
        assertEquals(ApiError.Forbidden, refusal { api.setOrderStatus(OrderStatusBody("o", "ready")) })
    }

    @Test
    fun `an unknown student number keeps its reason despite the 404`() = runTest {
        respond(404, """{"code":"STUDENT_NOT_FOUND","detail":null}""")
        val error = refusal { api.loadWallet(LoadWalletBody("NOPE999", 50.0)) }
        assertEquals(ConflictCode.STUDENT_NOT_FOUND, (error as ApiError.Conflict).code)
    }

    @Test
    fun `an amount out of range keeps its reason despite the 400`() = runTest {
        respond(400, """{"code":"AMOUNT_OUT_OF_RANGE","detail":"R10 to R1000"}""")
        val error = refusal { api.loadWallet(LoadWalletBody("TEST001", 5.0)) }
        assertEquals(ConflictCode.AMOUNT_OUT_OF_RANGE, (error as ApiError.Conflict).code)
    }

    @Test
    fun `a malformed request is a server-side bug, not a user message`() = runTest {
        respond(400, """{"code":"BAD_REQUEST","detail":"slot_id must be a uuid"}""")
        assertTrue(refusal { api.placeOrder(order) } is ApiError.Server)
    }

    @Test
    fun `signed out is Unauthorised`() = runTest {
        respond(401, """{"code":"NOT_AUTHENTICATED","detail":null}""")
        assertEquals(ApiError.Unauthorised, refusal { api.placeOrder(order) })
    }

    @Test
    fun `a top-up returns the new balance and nothing private`() = runTest {
        respond(
            200,
            """{"id":"cb5c","full_name":"Test Student","student_number":"TEST001","wallet_balance":100}""",
        )
        val result = api.loadWallet(LoadWalletBody("test001", 100.0))
        assertEquals("TEST001", result.studentNumber)
        assertEquals(100.0, result.walletBalance, 0.0)
    }

    private companion object {
        // Trimmed from a real place-order response.
        const val PLACED = """{"id":"f1","order_number":1,"student_id":"cb5c",
            "collection_code":"7306","slot_id":"96e2fddc-39dc-4ee2-8db7-e9ec69671711",
            "total_amount":67,"status":"placed","payment_method":"wallet",
            "client_ref":"0b8e8d5e-4c55-4e0e-9a7a-111111111111",
            "placed_at":"2026-09-22T18:56:17.12+00:00","prepared_at":null,"ready_at":null,
            "completed_at":null,"cancelled_at":null,
            "order_items":[
              {"id":"a","item_id":"v","item_name":"Vetkoek","options_label":"2 vetkoeks, 2 Polony, Cheese slice","quantity":1,"unit_price":17,"units_consumed":2,"subtotal":17},
              {"id":"b","item_id":"c","item_name":"Fried chips","options_label":"Large","quantity":1,"unit_price":40,"units_consumed":1,"subtotal":40},
              {"id":"c","item_id":"v","item_name":"Vetkoek","options_label":"No vetkoek, Snoek","quantity":1,"unit_price":10,"units_consumed":0,"subtotal":10}
            ]}"""
    }
}
