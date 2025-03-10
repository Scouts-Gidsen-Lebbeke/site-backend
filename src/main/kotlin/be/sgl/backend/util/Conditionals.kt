package be.sgl.backend.util

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Conditional(WhenNotBlankCondition::class)
annotation class WhenNotBlank(val value: String, val not: Boolean = false)

class WhenNotBlankCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val attributes = metadata.getAnnotationAttributes(WhenNotBlank::class.java.name) ?: return false
        val property = context.environment.getProperty(attributes["value"] as String)
        val not = attributes["not"] as Boolean
        return (!property.isNullOrBlank() && !not) || (property.isNullOrBlank() && not)
    }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@WhenNotBlank("organization.external.id", true)
annotation class ForInternalOrganization

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@WhenNotBlank("organization.external.id")
annotation class ForExternalOrganization
