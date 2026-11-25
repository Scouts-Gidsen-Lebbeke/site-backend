package be.sgl.backend.people.domain

import be.sgl.backend.people.domain.User
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface UserRepository : JpaRepository<User, Int> {
    fun findByUsername(username: String): User?
    fun getByUsername(username: String): User
    fun findByNameAndFirstNameAndEmail(name: String, firstName: String, email: String): User?
    @Query("from User where name like concat('%', :query, '%') or firstName like concat('%', :query, '%')")
    fun findByQuery(query: String): List<User>
    @Modifying
    @Transactional
    @Query("update User set username = :username where id = :id and username is null")
    fun updateUsername(id: Int, username: String)
    fun findByNameAndFirstNameAndBirthdate(name: String, firstName: String, birthdate: LocalDate): User?
}