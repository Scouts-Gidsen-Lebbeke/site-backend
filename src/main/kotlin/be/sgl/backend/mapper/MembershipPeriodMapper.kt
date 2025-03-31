package be.sgl.backend.mapper

import be.sgl.backend.dto.MembershipPeriodDTO
import be.sgl.backend.entity.membership.MembershipPeriod
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface MembershipPeriodMapper {
    fun toDto(membership: MembershipPeriod): MembershipPeriodDTO
    fun toEntity(dto: MembershipPeriodDTO): MembershipPeriod
}