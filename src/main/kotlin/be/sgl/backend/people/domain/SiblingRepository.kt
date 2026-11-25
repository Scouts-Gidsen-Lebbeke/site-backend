package be.sgl.backend.people.domain

import be.sgl.backend.people.domain.SiblingRelation
import be.sgl.backend.people.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SiblingRepository : JpaRepository<SiblingRelation, Int> {
    fun getByUser(user: User): List<SiblingRelation>
}