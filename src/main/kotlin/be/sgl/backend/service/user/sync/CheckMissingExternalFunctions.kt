package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.user.User
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired

@ExternalUsecase
class CheckMissingExternalFunctions {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var fetchCurrentlyActiveExternalFunctions: FetchCurrentlyActiveExternalFunctions
    @Autowired
    private lateinit var createExternalFunction: CreateExternalFunction

    fun execute(user: User, assignIfMissing: Boolean): List<String> {
        val externalId = user.externalId ?: return emptyList()
        val externalFunctions = fetchCurrentlyActiveExternalFunctions.execute(externalId)
        logger.info { "Current external functions: $externalFunctions" }
        // check if member roles are applied externally (lookup role based on branch and apply (backup)ExternalIds)
        val functionsToAssign = mutableListOf<String>()
        for (role in user.roles.map { it.role }) {
            val externalRoleId = role.externalId
            if (externalRoleId != null && externalFunctions.none { it.functie == externalRoleId }) {
                logger.info { "External function $externalRoleId should be assigned for role ${role.name} but isn't" }
                functionsToAssign.add(externalRoleId)
            }
            val backupExternalRoleId = role.externalId
            if (backupExternalRoleId != null && externalFunctions.none { it.functie == backupExternalRoleId }) {
                logger.info { "External backup function $externalRoleId should be assigned for role ${role.name} but isn't" }
                functionsToAssign.add(backupExternalRoleId)
            }
            // We currently don't care about external functions corresponding to an internal role
            // They can be assigned on purpose, so it isn't always correct to end them
            // If members don't have any internal functions, they will still be synced in the next step
        }
        if (assignIfMissing) {
            functionsToAssign.forEach {
                logger.info { "Assigning external function $it..." }
                createExternalFunction.execute(externalId, it)
            }
        }
    }
}