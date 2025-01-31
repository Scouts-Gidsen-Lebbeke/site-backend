package be.sgl.backend.repository

import be.sgl.backend.entity.user.UserRegistration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRegistrationRepository : JpaRepository<UserRegistration, Int>