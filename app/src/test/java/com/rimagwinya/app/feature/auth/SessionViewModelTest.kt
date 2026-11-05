package com.rimagwinya.app.feature.auth

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.core.network.ApiError
import com.rimagwinya.app.data.repository.AuthRepository
import com.rimagwinya.app.data.repository.AuthState
import com.rimagwinya.app.data.repository.PushTokenRepository
import com.rimagwinya.app.domain.model.Profile
import com.rimagwinya.app.domain.model.UserRole
import io.github.jan.supabase.auth.status.SessionStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Who the app thinks is signed in. The bug these guard against: staff signed
 * in, a student's late profile answer arrived, and staff landed in the
 * student app.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>(relaxed = true)
    private val push = mockk<PushTokenRepository>(relaxed = true)
    private val status = MutableSharedFlow<SessionStatus>()
    private val current = MutableStateFlow<Profile?>(null)

    private val staff = profile("staff-id", UserRole.Staff)
    private val student = profile("student-id", UserRole.Student)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { auth.sessionStatus } returns status
        every { auth.currentProfile } returns current
        every { auth.currentUserId() } returns staff.id
        coEvery { auth.profile() } returns Result.success(staff)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `another person's profile never replaces the signed-in one`() = runTest(dispatcher) {
        val vm = SessionViewModel(auth, push)
        advanceUntilIdle()
        status.emit(mockk<SessionStatus.Authenticated>())
        advanceUntilIdle()

        current.value = student
        advanceUntilIdle()

        assertEquals(AuthState.SignedIn(staff), vm.state.value)
    }

    @Test
    fun `a newer copy of the same profile is taken`() = runTest(dispatcher) {
        val vm = SessionViewModel(auth, push)
        advanceUntilIdle()
        status.emit(mockk<SessionStatus.Authenticated>())
        advanceUntilIdle()

        val richer = staff.copy(walletBalance = Money.fromDecimal(50.0))
        current.value = richer
        advanceUntilIdle()

        assertEquals(AuthState.SignedIn(richer), vm.state.value)
    }

    @Test
    fun `a failed refresh keeps someone already signed in`() = runTest(dispatcher) {
        val vm = SessionViewModel(auth, push)
        advanceUntilIdle()
        status.emit(mockk<SessionStatus.Authenticated>())
        advanceUntilIdle()

        coEvery { auth.profile() } returns Result.failure(java.io.IOException("offline"))
        status.emit(mockk<SessionStatus.Authenticated>())
        status.emit(mockk<SessionStatus.RefreshFailure>())
        advanceUntilIdle()

        assertEquals(AuthState.SignedIn(staff), vm.state.value)
    }

    private fun profile(id: String, role: UserRole) = Profile(
        id = id, fullName = "Name", email = "$id@x.co", studentNumber = id, phone = null,
        role = role, walletBalance = Money.ZERO, noShowCount = 0, language = "en",
    )
}
