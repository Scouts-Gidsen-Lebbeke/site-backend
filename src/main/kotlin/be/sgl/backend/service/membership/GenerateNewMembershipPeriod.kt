package be.sgl.backend.service.membership

import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipRestriction
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.util.I18nUtil
import be.sgl.backend.util.Usecase
import java.time.Duration

@Usecase
class GenerateNewMembershipPeriod(
    private val membershipPeriodRepository: MembershipPeriodRepository,
    private val mailService: MailService
) {

    fun execute(previousPeriod: MembershipPeriod): MembershipPeriod {
        var newPeriod = MembershipPeriod()
        newPeriod.start = previousPeriod.end.plusDays(1)
        newPeriod.end = newPeriod.start.plusDays(Duration.between(previousPeriod.start, newPeriod.start).toDays())
        newPeriod.price = previousPeriod.price
        newPeriod.registrationLimit = previousPeriod.registrationLimit
        newPeriod.reductionFactor = previousPeriod.reductionFactor
        newPeriod.siblingReduction = previousPeriod.siblingReduction
        previousPeriod.restrictions.forEach { previousRestriction ->
            val newRestriction = MembershipRestriction()
            newRestriction.period = newPeriod
            newRestriction.branch = previousRestriction.branch
            newRestriction.alternativeStart = previousRestriction.alternativeStart
            newRestriction.alternativePrice = previousRestriction.alternativePrice
            newRestriction.registrationLimit = previousRestriction.registrationLimit
        }
        newPeriod = membershipPeriodRepository.save(newPeriod)
        notifyOrganizationAboutNewPeriod(newPeriod)
        return newPeriod
    }

    fun notifyOrganizationAboutNewPeriod(newPeriod: MembershipPeriod) {
        val params = mapOf(
            "periodName" to newPeriod.toString()
        )
        mailService.builder()
            .toOrganization()
            .subject(I18nUtil.i18n("new.membership.period.created"))
            .template("new-membership-period-created.html", params)
            .send()

    }
}