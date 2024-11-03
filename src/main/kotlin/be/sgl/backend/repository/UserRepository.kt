package be.sgl.backend.repository

import be.sgl.backend.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun getUserByExternalIdEquals(id: String): User
}