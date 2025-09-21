package be.sgl.backend.repository.shop

import be.sgl.backend.entity.shop.Shop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ShopRepository : JpaRepository<Shop, Int> {
    @Query("from Shop order by open desc")
    fun findAllRecentFirst(): List<Shop>
    @Query("from Shop where now() between open and closed and not cancelled order by open")
    fun findAllVisibleShops(): List<Shop>
}