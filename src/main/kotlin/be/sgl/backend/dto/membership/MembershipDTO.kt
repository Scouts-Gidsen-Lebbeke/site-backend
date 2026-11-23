package be.sgl.backend.dto.membership

import be.sgl.backend.dto.branch.BranchBaseDTO
import be.sgl.backend.dto.user.UserDTO
import be.sgl.backend.entity.membership.Membership
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// read-only
@Schema(name = "Membership")
data class MembershipDTO(
    val id: Int?,
    val period: MembershipPeriodBaseDTO,
    val branch: BranchBaseDTO,
    val user: UserDTO,
    val price: Double,
    val paid: Boolean,
    val createdDate: LocalDateTime?
) {
    companion object {
        fun of(membership: Membership): MembershipDTO {
            return MembershipDTO(membership.id, MembershipPeriodBaseDTO.of(membership.period),
                BranchBaseDTO.of(membership.branch), UserDTO.of(membership.user), membership.price, membership.paid,
                membership.createdDate)
        }
    }
}
