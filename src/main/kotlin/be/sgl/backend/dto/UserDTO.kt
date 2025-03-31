package be.sgl.backend.dto

import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.util.Nis
import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class UserDTO(
    val username: String?,
    @NotBlank
    val name: String,
    @NotBlank
    val firstName: String,
    @Email
    @NotBlank
    val email: String,
    val image: String?,
    val level: RoleLevel,
    val memberId: String?,
    val birthdate: LocalDate,
    @PhoneNumber
    val mobile: String?,
    @Nis
    val nis: String?,
    val accountNo: String?,
    val sex: Sex,
    val hasReduction: Boolean,
    val addresses: List<AddressDTO>,
    val contacts: List<ContactDTO>
)

data class StaffDTO(
    val name: String,
    val firstName: String,
    val image: String?,
    val nickname: String?,
    val totem: String?,
)