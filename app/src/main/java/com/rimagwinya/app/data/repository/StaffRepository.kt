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
import com.rimagwinya.app.domain.model.WalletEntry
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

    /**
     * Students whose number or name contains [query]. An exact number match
     * comes first, so scanning a card still lands on one obvious row.
     */
    open suspend fun searchStudents(query: String): Result<List<StudentAccount>> = call {
        val term = studentSearchTerm(query)
        if (term.isEmpty()) return@call emptyList()
        val found = api.searchStudents("(student_number.ilike.*$term*,full_name.ilike.*$term*)")
            .map { it.toStudentAccount() }
        val exact = Validation.normaliseStudentNumber(query)
        found.sortedByDescending { it.studentNumber == exact }
    }

    open suspend fun walletHistory(studentId: String): Result<List<WalletEntry>> = call {
        api.walletHistory("eq.$studentId").map { it.toDomain() }
    }

    /**
     * Records a top-up already paid on the speed point. The function only
     * returns the new balance, so the rest of [student] is carried over.
     */
    open suspend fun topUp(student: StudentAccount, amount: Money): Result<StudentAccount> = call {
        val result = functions.loadWallet(LoadWalletBody(student.studentNumber, amount.cents / 100.0))
        student.copy(balance = Money.fromDecimal(result.walletBalance))
    }

    companion object {
        /**
         * The search text made safe for a PostgREST `or=(...)` filter:
         * commas, brackets, quotes and wildcards would change the filter's
         * meaning, so they are dropped.
         */
        fun studentSearchTerm(query: String): String =
            query.trim().filterNot { it in ",()*\"\\:" }.replace(Regex("\\s+"), " ")
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(io) {
        runCatching { block() }.recoverCatching { throw it.asApiError() }
    }
}
