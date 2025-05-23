package be.sgl.backend.service.membership

import be.sgl.backend.dto.MembershipPeriodDTO
import be.sgl.backend.dto.MembershipPeriodResultDTO
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.mapper.MembershipPeriodMapper
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.service.exception.MembershipPeriodNotFoundException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MembershipPeriodService {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var periodRepository: MembershipPeriodRepository
    @Autowired
    private lateinit var membershipRepository: MembershipRepository
    @Autowired
    private lateinit var mapper: MembershipPeriodMapper

    fun getAllMembershipPeriods(): List<MembershipPeriodResultDTO> {
        logger.debug { "Fetching all activities" }
        return periodRepository.findAllRecentFirst()
            .map { MembershipPeriodResultDTO(it, membershipRepository.getPaidByPeriod(it)) }
    }

    fun getMembershipPeriodDTOById(id: Int): MembershipPeriodDTO {
        return mapper.toDto(getPeriodById(id))
    }

    fun getCurrentMembershipPeriod(): MembershipPeriodDTO {
        return mapper.toDto(periodRepository.getActivePeriod())
    }

    fun saveMembershipPeriodDTO(dto: MembershipPeriodDTO): MembershipPeriodDTO {
        // TODO: validate overlaps and futureness
        val newPeriod = mapper.toEntity(dto)
        newPeriod.validateRestrictions()
        for (restriction in newPeriod.restrictions) {
            restriction.period = newPeriod
        }
        return mapper.toDto(periodRepository.save(newPeriod))
    }

    fun mergeMembershipPeriodDTOChanges(id: Int, dto: MembershipPeriodDTO): MembershipPeriodDTO {
        val period = getPeriodById(id)
        // TODO
        return mapper.toDto(periodRepository.save(period))
    }

    private fun getPeriodById(id: Int): MembershipPeriod {
        return periodRepository.findById(id).orElseThrow { MembershipPeriodNotFoundException() }
    }
}