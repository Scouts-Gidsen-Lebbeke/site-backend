package be.sgl.backend.service.mapper

import be.sgl.backend.dto.ActivityDTO
import be.sgl.backend.entity.registrable.activity.Activity
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface ActivityMapper {
    fun toDto(activity: Activity): ActivityDTO
    fun toEntity(dto: ActivityDTO): Activity
}