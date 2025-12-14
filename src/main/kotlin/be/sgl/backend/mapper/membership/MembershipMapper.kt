package be.sgl.backend.mapper.membership

import be.sgl.backend.dto.membership.MembershipDTO
import be.sgl.backend.entity.membership.Membership
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface MembershipMapper {
    fun toDto(membership: Membership): MembershipDTO
    fun toEntity(dto: MembershipDTO): Membership
}