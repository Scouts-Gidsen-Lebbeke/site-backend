package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.user.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.service.exception.UserNotFoundException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired

@ExternalUsecase
class CheckForNewMemberId {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var ledenApi: LedenApi
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    protected lateinit var mailService: MailService

    fun execute(user: User, saveAndNotifyUser: Boolean): Boolean {
        logger.info { "Checking if ${user.username} has a new external member id ..." }
        val newMemberId = ledenApi.getLid(user.externalId).verbondsgegevens.lidnummer
        if (newMemberId == null) {
            logger.info { "User has no external member id yet" }
            return false
        }
        if (user.memberId == newMemberId) {
            logger.info { "User is already linked to the same external member id...." }
            return false
        }
        if (!saveAndNotifyUser) {
            logger.info { "User has a new member id $newMemberId, but checked with save flag off" }
            return true
        }
        logger.info { "Saving externally generated member id $newMemberId, linking it internally..." }
        user.memberId = newMemberId
        userRepository.save(user)
        val params = mapOf(
            "member" to user.firstName,
            "memberId" to newMemberId
        )
        mailService.builder()
            .to(user.email)
            .subject("Bevestiging aanmaak lidnummer")
            .template("member-id-confirmation.html", params)
            .send()
        logger.info { "Sent mail for account creation to user" }
        return true
    }
}