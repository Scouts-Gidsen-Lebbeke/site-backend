package be.sgl.backend.shop.domain

import be.sgl.backend.shop.domain.ShopTestMother.shop
import be.sgl.backend.shop.domain.ShopOrderTestMother.shopOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ShopOrderTest {

    @Test
    fun `should describe the order by shop name and order id`() {
        // given
        val shop = shop().name("Truien").build()
        val order = shopOrder().id(42).shop(shop).build()

        // when
        val description = order.getDescription()

        // then
        assertThat(description).isEqualTo("${shop.name} #${order.id}")
    }

    @Test
    fun `should have delivery costs only for delivery and shipping`() {
        // given

        // when
        val costs = DeliveryOption.entries.associateWith { it.cost }

        // then
        assertThat(costs).containsExactlyInAnyOrderEntriesOf(mapOf(
            DeliveryOption.PICKUP to 0.0,
            DeliveryOption.DELIVERY to 4.0,
            DeliveryOption.SHIPPING to 7.0,
            DeliveryOption.ARRANGED to 0.0
        ))
    }
}
