package be.sgl.backend.util

import be.sgl.backend.config.LocaleConfig.Companion.BE_NL
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun String?.nullIfBlank() = this?.takeIf(String::isNotBlank)

fun String.base64Encoded(): String = Base64.getEncoder().encodeToString(toByteArray())

fun LocalDate.belgian(): String = format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun LocalDateTime.belgian(): String = format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun Double?.pricePrecision() = this?.let { String.format(BE_NL, "%.2f", it) }

fun Double?.priceWithCurrency() = this?.let { "€ " + String.format(BE_NL, "%.2f", it) }

fun Double.reducePrice(factor: Double) = BigDecimal(this / factor).setScale(2, RoundingMode.HALF_UP).toDouble()