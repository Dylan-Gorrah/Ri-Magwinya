package com.rimagwinya.app.feature.auth

import androidx.lifecycle.SavedStateHandle
import com.rimagwinya.app.R
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.core.network.ConflictCode
import com.rimagwinya.app.data.repository.AuthRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The two sign-in screens: student (the default) and staff. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { auth.signIn(any(), any(), any()) } returns Result.success(mockk(relaxed = true))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(staff: Boolean) =
        LoginViewModel(auth, SavedStateHandle(mapOf("staffHint" to staff)))

    private fun LoginViewModel.fill() {
        onEmail("someone@gmail.com")
        onPassword("password123")
    }

    @Test
    fun `the staff card opens the staff screen, with no sign-up`() {
        assertTrue(vm(staff = true).state.value.staffOnly)
        assertFalse(vm(staff = false).state.value.staffOnly)
    }

    @Test
    fun `the staff screen asks for a staff account`() = runTest(dispatcher) {
        val vm = vm(staff = true)
        vm.fill()
        vm.submit()
        advanceUntilIdle()
        coVerify { auth.signIn("someone@gmail.com", "password123", staffOnly = true) }
    }

    @Test
    fun `the student screen lets anyone in, and the role decides where`() = runTest(dispatcher) {
        val vm = vm(staff = false)
        vm.fill()
        vm.submit()
        advanceUntilIdle()
        coVerify { auth.signIn("someone@gmail.com", "password123", staffOnly = false) }
    }

    @Test
    fun `a student on the staff screen is told, and can try again`() = runTest(dispatcher) {
        coEvery { auth.signIn(any(), any(), true) } returns
            Result.failure(ApiError.Conflict(ConflictCode.NOT_STAFF))
        val vm = vm(staff = true)
        vm.fill()
        vm.submit()
        advanceUntilIdle()

        assertEquals(R.string.error_not_staff, vm.state.value.formError)
        assertFalse(vm.state.value.isSubmitting)
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `a wrong password shows the error and stops the spinner`() = runTest(dispatcher) {
        coEvery { auth.signIn(any(), any(), any()) } returns
            Result.failure(ApiError.Conflict(ConflictCode.BAD_CREDENTIALS))
        val vm = vm(staff = false)
        vm.fill()
        vm.submit()
        advanceUntilIdle()

        assertEquals(R.string.error_credentials, vm.state.value.formError)
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `no signal shows the network error`() = runTest(dispatcher) {
        coEvery { auth.signIn(any(), any(), any()) } returns
            Result.failure(ApiError.Offline(java.io.IOException("no route")))
        val vm = vm(staff = false)
        vm.fill()
        vm.submit()
        advanceUntilIdle()

        assertEquals(R.string.error_network, vm.state.value.formError)
    }

    @Test
    fun `a double tap sends one sign-in, and the button stays busy after success`() = runTest(dispatcher) {
        val vm = vm(staff = false)
        vm.fill()
        vm.submit()
        vm.submit()
        advanceUntilIdle()

        coVerify(exactly = 1) { auth.signIn(any(), any(), any()) }
        assertFalse(vm.state.value.canSubmit)
    }

    @Test
    fun `empty fields cannot be submitted`() = runTest(dispatcher) {
        val vm = vm(staff = false)
        vm.submit()
        advanceUntilIdle()
        coVerify(exactly = 0) { auth.signIn(any(), any(), any()) }
    }

    @Test
    fun `typing again clears the old error`() = runTest(dispatcher) {
        coEvery { auth.signIn(any(), any(), any()) } returns
            Result.failure(ApiError.Conflict(ConflictCode.BAD_CREDENTIALS))
        val vm = vm(staff = false)
        vm.fill()
        vm.submit()
        advanceUntilIdle()
        vm.onPassword("password1234")
        assertEquals(null, vm.state.value.formError)
    }
}
