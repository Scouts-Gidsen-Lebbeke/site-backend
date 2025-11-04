package be.sgl.backend.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotatedTypeMetadata

class ConditionalsTest {

    @Test
    fun `WhenNotBlankCondition should match when property is not blank and not is false`() {
        val condition = WhenNotBlankCondition()
        val context = mock(ConditionContext::class.java)
        val metadata = mock(AnnotatedTypeMetadata::class.java)
        val environment = mock(Environment::class.java)

        `when`(context.environment).thenReturn(environment)
        `when`(metadata.getAnnotationAttributes(WhenNotBlank::class.java.name))
            .thenReturn(mapOf("value" to "test.property", "not" to false))
        `when`(environment.getProperty("test.property")).thenReturn("some-value")

        assertTrue(condition.matches(context, metadata))
    }

    @Test
    fun `WhenNotBlankCondition should not match when property is blank and not is false`() {
        val condition = WhenNotBlankCondition()
        val context = mock(ConditionContext::class.java)
        val metadata = mock(AnnotatedTypeMetadata::class.java)
        val environment = mock(Environment::class.java)

        `when`(context.environment).thenReturn(environment)
        `when`(metadata.getAnnotationAttributes(WhenNotBlank::class.java.name))
            .thenReturn(mapOf("value" to "test.property", "not" to false))
        `when`(environment.getProperty("test.property")).thenReturn("")

        assertFalse(condition.matches(context, metadata))
    }

    @Test
    fun `WhenNotBlankCondition should match when property is blank and not is true`() {
        val condition = WhenNotBlankCondition()
        val context = mock(ConditionContext::class.java)
        val metadata = mock(AnnotatedTypeMetadata::class.java)
        val environment = mock(Environment::class.java)

        `when`(context.environment).thenReturn(environment)
        `when`(metadata.getAnnotationAttributes(WhenNotBlank::class.java.name))
            .thenReturn(mapOf("value" to "test.property", "not" to true))
        `when`(environment.getProperty("test.property")).thenReturn("")

        assertTrue(condition.matches(context, metadata))
    }

    @Test
    fun `WhenNotBlankCondition should not match when property is not blank and not is true`() {
        val condition = WhenNotBlankCondition()
        val context = mock(ConditionContext::class.java)
        val metadata = mock(AnnotatedTypeMetadata::class.java)
        val environment = mock(Environment::class.java)

        `when`(context.environment).thenReturn(environment)
        `when`(metadata.getAnnotationAttributes(WhenNotBlank::class.java.name))
            .thenReturn(mapOf("value" to "test.property", "not" to true))
        `when`(environment.getProperty("test.property")).thenReturn("some-value")

        assertFalse(condition.matches(context, metadata))
    }

    @Test
    fun `WhenNotBlankCondition should not match when property is null and not is false`() {
        val condition = WhenNotBlankCondition()
        val context = mock(ConditionContext::class.java)
        val metadata = mock(AnnotatedTypeMetadata::class.java)
        val environment = mock(Environment::class.java)

        `when`(context.environment).thenReturn(environment)
        `when`(metadata.getAnnotationAttributes(WhenNotBlank::class.java.name))
            .thenReturn(mapOf("value" to "test.property", "not" to false))
        `when`(environment.getProperty("test.property")).thenReturn(null)

        assertFalse(condition.matches(context, metadata))
    }

    @Test
    fun `WhenNotBlankCondition should match when property is null and not is true`() {
        val condition = WhenNotBlankCondition()
        val context = mock(ConditionContext::class.java)
        val metadata = mock(AnnotatedTypeMetadata::class.java)
        val environment = mock(Environment::class.java)

        `when`(context.environment).thenReturn(environment)
        `when`(metadata.getAnnotationAttributes(WhenNotBlank::class.java.name))
            .thenReturn(mapOf("value" to "test.property", "not" to true))
        `when`(environment.getProperty("test.property")).thenReturn(null)

        assertTrue(condition.matches(context, metadata))
    }

    @Test
    fun `WhenNotBlankCondition should return false when annotation attributes are null`() {
        val condition = WhenNotBlankCondition()
        val context = mock(ConditionContext::class.java)
        val metadata = mock(AnnotatedTypeMetadata::class.java)

        `when`(metadata.getAnnotationAttributes(WhenNotBlank::class.java.name)).thenReturn(null)

        assertFalse(condition.matches(context, metadata))
    }

    @Test
    fun `ForInternalOrganization annotation should be present`() {
        val annotation = ForInternalOrganization::class.java.getAnnotation(WhenNotBlank::class.java)
        assertNotNull(annotation)
        assertEquals("organization.external.id", annotation.value)
        assertTrue(annotation.not)
    }

    @Test
    fun `ForExternalOrganization annotation should be present`() {
        val annotation = ForExternalOrganization::class.java.getAnnotation(WhenNotBlank::class.java)
        assertNotNull(annotation)
        assertEquals("organization.external.id", annotation.value)
        assertFalse(annotation.not)
    }
}
