package com.rimagwinya.app.domain.model

import com.rimagwinya.app.core.money.Money

/**
 * The signed-in person.
 *
 * [role] comes from the server on every sign-in and is never decided by the
 * app. That is the whole reason the welcome screen's two cards are cosmetic.
 */
data class Profile(
    val id: String,
    val fullName: String,
    val email: String,
    /**
     * Null is a real state, not a bug: Google sign-in creates the profile
     * from a token with no student number in it. Such an account can browse
     * and nothing else until the number is supplied.
     */
    val studentNumber: String?,
    /** Optional: staff ring it when food is going cold on the counter. */
    val phone: String?,
    val role: UserRole,
    val walletBalance: Money,
    val noShowCount: Int,
    val language: String,
) {
    val canOrder: Boolean get() = studentNumber != null

    /** Two no-shows withdraws pay-at-counter. */
    val noShowBlocked: Boolean get() = noShowCount >= 2
}

enum class UserRole(val serverName: String) {
    Student("student"),
    Staff("staff");

    companion object {
        /**
         * Anything unrecognised is treated as a student. Failing closed
         * matters here: a future role the app does not know about must not
         * accidentally unlock the staff side.
         */
        fun from(raw: String?): UserRole =
            entries.firstOrNull { it.serverName.equals(raw, ignoreCase = true) } ?: Student
    }
}
