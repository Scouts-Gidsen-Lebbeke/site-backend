package be.sgl.backend.service

import be.sgl.backend.repository.MembershipRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MembershipService {
    @Autowired
    private lateinit var membershipRepository: MembershipRepository
}