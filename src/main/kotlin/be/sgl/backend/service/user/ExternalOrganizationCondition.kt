package be.sgl.backend.service.user

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

open class InternalOrganizationCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        return context.environment.getProperty("external.organization.id").isNullOrBlank()
    }
}

class ExternalOrganizationCondition : InternalOrganizationCondition() {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata) = !super.matches(context, metadata)
}