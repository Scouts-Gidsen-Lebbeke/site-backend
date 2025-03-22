package be.sgl.backend.service.user

import be.sgl.backend.config.security.BearerTokenFilter
import be.sgl.backend.entity.user.*
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.service.exception.UserNotFoundException
import be.sgl.backend.util.*
import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.apache.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KFunction1

@Service
@ForExternalOrganization
class ExternalUserDataProvider : UserDataProvider() {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var entityManager: EntityManager
    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder
    @Value("\${rest.ga.url}")
    private lateinit var restGAUrl: String
    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String

    override fun acceptRegistration(user: User) {
        logger.debug { "Accepting registration for user ${user.id}..." }
        val address = user.addresses.first()
        createExternalRegistration(LidAanvraag(
            externalOrganizationId,
            null,
            user.firstName,
            user.name,
            user.birthdate,
            Persoonsgegevens(
                user.sex.code,
                user.mobile,
                null
            ),
            user.email,
            Adres(
                null,
                address.country,
                address.zipcode,
                address.town,
                address.street,
                null,
                address.number.toString(),
                address.subPremise,
                null,
                true,
                null,
                "normaal"
            ),
            user.hasReduction
        ))
        logger.debug { "External registration finished: request created and ready to be approved!" }
    }

    override fun findUser(username: String): User? {
        return userRepository.findByUsername(username)?.withExternalData()
    }

    override fun getUser(username: String): User {
        return userRepository.findByUsername(username)?.withExternalData() ?: throw UserNotFoundException(username)
    }

    override fun findByNameAndEmail(name: String, firstName: String, email: String): User? {
        return userRepository.findByNameAndFirstNameAndEmail(name, firstName, email)?.withExternalData()
    }

    override fun updateUser(user: User): User {
        val persistedUser = userRepository.getReferenceById(user.id!!)
        persistedUser.firstName = user.firstName
        persistedUser.name = user.name
        persistedUser.email = user.email
        persistedUser.birthdate = user.birthdate
        persistedUser.image = user.image
        persistedUser.ageDeviation = user.ageDeviation
        persistedUser.sex = user.sex
        userRepository.save(persistedUser)
        // TODO: post rest to GA
        return user
    }

    override fun startRole(user: User, role: Role) {
        if (user.roles.none { it.role == role }) {
            logger.warn { "${user.username} already has the role ${role.name}! Starting aborted." }
            return
        }
        val newRole = UserRole(user, role)
        val externalRole = role.externalId
        if (externalRole == null) {
            logger.error { "Trying to end a non-externally linked role ${role.name}!" }
            return
        }
        val functionList = mutableListOf(
            Functie(externalOrganizationId, externalRole, newRole.startDate.asExternalDate() ?: return, null)
        )
        role.backupExternalId?.let {
            logger.debug { "${user.username} has a back-up external id, also adding this role." }
            functionList.add(
                Functie(externalOrganizationId, it, newRole.startDate.asExternalDate() ?: return, null)
            )
        }
        postExternalMemberData(user.externalId!!, LidFuncties(functionList))
        user.roles.add(newRole)
    }

    override fun endRole(user: User, role: Role) {
        val userRole = user.roles.find { it.role == role }
        if (userRole == null) {
            logger.warn { "${user.username} never had the role ${role.name}! Ending aborted." }
            return
        }
        val externalRole = role.externalId
        if (externalRole == null) {
            logger.error { "Trying to end a non-externally linked role ${role.name}!" }
            return
        }
        userRole.endDate = LocalDate.now()
        val functionList = mutableListOf(
            Functie(externalOrganizationId, externalRole, userRole.startDate.asExternalDate() ?: return, userRole.endDate.asExternalDate())
        )
        role.backupExternalId?.let {
            logger.debug { "${user.username} has a back-up external id, also removing this role." }
            functionList.add(
                Functie(externalOrganizationId, it, userRole.startDate.asExternalDate() ?: return, userRole.endDate.asExternalDate())
            )
        }
        postExternalMemberData(user.externalId!!, LidFuncties(functionList))
        user.roles.remove(userRole)
    }

    private fun User.withExternalData() = apply {
        getExternalData<Lid>(externalId, ::getExternalMemberData)?.let {
            roles.addAll(it.functies.mapNotNull { f -> translateFunction(this, f) })
            sex = Sex.values().firstOrNull { s -> s.code == it.persoonsgegevens.geslacht } ?: Sex.UNKNOWN
            mobile = it.persoonsgegevens.gsm
            hasHandicap = it.vgagegevens.beperking
            hasReduction = it.vgagegevens.verminderdlidgeld
            accountNo = it.persoonsgegevens.rekeningnummer
            birthdate = it.vgagegevens.geboortedatum
            memberId = it.verbondsgegevens.lidnummer
            addresses.addAll(it.adressen.map { a -> a.asAddress() } )
            contacts.addAll(it.contacten.map { c ->
                val contact = Contact()
                contact.name = c.achternaam
                contact.firstName = c.voornaam
                contact.role = when(c.rol) {
                    "vader" -> ContactRole.FATHER
                    "moeder" -> ContactRole.MOTHER
                    "voogd" -> ContactRole.GUARDIAN
                    else -> ContactRole.RESPONSIBLE
                }
                contact.address = addresses.firstOrNull { a -> a.externalId == c.id }
                contact.mobile = c.gsm
                contact.email = c.email
                contact
            })
        }
        entityManager.detach(this)
    }

    private fun <T> getExternalData(externalId: String?, call: KFunction1<String, Mono<T>>): T? {
        if (externalId == null) {
            logger.error { "External data call for null id!" }
            return null
        }
        return call(externalId).block()
    }

    private fun translateFunction(user: User, function: Functie): UserRole? {
        if (function.groep != externalOrganizationId) return null
        val role = roleRepository.getRoleByExternalIdEquals(function.functie) ?: return null
        val endDate = function.einde?.let { LocalDate.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
        if (endDate != null && endDate < LocalDate.now()) return null
        return UserRole(user, role, LocalDate.parse(function.begin, DateTimeFormatter.ISO_OFFSET_DATE_TIME), endDate)
    }

    private fun authorizedCall() = webClientBuilder
        .baseUrl(restGAUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${BearerTokenFilter.getToken()}")
        .build()

    private fun getExternalMemberData(externalId: String) = authorizedCall().get()
        .uri { it.path("/lid/{id}").build(externalId) }
        .retrieve()
        .bodyToMono(Lid::class.java)
        .cache(Duration.ofSeconds(5))

    private fun postExternalMemberData(externalId: String, data: Any) = authorizedCall().post()
        .uri { it.path("/lid/{id}").build(externalId) }
        .bodyValue(data)
        .retrieve()

    private fun createExternalRegistration(registration: LidAanvraag) = webClientBuilder
        .baseUrl(restGAUrl)
        .build()
        .post()
        .uri("/lid/aanvraag")
        .bodyValue(registration)
        .retrieve()
}