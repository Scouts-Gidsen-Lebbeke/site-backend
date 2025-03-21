package be.sgl.backend.util

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraintvalidation.SupportedValidationTarget
import jakarta.validation.constraintvalidation.ValidationTarget
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass


@Constraint(validatedBy = [PhoneNumberValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class PhoneNumber(
    val message: String = "{PhoneNumber.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
class PhoneNumberValidator : ConstraintValidator<PhoneNumber, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return value.nullIfBlank()?.matches(Regex("^[0-9]{9,10}\$")) ?: true
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

@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
class NisValidator : ConstraintValidator<Nis, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        value ?: return true
        if (!value.matches(Regex("[0-9]{11}"))) {
            return false
        }
        val base = value.substring(0, 9).toLong()
        val check = value.substring(9, 11).toLong()
        return checksum(base, check) || checksum("2$base".toLong(), check)
    }

    private fun checksum(base: Long, check: Long) = 97 - (base % 97) == check
}

@Constraint(validatedBy = [CountryCodeValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class CountryCode(
    val message: String = "{CountryCode.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
class CountryCodeValidator : ConstraintValidator<CountryCode, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        return value == null || Locale.getISOCountries().contains(value.uppercase())
    }
}

@Constraint(validatedBy = [KboValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Kbo(
    val message: String = "{Kbo.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
class KboValidator : ConstraintValidator<Kbo, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return value.nullIfBlank()?.matches(Regex("^0[0-9]{9}\$")) ?: true
    }
}

@StartBeforeEnd
interface StartEndTime {
    val start: LocalDateTime
    val end: LocalDateTime
}

@StartBeforeEnd
interface StartEndDate {
    val start: LocalDate
    val end: LocalDate
}

@Constraint(validatedBy = [StartEndTimeValidator::class, StartEndDateValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StartBeforeEnd(
    val message: String = "{StartBeforeEnd.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
class StartEndTimeValidator : ConstraintValidator<StartBeforeEnd, StartEndTime?> {
    override fun isValid(value: StartEndTime?, context: ConstraintValidatorContext): Boolean {
        value ?: return true
        return value.start < value.end
    }
}

@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
class StartEndDateValidator : ConstraintValidator<StartBeforeEnd, StartEndDate?> {
    override fun isValid(value: StartEndDate?, context: ConstraintValidatorContext): Boolean {
        value ?: return true
        return value.start < value.end
    }
}
