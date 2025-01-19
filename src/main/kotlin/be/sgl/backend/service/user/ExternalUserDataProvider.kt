package be.sgl.backend.service.user

import be.sgl.backend.config.security.BearerTokenFilter
import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.*
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.UserRepository
import be.sgl.backend.util.Functie
import be.sgl.backend.util.Lid
import be.sgl.backend.util.asAddress
import org.apache.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

@Service
@Conditional(ExternalOrganizationCondition::class)
class ExternalUserDataProvider : UserDataProvider {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder
    @Value("\${rest.ga.url}")
    private lateinit var restGAUrl: String
    @Value("\${external.organization.id}")
    private lateinit var externalOrganizationId: String

    private val cache = ConcurrentHashMap<String, Mono<Lid>>()

    override fun createRegistration(registration: UserRegistration): User {
        TODO("Not yet implemented")
    }

    override fun acceptRegistration(registration: UserRegistration): User {
        TODO("Not yet implemented")
    }

    override fun getUser(username: String): User {
        val user = userRepository.getUserByUsernameEquals(username)
        getExternalData(user.externalId)?.let {
            user.roles.addAll(it.functies.mapNotNull { f -> translateFunction(user, f) })
        }
        return user
    }

    private fun translateFunction(user: User, function: Functie): UserRole? {
        if (function.groep != externalOrganizationId) return null
        val role = roleRepository.getRoleByExternalIdEquals(function.functie) ?: return null
        val endDate = function.einde?.let { LocalDate.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
        return UserRole(user, role, LocalDate.parse(function.begin, DateTimeFormatter.ISO_OFFSET_DATE_TIME), endDate)
    }

    override fun getUserWithAllData(username: String): User {
        val user = getUser(username)
        getExternalData(user.externalId)?.let {
            user.userData.email = it.email
            user.userData.sex = when(it.persoonsgegevens.geslacht) {
                "M" -> Sex.MALE
                "V" -> Sex.FEMALE
                else -> Sex.UNKNOWN
            }
            user.userData.mobile = it.persoonsgegevens.gsm
            user.userData.hasHandicap = it.persoonsgegevens.beperking
            user.userData.hasReduction = it.persoonsgegevens.verminderdlidgeld
            user.userData.accountNo = it.persoonsgegevens.rekeningnummer
            user.userData.birthdate = LocalDate.parse(it.vgagegevens.geboortedatum)
            user.userData.memberId = it.verbondsgegevens.lidnummer
            user.userData.addresses.addAll(it.adressen.map { a -> a.asAddress() } )
            user.userData.contacts.addAll(it.contacten.map { c ->
                val contact = Contact()
                contact.name = c.achternaam
                contact.firstName = c.voornaam
                contact.role = when(c.rol) {
                    "vader" -> ContactRole.FATHER
                    "moeder" -> ContactRole.MOTHER
                    "voogd" -> ContactRole.GUARDIAN
                    else -> ContactRole.RESPONSIBLE
                }
                contact.address = user.userData.addresses.firstOrNull { it.externalId == c.id }
                contact.mobile = c.gsm
                contact.email = c.email
                contact
            })
        }
        return user
    }

    override fun getMedicalRecord(user: User): MedicalRecord? {
        TODO("Not yet implemented")
    }

    override fun updateMedicalRecord(medicalRecord: MedicalRecord) {
        TODO("Not yet implemented")
    }

    private fun getExternalData(externalId: String?): Lid? {
        return cache.computeIfAbsent(externalId ?: return null, ::callWebClient).block()
    }

    private fun callWebClient(externalId: String) = webClientBuilder
        .baseUrl(restGAUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${BearerTokenFilter.getToken()}")
        .build()
        .get()
        .uri { it.path("/lid/{id}").build(externalId) }
        .retrieve()
        .bodyToMono(Lid::class.java)
        .cache(Duration.ofSeconds(5))
}