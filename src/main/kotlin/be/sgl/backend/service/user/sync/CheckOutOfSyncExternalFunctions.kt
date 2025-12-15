package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.user.User
import mu.KotlinLogging

@ExternalUsecase
class CheckOutOfSyncExternalFunctions(
    private val fetchCurrentlyActiveExternalFunctions: FetchCurrentlyActiveExternalFunctions,
    private val createExternalFunctions: CreateExternalFunctions,
    private val endExternalFunctions: EndExternalFunctions
) {

    private val logger = KotlinLogging.logger {}

    fun execute(user: User, correct: Boolean): OutOfSyncState {
        val externalId = user.externalId ?: return OutOfSyncState()
        val uncheckedActiveExternalFunctions = fetchCurrentlyActiveExternalFunctions.execute(externalId)
            .map { it.functie }.toMutableList()
        logger.info { "Current external functions: $uncheckedActiveExternalFunctions" }
        val functionsToAssign = mutableListOf<String>()
        for (role in user.roles.map { it.role }) {
            if (role.adminRole) {
                if (!uncheckedActiveExternalFunctions.remove(role.externalId) &&
                    !uncheckedActiveExternalFunctions.remove(role.backupExternalId)) {
                    functionsToAssign.add(role.backupExternalId!!)
                }
                continue
            }
            val externalRoleId = role.externalId
            if (externalRoleId != null && !uncheckedActiveExternalFunctions.contains(externalRoleId)) {
                logger.info { "External function $externalRoleId should be assigned for role ${role.name} but isn't" }
                functionsToAssign.add(externalRoleId)
            }
            uncheckedActiveExternalFunctions.remove(externalRoleId)
            val backupExternalRoleId = role.backupExternalId
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
        return OutOfSyncState(functionsToAssign, uncheckedActiveExternalFunctions)
    }

    data class OutOfSyncState(val functionsToAssign: List<String>, val functionsToDeassign: List<String>) {

        constructor() : this(emptyList(), emptyList())

        fun isOutOfSync(): Boolean {
            return functionsToAssign.isNotEmpty() || functionsToDeassign.isNotEmpty()
        }
    }
}