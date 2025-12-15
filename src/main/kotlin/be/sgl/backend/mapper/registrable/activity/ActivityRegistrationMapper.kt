package be.sgl.backend.mapper.registrable.activity

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.dto.registrable.activity.*
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface ActivityRegistrationMapper {
    fun toDto(activity: Activity): ActivityDTO
    fun toBaseDto(activity: Activity): ActivityBaseDTO
    fun toEntity(dto: ActivityDTO): Activity
    fun toDto(registration: ActivityRegistration): ActivityRegistrationDTO
    fun toEntity(dto: ActivityRegistrationDTO): ActivityRegistration
    fun toDto(restriction: ActivityRestriction): ActivityRestrictionDTO
    fun toEntity(dto: ActivityRestrictionDTO): ActivityRestriction
    fun toDto(status: ActivityRegistrationStatus): ActivityRegistrationStatusDTO
    fun toEntity(dto: AddressDTO): Address
}