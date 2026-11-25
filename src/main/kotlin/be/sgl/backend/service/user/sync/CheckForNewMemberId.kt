package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.user.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.mail.MailService
import mu.KotlinLogging

@ExternalUsecase
class CheckForNewMemberId(
    private val ledenApi: LedenApi,
    private val userRepository: UserRepository,
    private val mailService: MailService
) {

    private val logger = KotlinLogging.logger {}

    fun execute(user: User, saveAndNotifyUser: Boolean): Boolean {
        logger.info { "Checking if ${user.username} has a new external member id ..." }
        val externalId = user.externalId ?: run {
            logger.info { "User is not linked to Groepsadmin" }
            return false
        }
        val newMemberId = ledenApi.getLid(externalId).verbondsgegevens.lidnummer
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