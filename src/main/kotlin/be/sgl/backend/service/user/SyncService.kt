package be.sgl.backend.service.user

import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LedenlijstApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.*
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.util.ForExternalOrganization
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.OffsetDateTime

@Component
@ForExternalOrganization
class SyncService {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var membershipRepository: MembershipRepository
    @Autowired
    private lateinit var ledenApi: LedenApi
    @Autowired
    private lateinit var lidaanvragenApi: LidaanvragenApi
    @Autowired
    private lateinit var ledenlijstApi: LedenlijstApi
    @Autowired
    protected lateinit var mailService: MailService
    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String

    fun syncMembers(sseEmitter: SseEmitter) {
        val externalUsers = getExternalMemberIds()
        val openRegistrations = getOpenRegistrations()
        membershipRepository.getCurrent().forEach { membership ->
            val user = membership.user
            sseEmitter.send("Checking user #${user.id} for active membership #${membership.id}...")
            if (user.username == null) {
                sseEmitter.send("User has no username, looking up external membership request...")
                openRegistrations[user]?.let { requestId ->
                    sseEmitter.send("User has external membership request $requestId, trying to accept...")
                    val newRole = membership.user.roles.firstOrNull { it.role.forExternalSync }?.role
                    if (newRole == null) {
                        // This should only happen for staff branch memberships, which shouldn't be common for new members
                        logger.info { "Internal membership didn't assign role, manual intervention is needed." }
                         // TODO: escalate this
                        return@forEach
                    }
                    sseEmitter.send("Internal membership assigned role ${newRole.name}, passing it for external creation...")
                    user.externalId = createExternalMemberForRequestId(requestId, newRole)
                    userRepository.save(user)
                    lidaanvragenApi.deleteAanvraag(requestId, "ja", false)
                    sseEmitter.send("External membership request accepted.")
                    return@forEach
                }
                sseEmitter.send("No external membership request found, checking linked external user upon member id linkage...")
                checkNotNull(user.externalId) { "Member with an active membership but without a username should only exist for users with open registrations!" }
                checkNotNull(externalUsers.remove(user.externalId)) { "Member with an external id (${user.externalId}) but no username should still be found externally!"}
                ledenApi.getLid(user.externalId).verbondsgegevens.lidnummer?.let {
                    // TODO: avoid this mail is sent multiple times, perhaps with externalMember.aangepast?
                    val params = mapOf(
                        "member" to user.firstName,
                        "memberId" to it
                    )
                    mailService.builder()
                        .to(user.email)
                        .subject("Bevestiging aanmaak lidnummer")
                        .template("member-id-confirmation.html", params)
                        .send()
                    sseEmitter.send("User has memberId $it, sent mail for account creation.")
                    return@forEach
                }
                sseEmitter.send("User has no member id yet, no further checks needed.")
                return@forEach
            }
            sseEmitter.send("User has a username, checking external functions...")
            checkNotNull(externalUsers.remove(user.externalId)) { "Member with a username (${user.username}) should always be found externally!" }
            val externalFunctions = getCurrentlyActiveExternalFunctionIds(user.externalId!!)
            sseEmitter.send("Current external functions: $externalFunctions")
            // check if member roles are applied externally (lookup role based on branch and apply (backup)ExternalIds)
            for (userRole in user.roles) {
                userRole.role.externalId?.let {
                    if (it !in externalFunctions) {
                        sseEmitter.send("External function $it should be assigned for role ${userRole.role.name} but isn't, assigning it...")
                        createExternalFunction(user, it)
                    }
                }
                userRole.role.backupExternalId?.let {
                    if (it !in externalFunctions) {
                        sseEmitter.send("External backup function $it should be assigned for role ${userRole.role.name} but isn't, assigning it...")
                        createExternalFunction(user, it)
                    }
                }
                // We currently don't care about external functions corresponding to an internal role
                // They can be assigned on purpose, so it isn't always correct to end them
                // If members don't have any internal functions, they will still be synced in the next step
            }
        }
        for ((externalId, _) in externalUsers) {
            sseEmitter.send("External user #$externalId has no membership, removing all external roles...")
            val lidPatch = Lid().apply {
                functies = mutableListOf()
            }
            getCurrentlyActiveExternalFunctions(externalId).forEach {
                it.einde = OffsetDateTime.now()
                lidPatch.functies.add(it)
            }
            ledenApi.patchLid(externalId, true, lidPatch)
        }
    }

    private fun getExternalMemberIds(): MutableMap<String, LijstLid> {
        val filter = Filter().apply {
            naam = "Members $externalOrganizationId"
            type = Filter.TypeEnum.GROEP
            groepen = listOf(externalOrganizationId)
            kolommen = listOf( // at least one is required
                "be.vvksm.groepsadmin.model.column.VoornaamColumn",
//                "be.vvksm.groepsadmin.model.column.AchternaamColumn",
//                "be.vvksm.groepsadmin.model.column.VVKSMFunktiesColumn",
//                "be.vvksm.groepsadmin.model.column.GeboorteDatumColumn",
//                "be.vvksm.groepsadmin.model.column.VVKSMTakkenColumn",
//                "be.vvksm.groepsadmin.model.column.VVKSMLeeftijdsTakkenColumn"
            )
            criteria = Criteria().apply {
                functies = listOf() // TODO
                groepen = listOf(externalOrganizationId)
            }
            delen = false
        }
        var filterResult = ledenlijstApi.postFilterNu(filter, "0")
        val memberIds = filterResult.leden.associateBy { it.id }.toMutableMap()
        var offset = filterResult.aantal
        while (offset < filterResult.totaal) {
            filterResult = ledenlijstApi.postFilterNu(filter, "$offset")
            memberIds += filterResult.leden.associateBy { it.id }
            offset += filterResult.aantal
        }
        return memberIds
    }

    private fun getOpenRegistrations(): Map<User, String> {
        return lidaanvragenApi.aanvragen.aanvragen.mapNotNull {
            val user = userRepository.findByNameAndFirstNameAndEmail(it.achternaam, it.voornaam, it.email) ?: return@mapNotNull null
            user to it.id
        }.associate { it }
    }

    private fun createExternalMemberForRequestId(requestId: String, newRole: Role): String {
        val request = lidaanvragenApi.getAanvraag(requestId)
        val newLid = Lid().apply {
            persoonsgegevens = request.persoonsgegevens
            vgagegevens = VgaGegevens().apply {
                voornaam = request.voornaam
                achternaam = request.achternaam
                geboortedatum = request.geboortedatum
                beperking = false // this should be done better
                verminderdlidgeld = request.verminderdlidgeld
            }
            adressen = request.adressen.toMutableList()
            functies = listOf(
                FunctieInstantie().apply {
                    groep = externalOrganizationId
                    functie = newRole.externalId
                    begin = OffsetDateTime.now()
                }
            )
            email = request.email
        }
        return ledenApi.postLid(false, newLid, null).id
    }

    private fun getCurrentlyActiveExternalFunctions(externalId: String): MutableList<FunctieInstantie> {
        return ledenApi.getLid(externalId).functies
            .filter { it.groep == externalOrganizationId && it.einde == null }
            .toMutableList()
    }

    private fun getCurrentlyActiveExternalFunctionIds(externalId: String): MutableList<String> {
        return getCurrentlyActiveExternalFunctions(externalId).map { it.functie }.toMutableList()
    }

    private fun createExternalFunction(user: User, functionId: String) {
        val externalFunction = FunctieInstantie().apply {
            groep = externalOrganizationId
            functie = functionId
            begin = OffsetDateTime.now()
        }
        val lidPatch = Lid().apply {
            functies = mutableListOf(externalFunction)
        }
        ledenApi.patchLid(user.externalId, true, lidPatch)
    }

    private fun endExternalFunction(externalId: String, externalFunction: FunctieInstantie) {
        externalFunction.einde = OffsetDateTime.now()
        val lidPatch = Lid().apply {
            functies = mutableListOf(externalFunction)
        }
        ledenApi.patchLid(externalId, true, lidPatch)
    }
}