package be.sgl.backend.dto.user

import be.sgl.backend.people.domain.User
import io.swagger.v3.oas.annotations.media.Schema

// read-only
@Schema(name = "Staff")
data class StaffDTO(
    val name: String,
    val firstName: String,
    val image: String?,
    val nickname: String?,
    val totem: String?,
) {
    companion object {
        fun of(user: User, branchId: Int): StaffDTO {
            return StaffDTO(user.name, user.firstName, user.image, user.staffData.nicknames[branchId], user.staffData.totem)
        }
    }
}
