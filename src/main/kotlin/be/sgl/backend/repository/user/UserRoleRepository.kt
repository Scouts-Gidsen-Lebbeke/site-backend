package be.sgl.backend.repository.user

import be.sgl.backend.entity.user.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRoleRepository : JpaRepository<UserRole, Long>