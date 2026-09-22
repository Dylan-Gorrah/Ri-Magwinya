package com.rimagwinya.app.feature.profile

import com.rimagwinya.app.core.util.FieldResult
import com.rimagwinya.app.core.util.Problem
import com.rimagwinya.app.core.util.Validation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Editing your own profile: the name staff read off the order, and an
 * optional phone number for when something goes wrong with it.
 */
class EditProfileTest {

    private fun valid(number: String) = Validation.phone(number) is FieldResult.Valid

    @Test
    fun `South African numbers are accepted however they are written`() {
        assertTrue(valid("0821234567"))
        assertTrue(valid("082 123 4567"))
        assertTrue(valid("+27 82 123 4567"))
        assertTrue(valid("+27821234567"))
        assertTrue(valid("(021) 555-1234"))
    }

    @Test
    fun `wrong lengths and rubbish are refused`() {
        assertFalse("too short", valid("082 123"))
        assertFalse("too long", valid("08212345678"))
        assertFalse("not a number", valid("call me"))
        assertFalse("must start with 0", valid("8212345678"))
    }

    @Test
    fun `no phone number is not an error - it is optional`() {
        assertEquals(FieldResult.Empty, Validation.phone(""))
        assertEquals(FieldResult.Empty, Validation.phone("   "))
        assertFalse(EditProfileForm(fullName = "Thabo Mokoena", phone = "").phoneError)
    }

    @Test
    fun `the message names the shape people should type`() {
        val problem = (Validation.phone("nope") as FieldResult.Invalid).problem
        assertEquals(Problem.PhoneMalformed, problem)
    }

    @Test
    fun `a good form saves`() {
        assertTrue(EditProfileForm(fullName = "Thabo Mokoena", phone = "082 123 4567").canSave)
        assertTrue("phone may be left out", EditProfileForm(fullName = "Thabo Mokoena").canSave)
    }

    @Test
    fun `an empty name blocks saving - it is what staff call out`() {
        assertFalse(EditProfileForm(fullName = "", phone = "082 123 4567").canSave)
        assertFalse(EditProfileForm(fullName = " ", phone = "082 123 4567").canSave)
    }

    @Test
    fun `a malformed phone blocks saving`() {
        assertFalse(EditProfileForm(fullName = "Thabo Mokoena", phone = "12").canSave)
    }

    @Test
    fun `it cannot be saved twice`() {
        assertFalse(EditProfileForm(fullName = "Thabo Mokoena", saving = true).canSave)
    }
}
