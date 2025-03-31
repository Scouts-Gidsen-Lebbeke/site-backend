package be.sgl.backend.service.membership

import be.sgl.backend.dto.MembershipPeriodDTO
import be.sgl.backend.mapper.MembershipPeriodMapper
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MembershipPeriodService {

    @Autowired
    private lateinit var membershipPeriodRepository: MembershipPeriodRepository
    @Autowired
    private lateinit var mapper: MembershipPeriodMapper

    fun getCurrentMembershipPeriod(): MembershipPeriodDTO {
        return mapper.toDto(membershipPeriodRepository.getActivePeriod())
    }
}