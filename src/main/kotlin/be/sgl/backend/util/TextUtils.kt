package be.sgl.backend.util

fun String?.nullIfBlank() = this?.takeIf(String::isNotBlank)