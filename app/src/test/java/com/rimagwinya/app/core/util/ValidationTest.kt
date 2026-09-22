package com.rimagwinya.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {

    // -----------------------------------------------------------------
    // Email
    // -----------------------------------------------------------------

    @Test
    fun `ordinary addresses are accepted`() {
        // Students use personal accounts, so these must all work.
        listOf(
            "thabo@gmail.com",
            "thabo.mokoena@gmail.com",
            "thabo+tuckshop@gmail.com",
            "t@outlook.co.za",
            "someone@rcconnect.edu.za",
        ).forEach {
            assertTrue(it, Validation.email(it).isValid)
        }
    }

    @Test
    fun `no college domain is enforced`() {
        // The prototype required @rcconnect.edu.za. That rule is gone: a
        // personal Gmail is a perfectly valid account.
        assertTrue(Validation.email("anyone@gmail.com").isValid)
    }

    @Test
    fun `malformed addresses are rejected`() {
        listOf(
            "thabo",
            "thabo@",
            "@gmail.com",
            "thabo@gmail",
            "thabo gmail.com",
            "thabo@@gmail.com",
            "thabo@gmail..com",
        ).forEach {
            assertEquals(it, Problem.EmailMalformed, Validation.email(it).problemOrNull)
        }
    }

    @Test
    fun `an empty field is not yet an error`() {
        // Nothing typed means the button is disabled, not that the person is
        // told off before they have started.
        assertEquals(FieldResult.Empty, Validation.email(""))
        assertEquals(null, Validation.email("").problemOrNull)
        assertFalse(Validation.email("").isValid)
    }

    @Test
    fun `surrounding whitespace is tolerated`() {
        assertTrue(Validation.email("  thabo@gmail.com  ").isValid)
    }

    // -----------------------------------------------------------------
    // Password
    // -----------------------------------------------------------------

    @Test
    fun `password must be at least eight characters`() {
        assertEquals(Problem.PasswordTooShort, Validation.password("short12").problemOrNull)
        assertTrue(Validation.password("eight888").isValid)
        assertTrue(Validation.password("a much longer passphrase").isValid)
    }

    @Test
    fun `password whitespace is meaningful and not trimmed`() {
        // Trimming a password silently changes what someone typed, and they
        // would then fail to sign in on another device that did not trim.
        assertTrue(Validation.password("  pass  ").isValid)
    }

    // -----------------------------------------------------------------
    // Student number
    // -----------------------------------------------------------------

    @Test
    fun `student numbers accept the shapes colleges actually use`() {
        listOf("ST2024001", "2024001", "st-2024-001", "ABC123").forEach {
            assertTrue(it, Validation.studentNumber(it).isValid)
        }
    }

    @Test
    fun `student numbers reject junk`() {
        listOf("ab", "!!!!!!", "ST 2024 001", "-leading").forEach {
            assertEquals(it, Problem.StudentNumberMalformed,
                Validation.studentNumber(it).problemOrNull)
        }
    }

    @Test
    fun `student numbers are normalised to one casing`() {
        // The column is unique, so st2024001 and ST2024001 must not be able
        // to both exist and confuse staff at the counter.
        assertEquals("ST2024001", Validation.normaliseStudentNumber(" st2024001 "))
    }

    @Test
    fun `emails are normalised to lower case`() {
        assertEquals("thabo@gmail.com", Validation.normaliseEmail("  Thabo@Gmail.COM "))
    }

    // -----------------------------------------------------------------
    // Name
    // -----------------------------------------------------------------

    @Test
    fun `name needs more than one character`() {
        assertEquals(Problem.NameTooShort, Validation.fullName("T").problemOrNull)
        assertTrue(Validation.fullName("Thabo Mokoena").isValid)
    }
}
