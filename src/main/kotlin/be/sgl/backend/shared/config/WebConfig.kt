package be.sgl.backend.shared.config

import be.sgl.backend.shared.image.ImageService.Companion.IMAGE_BASE_PATH
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/$IMAGE_BASE_PATH/**")
            .addResourceLocations("file:$IMAGE_BASE_PATH/")
    }
}