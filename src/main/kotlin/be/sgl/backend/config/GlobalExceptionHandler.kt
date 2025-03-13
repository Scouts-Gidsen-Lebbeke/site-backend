package be.sgl.backend.config

import be.sgl.backend.service.exception.ImageException
import be.sgl.backend.service.exception.IncompleteConfigurationException
import be.sgl.backend.service.exception.NotFoundException
import be.woutschoovaerts.mollie.exception.MollieException
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<BadRequestResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity(BadRequestResponse("Validation error(s)", errors), BAD_REQUEST)
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): ResponseEntity<String> {
        return ResponseEntity(ex.message, NOT_FOUND)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateExceptions(ex: IllegalStateException): ResponseEntity<BadRequestResponse> {
        return ResponseEntity(BadRequestResponse(ex.message), BAD_REQUEST)
    }

    @ExceptionHandler(IncompleteConfigurationException::class)
    fun handleIncompleteConfigurationException(ex: IncompleteConfigurationException): ResponseEntity<String> {
        return ResponseEntity(ex.message, CONFLICT)
    }

    @ExceptionHandler(ImageException::class)
    fun handleImageException(ex: ImageException): ResponseEntity<String> {
        return ResponseEntity(ex.message, INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(NotImplementedError::class)
    fun handleCoffeeException(ex: NotImplementedError): ResponseEntity<String> {
        return ResponseEntity("Drink some tea", I_AM_A_TEAPOT)
    }

    @ExceptionHandler(MollieException::class)
    fun handleMollieException(ex: MollieException): ResponseEntity<BadRequestResponse> {
        return ResponseEntity(BadRequestResponse(ex.message, ex.details), BAD_REQUEST)
    }
}

@Schema(description = "Wrapper for all validation errors.")
data class BadRequestResponse(
    val message: String?,
    val errors: Map<String, Any>? = null
)

