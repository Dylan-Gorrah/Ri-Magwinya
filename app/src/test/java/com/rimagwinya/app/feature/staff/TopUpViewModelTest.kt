package com.rimagwinya.app.feature.staff

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.data.repository.StaffRepository
import com.rimagwinya.app.domain.model.StudentAccount
import com.rimagwinya.app.domain.model.WalletEntry
import com.rimagwinya.app.domain.model.WalletEntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** Staff finding a student by number or name, seeing their wallet, topping up. */
@OptIn(ExperimentalCoroutinesApi::class)
class TopUpViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val staff = mockk<StaffRepository>()

    private val thabo = StudentAccount(
        id = "t", fullName = "Thabo Mokoena", studentNumber = "ST1234567",
        balance = Money.ofRands(20), email = "thabo@example.com", phone = "082 123 4567",
    )
    private val lerato = thabo.copy(id = "l", fullName = "Lerato Mokoena", studentNumber = "ST7654321")
    private val topUp = WalletEntry(Money.ofRands(50), WalletEntryType.TopUp, Instant.parse("2026-09-22T08:00:00Z"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { staff.walletHistory(any()) } returns Result.success(listOf(topUp))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `one match goes straight to the student and loads their history`() = runTest(dispatcher) {
        coEvery { staff.searchStudents("ST1234567") } returns Result.success(listOf(thabo))
        val vm = TopUpViewModel(staff)

        vm.onQuery("ST1234567")
        vm.search()
        advanceUntilIdle()

        assertEquals(thabo, vm.state.value.student)
        assertEquals(listOf(topUp), vm.state.value.history)
        assertFalse(vm.state.value.historyLoading)
    }

    @Test
    fun `several matches wait for staff to pick one`() = runTest(dispatcher) {
        coEvery { staff.searchStudents("Mokoena") } returns Result.success(listOf(lerato, thabo))
        val vm = TopUpViewModel(staff)

        vm.onQuery("Mokoena")
        vm.search()
        advanceUntilIdle()

        assertNull(vm.state.value.student)
        assertEquals(2, vm.state.value.results?.size)

        vm.select(thabo)
        advanceUntilIdle()
        assertEquals(thabo, vm.state.value.student)
    }

    @Test
    fun `no match shows an empty result rather than an error`() = runTest(dispatcher) {
        coEvery { staff.searchStudents(any()) } returns Result.success(emptyList())
        val vm = TopUpViewModel(staff)

        vm.onQuery("nobody")
        vm.search()
        advanceUntilIdle()

        assertEquals(emptyList<StudentAccount>(), vm.state.value.results)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `a top-up updates the balance and stays on the student`() = runTest(dispatcher) {
        coEvery { staff.searchStudents(any()) } returns Result.success(listOf(thabo))
        val after = thabo.copy(balance = Money.ofRands(70))
        coEvery { staff.topUp(thabo, Money.ofRands(50)) } returns Result.success(after)
        val vm = TopUpViewModel(staff)
        vm.onQuery("ST1234567")
        vm.search()
        advanceUntilIdle()

        vm.choosePreset(Money.ofRands(50))
        vm.askConfirm()
        assertTrue(vm.state.value.confirming)
        vm.confirm()
        advanceUntilIdle()

        with(vm.state.value) {
            assertEquals(after, student)
            assertEquals(Money.ofRands(50), added)
            assertNull(amount)
            assertFalse(adding)
        }
        // Once on selecting, once more after the top-up so it shows.
        coVerify(exactly = 2) { staff.walletHistory("t") }
    }

    @Test
    fun `an amount outside the range cannot be added`() = runTest(dispatcher) {
        coEvery { staff.searchStudents(any()) } returns Result.success(listOf(thabo))
        val vm = TopUpViewModel(staff)
        vm.onQuery("ST1234567")
        vm.search()
        advanceUntilIdle()

        vm.onCustom("5")
        assertFalse(vm.state.value.canAdd)
        vm.onCustom("5000")
        assertFalse(vm.state.value.canAdd)
        vm.onCustom("75")
        assertTrue(vm.state.value.canAdd)
    }

    @Test
    fun `search text is made safe for the PostgREST filter`() {
        assertEquals("ST10 Smith", StaffRepository.studentSearchTerm("  ST10, (Smith)*  "))
        assertEquals("", StaffRepository.studentSearchTerm(" ,() "))
    }
}
