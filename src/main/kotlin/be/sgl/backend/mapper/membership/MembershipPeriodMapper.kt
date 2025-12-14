package be.sgl.backend.mapper.membership

import be.sgl.backend.dto.membership.MembershipPeriodDTO
import be.sgl.backend.entity.membership.MembershipPeriod
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface MembershipPeriodMapper {
    fun toDto(membership: MembershipPeriod): MembershipPeriodDTO
    fun toEntity(dto: MembershipPeriodDTO): MembershipPeriod
}