package be.sgl.backend.repository

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.user.User
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ActivityRegistrationRepository : PaymentRepository<ActivityRegistration> {
    fun getByStartBetweenOrderByStart(begin: LocalDateTime, end: LocalDateTime): List<ActivityRegistration>
    fun getByUserAndStartBetweenOrderByStart(user: User, begin: LocalDateTime, end: LocalDateTime): List<ActivityRegistration>
    fun getBySubscribable(subscribable: Activity): List<ActivityRegistration>
    fun existsBySubscribable(subscribable: Activity): Boolean
    fun getByUser(user: User): List<ActivityRegistration>
    fun existsBySubscribableAndUser(subscribable: Activity, user: User): Boolean
    fun getByRestriction(restriction: ActivityRestriction): List<ActivityRegistration>
    @Query("from ActivityRegistration where restriction.branch = :branch")
    fun getByBranch(branch: Branch): List<ActivityRegistration>
    fun getByUserAndSubscribable(user: User, subscribable: Activity): ActivityRegistration?
}