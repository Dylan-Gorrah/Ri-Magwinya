package com.rimagwinya.app.feature.auth

import com.rimagwinya.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The register form's rules, including the confirm-password field.
 *
 * These are the checks that stop an account being created with a typo in
 * the password — after which the only way back is a password reset.
 */
class RegisterFormTest {

    private fun form(
        name: String = "Thabo Mokoena",
        email: String = "thabo@gmail.com",
        number: String = "ST2024001",
        password: String = "vetkoek123",
        confirm: String = "vetkoek123",
    ) = RegisterUiState(
        fullName = name,
        email = email,
        studentNumber = number,
        password = password,
        confirmPassword = confirm,
    )

    @Test
    fun `a complete, matching form can be submitted`() {
        assertTrue(form().canSubmit)
        assertTrue(form().passwordsMatch)
    }

    @Test
    fun `a mismatched confirmation blocks the button`() {
        val mismatched = form(confirm = "vetkoek124")
        assertFalse(mismatched.passwordsMatch)
        assertFalse(mismatched.canSubmit)
    }

    @Test
    fun `the mismatch is explained under the field, once it has been left`() {
        val mismatched = form(confirm = "nope").copy(touched = setOf(RegisterField.ConfirmPassword))
        assertEquals(R.string.password_mismatch, mismatched.errorFor(RegisterField.ConfirmPassword))
    }

    @Test
    fun `nothing nags before a field has been touched`() {
        assertNull(form(email = "not-an-email").errorFor(RegisterField.Email))
    }

    @Test
    fun `each field is checked on its own`() {
        assertFalse("empty name", form(name = " ").canSubmit)
        assertFalse("bad email", form(email = "thabo@").canSubmit)
        assertFalse("short password", form(password = "short12", confirm = "short12").canSubmit)
        assertFalse("no student number", form(number = "").canSubmit)
    }

    @Test
    fun `an empty confirmation is a mismatch, not a pass`() {
        assertFalse(form(confirm = "").canSubmit)
    }

    @Test
    fun `nothing can be submitted twice`() {
        assertFalse(form().copy(isSubmitting = true).canSubmit)
    }

    @Test
    fun `no session after sign-up means the email needs confirming`() {
        val waiting = RegisterUiState(registered = false, awaitingEmailConfirmation = true)
        assertTrue(waiting.awaitingEmailConfirmation)
        assertFalse("and it must not look like a success", waiting.registered)
    }
}
