package be.sgl.backend.dto

import be.sgl.backend.entity.shop.DeliveryOption
import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

// read-only, no validation
data class ShopOrderDTO(
    val id: Int?,
    val price: Double,
    val paid: Boolean,
    val completed: Boolean,
    val items: List<OrderItemDTO>,
    val remarks: String?,
    val name: String,
    val firstName: String,
    val email: String,
    val mobile: String?,
    val subscribable: ShopBaseDTO,
    val deliveryOption: DeliveryOption,
    val pickupFrom: LocalDateTime?,
    val pickupTo: LocalDateTime?,
    val deliveryFrom: LocalDateTime?,
    val deliveryTo: LocalDateTime?,
    val shippingOrDeliveryAddress: AddressDTO?,
    val arrangedContactPerson: String?
)

data class OrderItemDTO(
    val id: Int?,
    val shopItem: ShopItemDTO,
    var amount: Int,
    var option: String?
)

data class ShopOrderAttemptData(
    @field:NotBlank(message = "{NotBlank.shop.order.name}")
    var name: String?,
    @field:NotBlank(message = "{NotBlank.shop.order.firstName}")
    var firstName: String?,
    @field:NotBlank(message = "{NotBlank.shop.order.email}")
    @field:Email(message = "{Email.shop.order.email}")
    var email: String?,
    @PhoneNumber(message = "{PhoneNumber.shop.order.mobile}")
    var mobile: String?,
    var orderItems: List<OrderItemAttemptData>
)

data class OrderItemAttemptData(
    @field:NotNull(message = "{NotNull.shop.order.item.id}")
    var shopItemId: Int?,
    @field:Positive(message = "{Positive.shop.order.item.amount}")
    var amount: Int?,
    var option: String?,
)