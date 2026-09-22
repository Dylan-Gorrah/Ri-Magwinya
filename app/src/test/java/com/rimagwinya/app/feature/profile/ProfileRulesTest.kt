package com.rimagwinya.app.feature.profile

import com.rimagwinya.app.core.datastore.Settings
import com.rimagwinya.app.core.designsystem.theme.ThemeChoice
import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.Profile
import com.rimagwinya.app.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Settings defaults, the loyalty card, and the change-password form. */
class ProfileRulesTest {

    private fun state(stamps: Int = 0, role: UserRole = UserRole.Student) = ProfileUiState(
        profile = Profile(
            id = "p", fullName = "Test Student", email = "t@x.co", studentNumber = "TEST001",
            role = role, walletBalance = Money.ofRands(33), noShowCount = 0, language = "en",
        ),
        stamps = stamps,
    )

    @Test
    fun `defaults are what a new install should do`() {
        val defaults = Settings()
        assertEquals(ThemeChoice.System, defaults.theme)
        assertEquals("en", defaults.language)
        assertTrue("order notifications on by default", defaults.orderNotifications)
        assertFalse("the app is not locked until asked", defaults.biometricUnlock)
    }

    @Test
    fun `the loyalty card counts round to ten`() {
        assertEquals(0, state(stamps = 0).stampProgress)
        assertEquals(7, state(stamps = 7).stampProgress)
        // An eleventh collected order starts the next card.
        assertEquals(0, state(stamps = 10).stampProgress)
        assertEquals(1, state(stamps = 11).stampProgress)
    }

    @Test
    fun `wallet and loyalty are for students, not staff`() {
        assertFalse(state().isStaff)
        assertTrue(state(role = UserRole.Staff).isStaff)
    }

    @Test
    fun `a new password must be long enough and typed twice`() {
        assertFalse("too short", PasswordForm("short12", "short12").canSave)
        assertFalse("mismatch", PasswordForm("longenough1", "longenough2").canSave)
        assertFalse("blank", PasswordForm().canSave)
        assertTrue(PasswordForm("longenough1", "longenough1").canSave)
        assertFalse("not while saving", PasswordForm("longenough1", "longenough1", saving = true).canSave)
    }
}
