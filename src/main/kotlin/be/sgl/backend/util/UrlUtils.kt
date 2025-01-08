package be.sgl.backend.util

import java.net.URLEncoder

fun appendRequestParameter(url: String, name: String, value: Any): String {
    val encodedName = URLEncoder.encode(name, "UTF-8")
    val encodedValue = URLEncoder.encode(value.toString(), "UTF-8")
    return if (url.contains("?")) {
        "$url&$encodedName=$encodedValue"
    } else {
        "$url?$encodedName=$encodedValue"
    }
}