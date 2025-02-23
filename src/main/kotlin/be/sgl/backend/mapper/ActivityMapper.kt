package be.sgl.backend.mapper

import be.sgl.backend.dto.ActivityBaseDTO
import be.sgl.backend.dto.ActivityDTO
import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.dto.ActivityRestrictionDTO
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface ActivityMapper {
    fun toDto(activity: Activity): ActivityDTO
    fun toBaseDto(activity: Activity): ActivityBaseDTO
    fun toEntity(dto: ActivityDTO): Activity
    fun toDto(registration: ActivityRegistration): ActivityRegistrationDTO
    fun toEntity(dto: ActivityRegistrationDTO): ActivityRegistration
    fun toDto(restriction: ActivityRestriction): ActivityRestrictionDTO
    fun toEntity(dto: ActivityRestrictionDTO): ActivityRestriction
}