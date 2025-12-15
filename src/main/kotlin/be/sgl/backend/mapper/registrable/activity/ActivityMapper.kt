package be.sgl.backend.mapper.registrable.activity

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.dto.registrable.activity.*
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.registrable.activity.Activity
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface ActivityMapper {
    fun toDto(activity: Activity): ActivityDTO
    fun toBaseDto(activity: Activity): ActivityBaseDTO
    fun toEntity(dto: CreateOrUpdateActivityRequest): Activity
    fun toEntity(dto: AddressDTO): Address
}