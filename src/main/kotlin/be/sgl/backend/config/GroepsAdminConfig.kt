package be.sgl.backend.config

import be.sgl.backend.config.security.BearerTokenFilter
import be.sgl.backend.openapi.ApiClient
import be.sgl.backend.openapi.api.*
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime

@Configuration
class GroepsAdminConfig {

    @Bean
    fun getGroepsAdminApiClient(@Value("\${rest.ga.url}") basePath: String): ApiClient {
        val module = JavaTimeModule()
        module.addDeserializer(OffsetDateTime::class.java, OffsetDateTimeDeserializer())
        val objectMapper = ObjectMapper()
            .registerModule(module)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return DynamicAuthApiClient(basePath, objectMapper)
    }

    @Bean
    fun getLedenApi(groepsAdminApiClient: ApiClient): LedenApi {
        return LedenApi(groepsAdminApiClient)
    }

    @Bean
    fun getLidaanvragenApi(groepsAdminApiClient: ApiClient): LidaanvragenApi {
        return LidaanvragenApi(groepsAdminApiClient)
    }

    @Bean
    fun getGroepenApi(groepsAdminApiClient: ApiClient): GroepenApi {
        return GroepenApi(groepsAdminApiClient)
    }

    @Bean
    fun getFunctiesApi(groepsAdminApiClient: ApiClient): FunctiesApi {
        return FunctiesApi(groepsAdminApiClient)
    }

    @Bean
    fun getLedenlijstApi(groepsAdminApiClient: ApiClient): LedenlijstApi {
        return LedenlijstApi(groepsAdminApiClient)
    }

    // apparently bearerToken authentication is still not implemented,
    // see https://openapi-generator.tech/docs/generators/spring/#security-feature
    private class DynamicAuthApiClient(basePath: String, objectMapper: ObjectMapper) : ApiClient(objectMapper, createDefaultDateFormat()) {

        init {
            this.basePath = basePath
        }

        override fun <T : Any?> invokeAPI(path: String?, method: HttpMethod?, pathParams: MutableMap<String, Any>?,
                                          queryParams: MultiValueMap<String, String>?, body: Any?, headerParams: HttpHeaders?,
                                          cookieParams: MultiValueMap<String, String>?, formParams: MultiValueMap<String, Any>?,
                                          accept: MutableList<MediaType>?, contentType: MediaType?, authNames: Array<out String>?,
                                          returnType: ParameterizedTypeReference<T>?): RestClient.ResponseSpec {
            BearerTokenFilter.getToken()?.let { headerParams?.setBearerAuth(it) }
            headerParams?.accept = listOf(MediaType.APPLICATION_JSON)
            return super.invokeAPI(path, method, pathParams, queryParams, body, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType)
        }
    }

    class OffsetDateTimeDeserializer : JsonDeserializer<OffsetDateTime>() {
        override fun deserialize(p0: JsonParser, p1: DeserializationContext): OffsetDateTime {
            return OffsetDateTime.parse(p0.text)
        }
    }
}