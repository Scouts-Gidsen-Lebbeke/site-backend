package be.sgl.backend.service.activity

import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.user.SiblingRepository
import be.sgl.backend.service.registrable.CalculatePriceFromAdditionalData
import be.sgl.backend.util.reducePrice
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ValidateAndCreateActivityRegistration {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var checkRegistrationStatusForUser: CheckRegistrationStatusForUser
    @Autowired
    private lateinit var calculatePriceFromAdditionalData: CalculatePriceFromAdditionalData
    @Autowired
    private lateinit var siblingRepository: SiblingRepository
    @Autowired
    lateinit var registrationRepository: ActivityRegistrationRepository

    fun execute(restriction: ActivityRestriction, user: User, additionalData: String?): ActivityRegistration {
        val activity = restriction.activity
        val status = checkRegistrationStatusForUser.execute(activity, user)
        check(status.currentRegistration == null) { "Registration for user ${user.username} already exists!" }
        check(status.activeMembership) { "User ${user.username} has no active membership!" }
        check(status.openOptions.any { it.id == restriction.id }) { "The chosen restriction is not valid (anymore) for ${activity.name} and ${user.username}!" }
        // check(status.medicalsUpToDate) { "User ${user.username} has no active medical record!" }
        val finalPrice = calculatePriceForActivity(user, activity, restriction, additionalData)
        logger.info { "Calculated price for this registration is €$finalPrice" }
        return ActivityRegistration(activity, user, restriction, finalPrice, additionalData)
    }

    private fun calculatePriceForActivity(user: User, activity: Activity, restriction: ActivityRestriction, additionalData: String?): Double {
        logger.info { "Calculating price applicable to ${user.username} for activity #${activity.id} (restriction #${restriction.id})" }
        var finalPrice = restriction.alternativePrice ?: activity.price
        logger.info { "Calculated base price is €$finalPrice" }
        val additionalPrice = calculatePriceFromAdditionalData.execute(activity, additionalData)
        logger.info { "Additional cost from extra data is €$additionalPrice" }
        if (user.hasReduction) {
            logger.info { "User is eligible for reduced tariff, dividing base price with reduction factor (${activity.reductionFactor})" }
            return finalPrice.reducePrice(activity.reductionFactor) + additionalPrice
        }
        finalPrice += additionalPrice
        getSiblingsWithoutReductionAndWithRegistration(user, activity)?.let { sibling ->
            logger.info { "${user.username} has already subscribed sibling ${sibling.username}, applying sibling reduction" }
            return (finalPrice - activity.siblingReduction).coerceAtLeast(0.0)
        }
        return finalPrice
    }

    private fun getSiblingsWithoutReductionAndWithRegistration(user: User, activity: Activity): User? {
        return siblingRepository.getByUser(user).map { it.sibling }.firstOrNull {
            !it.hasReduction && registrationRepository.existsBySubscribableAndUser(activity, it)
        }
    }
}