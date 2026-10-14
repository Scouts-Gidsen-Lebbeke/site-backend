package be.sgl.backend.config

import be.sgl.backend.config.security.BearerTokenFilter
import be.sgl.backend.openapi.ApiClient
import be.sgl.backend.openapi.api.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Configuration
class GroepsAdminConfig {

    @Bean
    fun getGroepsAdminApiClient(@Value("\${rest.ga.url}") basePath: String): ApiClient {
        val module = SimpleModule()
            .addDeserializer(
                OffsetDateTime::class.java,
                OffsetDateTimeDeserializer()
            )

        val objectMapper = JsonMapper.builder()
            .addModule(module)
            .build()
        return DynamicAuthApiClient(basePath, objectMapper)
    }

    class OffsetDateTimeDeserializer : ValueDeserializer<OffsetDateTime>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): OffsetDateTime {
            return OffsetDateTime.parse(p.string)
                .withOffsetSameInstant(ZoneOffset.ofHours(2))
        }
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
    private class DynamicAuthApiClient(basePath: String, objectMapper: JsonMapper) : ApiClient(objectMapper, createDefaultDateFormat()) {

        init {
            this.basePath = basePath
        }

        override fun <T : Any> invokeAPI(path: String?, method: HttpMethod?, pathParams: MutableMap<String, Any>?,
                                          queryParams: MultiValueMap<String, String>?, body: Any?, headerParams: HttpHeaders?,
                                          cookieParams: MultiValueMap<String, String>?, formParams: MultiValueMap<String, Any>?,
                                          accept: MutableList<MediaType>?, contentType: MediaType?, authNames: Array<out String>?,
                                          returnType: ParameterizedTypeReference<T>?): RestClient.ResponseSpec {
            BearerTokenFilter.getToken()?.let { headerParams?.setBearerAuth(it) }
            headerParams?.accept = listOf(MediaType.APPLICATION_JSON)
            return super.invokeAPI(path, method, pathParams, queryParams, body, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType)
        }
    }
}