package be.sgl.backend.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class LocalizedException(val messageKey: String, vararg val params: Any)  : Exception(messageKey)