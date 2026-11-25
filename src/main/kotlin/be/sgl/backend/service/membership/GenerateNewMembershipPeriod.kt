package be.sgl.backend.service.membership

import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipRestriction
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.shared.mail.MailService
import be.sgl.backend.shared.util.I18nUtil
import be.sgl.backend.shared.util.Usecase
import java.time.temporal.ChronoUnit

@Usecase
class GenerateNewMembershipPeriod(
    private val membershipPeriodRepository: MembershipPeriodRepository,
    private val mailService: MailService
) {

    fun execute(previousPeriod: MembershipPeriod): MembershipPeriod {
        var newPeriod = MembershipPeriod()
        newPeriod.start = previousPeriod.end.plusDays(1)
        newPeriod.end = newPeriod.start.plusDays(ChronoUnit.DAYS.between(previousPeriod.start, previousPeriod.end))
        newPeriod.price = previousPeriod.price
        newPeriod.registrationLimit = previousPeriod.registrationLimit
        newPeriod.reductionFactor = previousPeriod.reductionFactor
        newPeriod.siblingReduction = previousPeriod.siblingReduction
        val shiftInDays = ChronoUnit.DAYS.between(previousPeriod.start, newPeriod.start)
        previousPeriod.restrictions.forEach { previousRestriction ->
            val newRestriction = MembershipRestriction()
            newRestriction.period = newPeriod
            newRestriction.branch = previousRestriction.branch
            newRestriction.alternativeStart = previousRestriction.alternativeStart?.plusDays(shiftInDays)
            newRestriction.alternativePrice = previousRestriction.alternativePrice
            newRestriction.registrationLimit = previousRestriction.registrationLimit
            newPeriod.restrictions.add(newRestriction)
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