package com.rimagwinya.app.data.repository

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.IoDispatcher
import com.rimagwinya.app.core.network.asApiError
import com.rimagwinya.app.core.util.Validation
import com.rimagwinya.app.data.remote.AdjustStockBody
import com.rimagwinya.app.data.remote.FunctionsApi
import com.rimagwinya.app.data.remote.LoadWalletBody
import com.rimagwinya.app.data.remote.SalesRangeBody
import com.rimagwinya.app.data.remote.SupabaseApi
import com.rimagwinya.app.data.remote.toDomain
import com.rimagwinya.app.data.remote.toJson
import com.rimagwinya.app.data.remote.toStudentAccount
import com.rimagwinya.app.domain.model.ItemDraft
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.SalesSummary
import com.rimagwinya.app.domain.model.StudentAccount
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything only staff can do. Each call is also refused by the database
 * for anyone else — the app hiding the staff tabs is a convenience, not the
 * security.
 */
@Singleton
open class StaffRepository @Inject constructor(
    private val api: SupabaseApi,
    private val functions: FunctionsApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /** +1 or -1 from a stepper, or a batch replayed from the offline queue. */
    open suspend fun adjustStock(itemId: String, delta: Int): Result<MenuItem> = call {
        api.adjustStock(AdjustStockBody(itemId, delta)).toDomain()
    }

    open suspend fun createItem(draft: ItemDraft): Result<MenuItem> = call {
        try {
            api.createItem(draft.toJson(isNew = true)).first().toDomain()
        } catch (e: ApiError.Conflict) {
            // PostgREST answers a taken slug (a second "Coke") with 409.
            // Add a short suffix and try once more.
            val suffix = "-" + (System.currentTimeMillis() % 10_000)
            api.createItem(draft.toJson(isNew = true, slugSuffix = suffix)).first().toDomain()
        }
    }

    open suspend fun updateItem(id: String, draft: ItemDraft): Result<MenuItem> = call {
        api.updateItem("eq.$id", draft.toJson(isNew = false)).first().toDomain()
    }

    open suspend fun deleteItem(id: String): Result<Unit> = call { api.deleteItem("eq.$id") }

    open suspend fun sales(day: LocalDate): Result<SalesSummary> = call {
        api.salesSummary(SalesRangeBody(day.toString(), day.toString())).toDomain()
    }

    open suspend fun findStudent(studentNumber: String): Result<StudentAccount> = call {
        val number = Validation.normaliseStudentNumber(studentNumber)
        api.profileByStudentNumber("eq.$number").firstOrNull()?.toStudentAccount()
            ?: throw ApiError.NotFound
    }

    /** Records a top-up already paid on the speed point. */
    open suspend fun topUp(studentNumber: String, amount: Money): Result<StudentAccount> = call {
        val result = functions.loadWallet(LoadWalletBody(studentNumber, amount.cents / 100.0))
        StudentAccount(
            id = result.id,
            fullName = result.fullName,
            studentNumber = result.studentNumber,
            balance = Money.fromDecimal(result.walletBalance),
        )
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(io) {
        runCatching { block() }.recoverCatching { throw it.asApiError() }
    }
}
