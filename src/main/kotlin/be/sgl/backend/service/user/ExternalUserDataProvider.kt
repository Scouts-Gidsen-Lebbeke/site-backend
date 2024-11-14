package be.sgl.backend.service.user

import be.sgl.backend.config.security.BearerTokenFilter
import be.sgl.backend.entity.MedicalRecord
import be.sgl.backend.entity.User
import be.sgl.backend.entity.UserRegistration
import be.sgl.backend.repository.UserRepository
import be.sgl.backend.util.Lid
import org.apache.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service
@Primary
class ExternalUserDataProvider : UserDataProvider {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder

    private val cache = ConcurrentHashMap<String, Mono<Lid>>()

    override fun createRegistration(registration: UserRegistration): User {
        TODO("Not yet implemented")
    }

    override fun acceptRegistration(registration: UserRegistration): User {
        TODO("Not yet implemented")
    }

    override fun getUser(username: String): User {
        val user = userRepository.getUserByUsernameEquals(username)
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
        .baseUrl("https://groepsadmin.scoutsengidsenvlaanderen.be/groepsadmin/rest-ga")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${BearerTokenFilter.getToken()}")
        .build()
        .get()
        .uri { it.path("/lid/{id}").build(externalId) }
        .retrieve()
        .bodyToMono(Lid::class.java)
        .cache(Duration.ofSeconds(5))
}