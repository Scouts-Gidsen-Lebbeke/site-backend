package be.sgl.backend.people.domain

import be.sgl.backend.people.domain.Contact
import be.sgl.backend.people.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContactRepository : JpaRepository<Contact, Int> {
    fun findContactsByUser(user: User): List<Contact>
    fun findContactsByUserAndNisNotNull(user: User): List<Contact>
}