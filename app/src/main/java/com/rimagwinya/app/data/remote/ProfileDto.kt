package com.rimagwinya.app.data.remote

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.Profile
import com.rimagwinya.app.domain.model.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    @SerialName("student_number") val studentNumber: String? = null,
    val phone: String? = null,
    val role: String,
    @SerialName("wallet_balance") val walletBalance: Double = 0.0,
    @SerialName("no_show_count") val noShowCount: Int = 0,
    val language: String = "en",
)

fun ProfileDto.toDomain(): Profile = Profile(
    id = id,
    fullName = fullName,
    email = email,
    studentNumber = studentNumber,
    phone = phone,
    role = UserRole.from(role),
    walletBalance = Money.fromDecimal(walletBalance),
    noShowCount = noShowCount,
    language = language,
)

/**
 * The only columns a student may change on their own profile. The database
 * enforces this with column grants, so anything else here would simply be
 * refused.
 *
 * `student_number` is absent on purpose — it is set once, through the
 * `claim_student_number` function, so nobody can rewrite theirs to take a
 * number that belongs to someone else.
 */
@Serializable
data class ProfilePatch(
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val language: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
)

@Serializable
data class ClaimStudentNumberBody(
    val p_number: String,
)
