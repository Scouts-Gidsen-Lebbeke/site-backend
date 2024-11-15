package be.sgl.backend.service.user

import be.sgl.backend.config.security.BearerTokenFilter
import be.sgl.backend.entity.MedicalRecord
import be.sgl.backend.entity.User
import be.sgl.backend.entity.UserRegistration
import be.sgl.backend.entity.UserRole
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.UserRepository
import be.sgl.backend.util.Functie
import be.sgl.backend.util.Lid
import org.apache.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@Service
@ConditionalOnProperty(name = ["external.organization.id"], matchIfMissing = false)
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
        val endDate = function.einde?.let { LocalDate.parse(it) }
        return UserRole(user, role, LocalDate.parse(function.begin), endDate)
    }

    override fun getUserWithAllData(username: String): User {
        val user = getUser(username)
        getExternalData(user.externalId)?.let {
            user.userData.mobile
        }
        return user
    }

    override fun getMedicalRecord(username: String): MedicalRecord {
        TODO("Not yet implemented")
    }

    override fun updateMedicalRecord(username: String) {
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