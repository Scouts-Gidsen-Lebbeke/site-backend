package be.sgl.backend.shared.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class IncompleteConfigurationException(message: String) : Exception(message)