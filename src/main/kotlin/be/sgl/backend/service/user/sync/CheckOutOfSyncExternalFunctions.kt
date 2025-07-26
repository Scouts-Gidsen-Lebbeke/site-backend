package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.user.User
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired

@ExternalUsecase
class CheckOutOfSyncExternalFunctions {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var fetchCurrentlyActiveExternalFunctions: FetchCurrentlyActiveExternalFunctions
    @Autowired
    private lateinit var createExternalFunctions: CreateExternalFunctions
    @Autowired
    private lateinit var endExternalFunctions: EndExternalFunctions

    fun execute(user: User, correct: Boolean): Pair<Int, Int> {
        val externalId = user.externalId ?: return 0 to 0
        val uncheckedActiveExternalFunctions = fetchCurrentlyActiveExternalFunctions.execute(externalId)
            .map { it.functie }.toMutableList()
        logger.info { "Current external functions: $uncheckedActiveExternalFunctions" }
        val functionsToAssign = mutableListOf<String>()
        for (role in user.roles.map { it.role }) {
            val externalRoleId = role.externalId
            if (externalRoleId != null && !uncheckedActiveExternalFunctions.contains(externalRoleId)) {
                logger.info { "External function $externalRoleId should be assigned for role ${role.name} but isn't" }
                functionsToAssign.add(externalRoleId)
            }
            uncheckedActiveExternalFunctions.remove(externalRoleId)
            val backupExternalRoleId = role.externalId
            if (backupExternalRoleId != null && !uncheckedActiveExternalFunctions.contains(backupExternalRoleId)) {
                logger.info { "External backup function $externalRoleId should be assigned for role ${role.name} but isn't" }
                functionsToAssign.add(backupExternalRoleId)
            }
            uncheckedActiveExternalFunctions.remove(backupExternalRoleId)
        }
        if (correct && functionsToAssign.isNotEmpty()) {
            logger.info { "Assigning missing external functions $functionsToAssign..." }
            createExternalFunctions.execute(externalId, *functionsToAssign.toTypedArray())
        }
        if (correct && uncheckedActiveExternalFunctions.isNotEmpty()) {
            logger.info { "Deassigning incorrect external functions $uncheckedActiveExternalFunctions..." }
            endExternalFunctions.execute(externalId, *uncheckedActiveExternalFunctions.toTypedArray())
        }
        return functionsToAssign.size to uncheckedActiveExternalFunctions.size
    }
}