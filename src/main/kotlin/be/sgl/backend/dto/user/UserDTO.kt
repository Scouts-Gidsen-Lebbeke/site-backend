package be.sgl.backend.dto.user

import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.Sex
import be.sgl.backend.entity.user.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// read-only
@Schema(name = "User")
data class UserDTO(
    val username: String?,
    val name: String?,
    val firstName: String?,
    val email: String?,
    val image: String?,
    val level: RoleLevel,
    val memberId: String?,
    val birthdate: LocalDate?,
    val mobile: String?,
    val nis: String?,
    val accountNo: String?,
    val sex: Sex?,
    val hasReduction: Boolean,
    val hasHandicap: Boolean
) {
    companion object {
        fun of(user: User): UserDTO {
            return UserDTO(user.username, user.name, user.firstName, user.email, user.image, user.level, user.memberId,
                user.birthdate, user.mobile, user.nis, user.accountNo, user.sex, user.hasReduction, user.hasHandicap)
        }
    }
}
