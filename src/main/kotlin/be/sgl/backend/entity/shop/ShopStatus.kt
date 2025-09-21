package be.sgl.backend.entity.shop

enum class ShopStatus {
    NOT_YET_OPEN,
    TAKING_ORDERS,
    ORDERING_CLOSED,
    COMPLETED,
    CANCELLED;

    companion object {
        fun Shop.getStatus(orders: List<ShopOrder>) = when {
            cancelled -> CANCELLED
            isNotYetOpen -> NOT_YET_OPEN
            !isClosed -> TAKING_ORDERS
            orders.all { it.completed } -> COMPLETED
            else -> ORDERING_CLOSED
        }
    }
}