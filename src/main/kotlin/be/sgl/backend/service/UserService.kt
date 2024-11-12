package be.sgl.backend.service

import be.sgl.backend.config.BearerTokenFilter
import be.sgl.backend.dto.UserDTO
import be.sgl.backend.repository.UserRepository
import be.sgl.backend.util.Lid
import be.sgl.backend.util.UserData
import org.apache.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class UserService {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder

    fun getProfile(username: String): UserDTO {
        val user = userRepository.getUserByUsernameEquals(username)
        return UserDTO(user, getUserData(user.externalId))
    }

    private fun getUserData(id: String?): UserData? {
        id ?: return null
        val token = BearerTokenFilter.getToken()
        return webClientBuilder
            .baseUrl("https://groepsadmin.scoutsengidsenvlaanderen.be/groepsadmin/rest-ga")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .build()
            .get()
            .uri { it.path("/lid/{id}").build(id) }
            .retrieve()
            .bodyToMono(Lid::class.java)
            .block()
    }
}