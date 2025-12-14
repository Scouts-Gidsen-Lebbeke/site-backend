package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationForm
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.user.User
import be.sgl.backend.service.SettingService
import be.sgl.backend.util.Usecase
import mu.KotlinLogging

@Usecase
class FilterIntoValidFormData(
    private val settingService: SettingService
) {
    private val logger = KotlinLogging.logger {}

    fun execute(user: User, registrations: List<ActivityRegistration>): List<DeclarationForm> {
        val forms = mutableListOf<DeclarationForm>()
        if (user.nis == null) {
            logger.debug { "User has no nis configured, no forms can be made." }
            return forms
        }
        val address = user.addresses.firstOrNull { it.postalAdress }
        if (address == null) {
            logger.debug { "User has no linked postal address, no forms can be made." }
            return forms
        }
        val parent = user.contacts.firstOrNull { it.nis != null }
        if (parent == null) {
            logger.debug { "User has no linked parent with a configured nis, no forms can be made." }
            return forms
        }
        val yearOfActivities = registrations.first().start.year
        val rate = getDispatchRateForYear(yearOfActivities)
        for ((index, chunk) in registrations.chunked(4).withIndex()) {
            val firstRegistration = chunk.first()
            val secondRegistration = chunk.getOrNull(1)
            val thirdRegistration = chunk.getOrNull(2)
            val fourthRegistration = chunk.getOrNull(3)
            val validForm = DeclarationForm(user, address, parent, firstRegistration, secondRegistration,
                thirdRegistration, fourthRegistration, rate, index)
            forms.add(validForm)
        }
        return forms
    }

    private fun getDispatchRateForYear(year: Int): Double {
        return settingService.getOrDefault("DISPATCH_RATE_$year", DEFAULT_DISPATCH_RATE)
    }

    companion object {
        private const val DEFAULT_DISPATCH_RATE = 14.4
    }
}