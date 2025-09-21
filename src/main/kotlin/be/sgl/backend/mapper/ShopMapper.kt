package be.sgl.backend.mapper

import be.sgl.backend.dto.*
import be.sgl.backend.entity.shop.Shop
import be.sgl.backend.entity.shop.ShopOrder
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface ShopMapper {
    fun toDto(shop: Shop): ShopDTO
    fun toBaseDto(shop: Shop): ShopBaseDTO
    fun toEntity(dto: ShopDTO): Shop
    fun toDto(order: ShopOrder): ShopOrderDTO
    fun toEntity(dto: ShopOrderDTO): ShopOrder
}