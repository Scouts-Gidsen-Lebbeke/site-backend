package be.sgl.backend.repository

import be.sgl.backend.entity.ActivityRegistration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ActivityRegistrationRepository : JpaRepository<ActivityRegistration, Int> {
    fun getByUserIdAndStartBeforeOrderByStart(userId: Int, start: LocalDateTime): List<ActivityRegistration>
}