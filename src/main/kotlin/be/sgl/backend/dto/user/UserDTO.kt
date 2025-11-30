package be.sgl.backend.dto.user

import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.entity.user.Sex
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
)