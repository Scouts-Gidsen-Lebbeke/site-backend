package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.registrable.Registrable
import be.sgl.backend.entity.user.User
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany

/**
 * An [Activity] is a physical event for which only members linked to a configured [ActivityRestriction] can apply.
 */
@Entity
class Activity : Registrable() {

    /**
     * Reduction factor applied on the final price (excluding additional costs) when a member is entitled to reduction.
     */
    var reductionFactor: Double = 3.0

    /**
     * Reduction amount applied on the final price when a member has siblings that are already registered.
     * This reduction has no effect on members that are entitled to reduction.
     */
    var siblingReduction: Double = 0.0
    @OneToMany
    var restrictions = mutableListOf<ActivityRestriction>()

    fun calculatePrice(user: User, restriction: ActivityRestriction, additionalData: String): Double {
        var finalPrice = restriction.alternativePrice ?: price
        val additionalPrice = readAdditionalData(additionalData)
        if (user.userData.hasReduction) {
            return finalPrice / reductionFactor + additionalPrice
        }
        finalPrice += additionalPrice
        if (user.siblings.any { it.isSubscribed() && !it.userData.hasReduction }) {
            return (finalPrice - siblingReduction).coerceAtLeast(0.0)
        }
        return finalPrice
    }
}