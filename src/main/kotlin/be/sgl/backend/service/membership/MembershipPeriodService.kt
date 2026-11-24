package be.sgl.backend.service.membership

import be.sgl.backend.dto.membership.CreateOrUpdateMembershipPeriodRequest
import be.sgl.backend.dto.membership.MembershipPeriodDTO
import be.sgl.backend.dto.membership.MembershipPeriodResult
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipRestriction
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.exception.MembershipPeriodNotFoundException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MembershipPeriodService(
    private val periodRepository: MembershipPeriodRepository,
    private val membershipRepository: MembershipRepository,
) {

    private val logger = KotlinLogging.logger {}

    fun getAllMembershipPeriods(): List<MembershipPeriodResult> {
        logger.debug { "Fetching all activities" }
        return periodRepository.findAllRecentFirst()
            .map { MembershipPeriodResult(it, membershipRepository.getPaidByPeriod(it)) }
    }

    fun getMembershipPeriodDTOById(id: Int): MembershipPeriodDTO {
        return MembershipPeriodDTO.of(getPeriodById(id))
    }

    fun getCurrentMembershipPeriod(): MembershipPeriodDTO {
        return MembershipPeriodDTO.of(periodRepository.getActivePeriod())
    }

    fun createPeriod(request: CreateOrUpdateMembershipPeriodRequest): MembershipPeriodDTO {
        val newPeriod = request.toEntity()
        linkRestrictions(newPeriod)
        validateRestrictions(newPeriod.restrictions)
        return MembershipPeriodDTO.of(periodRepository.save(newPeriod))
    }

    fun updatePeriod(id: Int, request: CreateOrUpdateMembershipPeriodRequest): MembershipPeriodDTO {
        val periodToUpdate = getPeriodById(id)
        val periodFromDto = request.toEntity()
        if (periodToUpdate.start.isAfter(LocalDate.now())) {
            linkRestrictions(periodFromDto)
            validateRestrictions(periodFromDto.restrictions)
            periodToUpdate.start = periodFromDto.start
            periodToUpdate.end = periodFromDto.end
            periodToUpdate.price = periodFromDto.price
            periodToUpdate.reductionFactor = periodFromDto.reductionFactor
            periodToUpdate.siblingReduction = periodFromDto.siblingReduction
            periodToUpdate.restrictions.clear()
            periodToUpdate.restrictions.addAll(periodFromDto.restrictions)
            linkRestrictions(periodToUpdate)
        }
        periodToUpdate.registrationLimit = periodFromDto.registrationLimit
        return MembershipPeriodDTO.of(periodRepository.save(periodToUpdate))
    }

    private fun getPeriodById(id: Int): MembershipPeriod {
        return periodRepository.findById(id).orElseThrow { MembershipPeriodNotFoundException() }
    }

    private fun linkRestrictions(period: MembershipPeriod) {
        period.restrictions.forEach { it.period = period }
    }

    fun validateRestrictions(restrictions: List<MembershipRestriction>) {
        // TODO: validate overlaps and futureness
        restrictions.onEach { it.validate() }.filter { it.branch != null }.groupBy { it.branch?.id ?: it.branch }.forEach { (_, restrictions) ->
            check(restrictions.filterNot { it.isTimeRestriction }.size <= 1) { "A branch should at most have one single non-time related restriction!" }
        }
    }
}