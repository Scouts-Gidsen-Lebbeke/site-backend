package be.sgl.backend.dto

import be.sgl.backend.entity.user.Sex
import java.io.Serializable
import java.time.LocalDate

open class UserDTO(
    val username: String,
    val name: String,
    val firstName: String,
    val image: String?
) : Serializable

class StaffDTO(
    val nickname: String?,
    val totem: String?,
    username: String,
    name: String,
    firstName: String,
    image: String?
) : UserDTO(username, name, firstName, image)

class ExtendedUserDTO(
    val memberId: String?,
    val birthdate: LocalDate,
    val email: String?,
    val mobile: String?,
    val nis: String?,
    val accountNo: String?,
    val sex: Sex,
    val hasReduction: Boolean,
    val address: AddressDTO,
    username: String,
    name: String,
    firstName: String,
    image: String?
) : UserDTO(username, name, firstName, image)