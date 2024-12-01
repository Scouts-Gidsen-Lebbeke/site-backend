package be.sgl.backend.repository

import be.sgl.backend.entity.membership.Membership
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository

@Repository
interface MembershipRepository : JpaRepository<Membership, Int>