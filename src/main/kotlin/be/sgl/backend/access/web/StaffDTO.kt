package be.sgl.backend.access.web

import be.sgl.backend.people.StaffMember
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Staff")
data class StaffDTO(
    val name: String,
    val firstName: String,
    val image: String?,
    val nickname: String?,
    val totem: String?,
) {
    companion object {
        fun of(staffMember: StaffMember): StaffDTO {
            return StaffDTO(staffMember.name, staffMember.firstName, staffMember.image, staffMember.nickname, staffMember.totem)
        }
    }
}
