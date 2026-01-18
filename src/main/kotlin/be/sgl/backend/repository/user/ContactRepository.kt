package be.sgl.backend.repository.user

import be.sgl.backend.entity.user.Contact
import be.sgl.backend.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContactRepository : JpaRepository<Contact, Int> {
    fun findContactsByUser(user: User): List<Contact>
    fun findContactsByUserAndNisNotNull(user: User): List<Contact>
}