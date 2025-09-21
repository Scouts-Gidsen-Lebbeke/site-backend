package be.sgl.backend.dto

import be.sgl.backend.entity.shop.DeliveryOption
import be.sgl.backend.entity.shop.Shop
import be.sgl.backend.entity.shop.ShopOrder
import be.sgl.backend.entity.shop.ShopStatus
import be.sgl.backend.entity.shop.ShopStatus.Companion.getStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.io.Serializable
import java.time.LocalDateTime

// DTO for a list overview of shops
@Schema(description = "Basic information about a shop.")
open class ShopBaseDTO(
    val id: Int?,
    @NotBlank(message = "{NotBlank.shop.name}")
    @Size(max = 50, message = "{Size.shop.name}")
    val name: String,
    @NotNull(message = "{NotNull.shop.start}")
    var open: LocalDateTime,
    @NotNull(message = "{NotNull.shop.closed}")
    var closed: LocalDateTime,
    var cancellable: Boolean
) : Serializable

// DTO for registration page and CRUD
@Schema(description = "The complete shop configuration.")
class ShopDTO(
    id: Int?,
    name: String,
    open: LocalDateTime,
    closed: LocalDateTime,
    cancellable: Boolean,
    @NotBlank(message = "{NotBlank.shop.description}")
    var description: String,
    var sendConfirmation: Boolean,
    var sendCompleteConfirmation: Boolean,
    @Email(message = "{Email.shop.communicationCC}")
    var communicationCC: String?,
    var cancelled: Boolean,
    var needsMobile: Boolean,
    @NotEmpty(message = "{NotEmpty.shop.delivery.options}")
    var deliveryOptions: List<DeliveryOption>,
    var pickupFrom: LocalDateTime?,
    var pickupTo: LocalDateTime?,
    var specifyPickupTime: Boolean,
    var deliveryFrom: LocalDateTime?,
    var deliveryTo: LocalDateTime?,
    var specifyDeliveryTime: Boolean,
    var deliveryCost: Double?,
    var shippingCost: Double?
) : ShopBaseDTO(id, name, open, closed, cancellable)

data class ShopItemDTO(
    var name: String,
    var info: String,
    var price: Double,
    var image: String?,
    var stock: Int?
)

// DTO for statistics list overview
class ShopResultDTO(
    id: Int?,
    name: String,
    open: LocalDateTime,
    closed: LocalDateTime,
    cancellable: Boolean,
    var orderCount: Int,
    var totalPrice: Double,
    var status: ShopStatus
) : ShopBaseDTO(id, name, open, closed, cancellable) {
    constructor(shop: Shop, orders: List<ShopOrder>) :
            this(shop.id, shop.name, shop.open, shop.closed, shop.cancellable, orders.count(), orders.sumOf { it.price }, shop.getStatus(orders))
}