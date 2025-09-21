package be.sgl.backend.service.shop

import be.sgl.backend.dto.ShopBaseDTO
import be.sgl.backend.dto.ShopDTO
import be.sgl.backend.dto.ShopResultDTO
import be.sgl.backend.entity.shop.Shop
import be.sgl.backend.mapper.ShopMapper
import be.sgl.backend.repository.shop.ShopOrderRepository
import be.sgl.backend.repository.shop.ShopRepository
import be.sgl.backend.service.exception.ShopNotFoundException
import be.sgl.backend.service.payment.CheckoutProvider
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ShopService(
    private val shopRepository: ShopRepository,
    private val shopOrderRepository: ShopOrderRepository,
    private val mapper: ShopMapper,
    private val checkoutProvider: CheckoutProvider
) {

    private val logger = KotlinLogging.logger {}

    fun getAllShops(): List<ShopResultDTO> {
        logger.debug { "Fetching all shops" }
        return shopRepository.findAllRecentFirst().map { ShopResultDTO(it, shopOrderRepository.getPaidOrdersByShop(it)) }
    }

    fun getVisibleShops(): List<ShopBaseDTO> {
        logger.debug { "Fetching all visible shops" }
        return shopRepository.findAllVisibleShops().map(mapper::toBaseDto)
    }

    fun getShopDTOById(id: Int): ShopDTO {
        logger.debug { "Fetching shop #$id" }
        return mapper.toDto(getShopById(id))
    }

    fun saveShopDTO(dto: ShopDTO): ShopDTO {
        logger.info { "Saving new shop ${dto.name} (${dto.open} - ${dto.closed})" }
        check(LocalDateTime.now() < dto.closed) { "New shops cannot be closed for orders yet!" }
        validateShopDTO(dto)
        val newShop = mapper.toEntity(dto)
        return mapper.toDto(shopRepository.save(newShop))
    }

    fun mergeShopDTOChanges(id: Int, dto: ShopDTO): ShopDTO {
        logger.info { "Updating shop #$id" }
        validateShopDTO(dto)
        val shop = getShopById(id)
        // update this first, maybe the status alters
        shop.closed = dto.closed
        check(shop.cancelled) { "A cancelled event cannot be edited anymore!" }
        check(shop.isClosed) { "A shop with closed orders cannot be edited anymore!" }
        if (shop.isNotYetOpen) {
            logger.info { "Shop orders are not yet open, shop can be fully edited" }
            // price and user data collection can only be altered if no registration was possible yet
            shop.needsMobile = dto.needsMobile
            check(dto.cancellable || !shop.cancellable) { "A previously cancellable event cannot be made uncancellable!" }
            shop.cancellable = dto.cancellable
            shop.name = dto.name
            // One can only delay or advance the registration period when it wasn't open yet
            shop.open = dto.open
        } else {
            logger.info { "Shop orders are already open, new shop item configuration should respect old one" }

        }
        shop.sendConfirmation = dto.sendConfirmation
        shop.sendCompleteConfirmation = dto.sendCompleteConfirmation
        shop.communicationCC = dto.communicationCC
        shop.description = dto.description
        return mapper.toDto(shopRepository.save(shop))
    }

    fun cancelShop(id: Int) {
        logger.info { "Cancel shop #$id..." }
        val shop = getShopById(id)
        check(shop.cancelled) { "This shop is already cancelled!" }
        check(shop.isClosed) { "A closed shop cannot be cancelled anymore!" }
        val orders = shopOrderRepository.getOrdersByShop(shop)
        if (orders.isNotEmpty()) {
            logger.info { "Shop has ${orders.size} linked registrations needing a refund..." }
            orders.forEach {
                checkoutProvider.refundPayment(it)
                logger.info { "Refund request sent for registration #${it.id}" }
            }
        }
        logger.info { "Registrations fully checked, marking event as cancelled..." }
        shop.cancelled = true
        shopRepository.save(shop)
        logger.info { "Shop successfully cancelled" }
    }

    private fun validateShopDTO(dto: ShopDTO) {
        check(dto.open < dto.closed) { "The closure of orders should be after the opening of orders!" }
    }

    private fun getShopById(id: Int): Shop {
        return shopRepository.findById(id).orElseThrow { ShopNotFoundException() }
    }
}