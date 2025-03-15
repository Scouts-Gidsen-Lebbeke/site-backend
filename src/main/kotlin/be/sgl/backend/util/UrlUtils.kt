package be.sgl.backend.util

import java.net.URLEncoder.encode

fun appendRequestParameters(url: String, vararg params: Pair<String, Any?>): String {
    return params.joinToString("&", if (url.contains("?")) "$url&" else "$url?") {
        "${encode(it.first, "UTF-8")}=${encode(it.second.toString(), "UTF-8")}"
    }
}