package com.rimagwinya.app.core.util

import com.rimagwinya.app.core.config.AppConfig

/**
 * Form validation, as pure functions so they can be unit tested without a
 * screen or a device.
 *
 * Note what is missing: any check on the email domain. Students use ordinary
 * personal accounts, so a well-formed address is the whole rule. The student
 * number is what actually identifies someone.
 */
object Validation {

    /**
     * Deliberately permissive. The only thing the app gains from a strict
     * pattern is turning away people with unusual but legitimate addresses;
     * whether the address really exists is settled by the confirmation email,
     * not by a regular expression.
     */
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$")

    /**
     * Letters and digits, with optional separators. Colleges write these
     * many ways, so this checks shape rather than a specific format.
     */
    private val STUDENT_NUMBER = Regex("^[A-Za-z0-9][A-Za-z0-9/-]{2,19}$")

    fun email(value: String): FieldResult {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> FieldResult.Empty
            !EMAIL.matches(trimmed) -> FieldResult.Invalid(Problem.EmailMalformed)
            else -> FieldResult.Valid
        }
    }

    fun password(value: String): FieldResult = when {
        value.isEmpty() -> FieldResult.Empty
        value.length < AppConfig.PASSWORD_MIN_LENGTH ->
            FieldResult.Invalid(Problem.PasswordTooShort)
        else -> FieldResult.Valid
    }

    fun fullName(value: String): FieldResult = when {
        value.trim().isEmpty() -> FieldResult.Empty
        value.trim().length < 2 -> FieldResult.Invalid(Problem.NameTooShort)
        else -> FieldResult.Valid
    }

    fun studentNumber(value: String): FieldResult {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> FieldResult.Empty
            !STUDENT_NUMBER.matches(trimmed) ->
                FieldResult.Invalid(Problem.StudentNumberMalformed)
            else -> FieldResult.Valid
        }
    }

    /** Student numbers are stored and compared in one casing. */
    fun normaliseStudentNumber(value: String): String = value.trim().uppercase()

    fun normaliseEmail(value: String): String = value.trim().lowercase()
}

sealed interface FieldResult {
    /** Nothing typed yet. Not an error — the button is simply still disabled. */
    data object Empty : FieldResult
    data object Valid : FieldResult
    data class Invalid(val problem: Problem) : FieldResult

    val isValid: Boolean get() = this is Valid

    /** Only show a message once someone has actually typed something wrong. */
    val problemOrNull: Problem? get() = (this as? Invalid)?.problem
}

/**
 * What is wrong, as a value rather than a sentence. The screen turns it into
 * words from strings.xml, which is what keeps Phase 13 a translation job.
 */
enum class Problem {
    EmailMalformed,
    PasswordTooShort,
    NameTooShort,
    StudentNumberMalformed,
}
