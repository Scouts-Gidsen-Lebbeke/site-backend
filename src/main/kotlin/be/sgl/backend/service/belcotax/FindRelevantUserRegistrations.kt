package be.sgl.backend.service.belcotax

import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.util.Usecase
import mu.KotlinLogging
import java.time.LocalDateTime

@Usecase
class FindRelevantUserRegistrations(
    private val registrationRepository: ActivityRegistrationRepository,
    private val isRelevantActivityRegistration: IsRelevantActivityRegistration
) {
    private val logger = KotlinLogging.logger {}

    fun forPreviousYear(user: User): List<ActivityRegistration> {
        val previousYear = LocalDateTime.now().year - 1
        return forYear(previousYear, user)
    }

    fun forYear(year: Int, user: User): List<ActivityRegistration> {
        val (beginOfYear, endOfYear) = getYearPeriod(year)
        val filteredRegistrations = registrationRepository.getPaidRegistrationsForUserBetween(user, beginOfYear, endOfYear)
            .filter { registration -> isRelevantActivityRegistration.forYear(year, registration) }
        logger.info { "Found ${filteredRegistrations.size} relevant registrations between $beginOfYear and $endOfYear." }
        return filteredRegistrations
    }

    private fun getYearPeriod(year: Int): Pair<LocalDateTime, LocalDateTime> {
        val beginOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0, 0)
        val endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999999999)
        return beginOfYear to endOfYear
    }
}