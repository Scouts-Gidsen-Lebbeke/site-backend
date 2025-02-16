package be.sgl.backend.util

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraintvalidation.SupportedValidationTarget
import jakarta.validation.constraintvalidation.ValidationTarget
import kotlin.reflect.KClass

@Constraint(validatedBy = [PhoneNumberValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class PhoneNumber(
    val message: String = "{PhoneNumber.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@SupportedValidationTarget(ValidationTarget.PARAMETERS)
class PhoneNumberValidator : ConstraintValidator<PhoneNumber, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return value?.matches(Regex("^[0-9]{9,10}\$")) ?: true
    }
}

@Constraint(validatedBy = [NisValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Nis(
    val message: String = "{Nis.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@SupportedValidationTarget(ValidationTarget.PARAMETERS)
class NisValidator : ConstraintValidator<Nis, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        value ?: return true
        if (!value.matches(Regex("[0-9.\\s-]*"))) {
            return false
        }
        val nis = value.replace(Regex("[.\\s-]"), "")
        val base = nis.substring(0, 9).toLong()
        val check = nis.substring(9, 11).toLong()
        return checksum(base, check) || checksum("2$base".toLong(), check)
    }

    private fun checksum(base: Long, check: Long) = 97 - (base % 97) == check
}