package be.sgl.backend.config

import be.sgl.backend.service.ImageService.Companion.IMAGE_BASE_PATH
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    @Value("\${server.servlet.context-path:}")
    private lateinit var contextPath: String

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("$contextPath/$IMAGE_BASE_PATH/**")
            .addResourceLocations("file:$IMAGE_BASE_PATH/")
    }
}