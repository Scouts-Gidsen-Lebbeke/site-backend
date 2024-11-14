package be.sgl.backend.repository

import be.sgl.backend.entity.Membership
import be.sgl.backend.entity.NewsItem
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository

@Repository
interface MembershipRepository : JpaRepository<Membership, Int>