package be.sgl.backend.config

import be.sgl.backend.exception.LocalizedException
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.*

@ControllerAdvice
class LocalizedExceptionHandler(
    private val messageSource: MessageSource
) {

    @ExceptionHandler(LocalizedException::class)
    fun handleLocalized(ex: LocalizedException, locale: Locale): ResponseEntity<ApiErrorResponse> {
        val localizedMessage = messageSource.getMessage(ex.messageKey, ex.params, locale)
        val response = ApiErrorResponse(BAD_REQUEST, BAD_REQUEST.ordinal.toString(), localizedMessage)
        return ResponseEntity.status(BAD_REQUEST).body(response)
    }
}
