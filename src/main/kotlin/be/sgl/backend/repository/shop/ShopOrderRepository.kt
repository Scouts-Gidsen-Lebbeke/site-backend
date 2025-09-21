package be.sgl.backend.repository.shop

import be.sgl.backend.entity.shop.Shop
import be.sgl.backend.entity.shop.ShopOrder
import be.sgl.backend.repository.PaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ShopOrderRepository : JpaRepository<ShopOrder, Int>, PaymentRepository<ShopOrder> {
    @Query("from ShopOrder where shop = :shop and paid")
    fun getPaidOrdersByShop(shop: Shop): List<ShopOrder>
    @Query("select price from ShopOrder where shop = :shop and paid")
    fun getPaidOrderPricesByShop(shop: Shop): List<Double>
    @Query("select count(*) from ShopOrder where shop = :shop and paid")
    fun countPaidOrdersByShop(shop: Shop): Int
    @Query("from ShopOrder where subscribable = :event")
    fun getOrdersByShop(shop: Shop): List<ShopOrder>
}