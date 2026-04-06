package be.sgl.backend.service.belcotax

import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.util.Usecase

@Usecase
class IsRelevantActivityRegistration {

    fun forYear(year: Int, registration: ActivityRegistration):  Boolean {
        val user = registration.user
        val ageAtStartOfActivity = user.getAge(registration.start.toLocalDate())
        val relevantAgeThresholdForUser = if (user.hasHandicap) 21 else 14
        return ageAtStartOfActivity < relevantAgeThresholdForUser
    }
}