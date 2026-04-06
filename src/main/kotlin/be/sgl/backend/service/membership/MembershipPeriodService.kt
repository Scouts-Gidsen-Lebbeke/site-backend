package be.sgl.backend.service.membership

import be.sgl.backend.dto.membership.CreateOrUpdateMembershipPeriodRequest
import be.sgl.backend.dto.membership.MembershipPeriodDTO
import be.sgl.backend.dto.membership.MembershipPeriodResult
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipRestriction
import be.sgl.backend.mapper.membership.MembershipPeriodMapper
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
    private val mapper: MembershipPeriodMapper
) {

    private val logger = KotlinLogging.logger {}

    fun getAllMembershipPeriods(): List<MembershipPeriodResult> {
        logger.debug { "Fetching all activities" }
        return periodRepository.findAllRecentFirst()
            .map { MembershipPeriodResult(it, membershipRepository.getPaidByPeriod(it)) }
    }

    fun getMembershipPeriodDTOById(id: Int): MembershipPeriodDTO {
        return mapper.toDto(getPeriodById(id))
    }

    fun getCurrentMembershipPeriod(): MembershipPeriodDTO {
        return mapper.toDto(periodRepository.getActivePeriod())
    }

    fun createPeriod(request: CreateOrUpdateMembershipPeriodRequest): MembershipPeriodDTO {
        val newPeriod = mapper.toEntity(request)
        validateRestrictions(newPeriod.restrictions)
        for (restriction in newPeriod.restrictions) {
            restriction.period = newPeriod
        }
        return mapper.toDto(periodRepository.save(newPeriod))
    }

    fun updatePeriod(id: Int, request: CreateOrUpdateMembershipPeriodRequest): MembershipPeriodDTO {
        val periodToUpdate = getPeriodById(id)
        val periodFromDto = mapper.toEntity(request)
        if (periodToUpdate.start.isAfter(LocalDate.now())) {
            validateRestrictions(periodFromDto.restrictions)
            periodToUpdate.start = periodFromDto.start
            periodToUpdate.end = periodFromDto.end
            periodToUpdate.price = periodFromDto.price
            periodToUpdate.reductionFactor = periodFromDto.reductionFactor
            periodToUpdate.siblingReduction = periodFromDto.siblingReduction
        }
        periodToUpdate.registrationLimit = periodFromDto.registrationLimit
        return mapper.toDto(periodRepository.save(periodToUpdate))
    }

    private fun getPeriodById(id: Int): MembershipPeriod {
        return periodRepository.findById(id).orElseThrow { MembershipPeriodNotFoundException() }
    }

    fun validateRestrictions(restrictions: List<MembershipRestriction>) {
        // TODO: validate overlaps and futureness
        restrictions.onEach { it.validate() }.filter { it.branch != null }.groupBy { it.branch }.forEach { (_, restrictions) ->
            check(restrictions.filterNot { it.isTimeRestriction }.size <= 1) { "A branch should at most have one single non-time related restriction!" }
        }
    }
}