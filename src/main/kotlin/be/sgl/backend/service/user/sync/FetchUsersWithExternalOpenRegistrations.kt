package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.user.User
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.repository.user.UserRepository
import mu.KotlinLogging

@ExternalUsecase
class FetchUsersWithExternalOpenRegistrations(
    private val userRepository: UserRepository,
    private val lidaanvragenApi: LidaanvragenApi
) {

    private val logger = KotlinLogging.logger {}

    fun execute(): Map<User, String> {
        logger.info { "Fetching users with external open registrations..." }
        val externalRegistrations = lidaanvragenApi.aanvragen.aanvragen
        logger.info { "Found ${externalRegistrations.size} external open registrations..." }
        return externalRegistrations.mapNotNull {
            val user = userRepository.findByNameAndFirstNameAndEmail(it.achternaam, it.voornaam, it.email) ?: return@mapNotNull null
            user to it.id
        }.associate { it }
    }
}