package be.sgl.backend.util

import java.util.Base64

fun String?.nullIfBlank() = this?.takeIf(String::isNotBlank)

fun String.base64Encoded(): String = Base64.getEncoder().encodeToString(toByteArray())