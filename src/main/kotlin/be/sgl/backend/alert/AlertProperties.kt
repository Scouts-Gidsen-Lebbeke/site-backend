package be.sgl.backend.alert

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@Validated
@ConfigurationProperties(prefix = "sgl.alert")
class AlertProperties {
    @setparam:DefaultValue("true")
    var enabled: Boolean = true
    @field:NotNull
    @field:Email
    var recipient: String? = null
    @field:NotNull
    var environment: String? = null
}