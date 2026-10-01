package com.noorpro.app.data

import java.net.URI
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TravelSearch(
    val origin: String,
    val destination: String,
    val departure: String,
    val returnDate: String,
    val adults: String,
    val roundTrip: Boolean
) {
    fun validationError(today: Date = Date()): String? {
        if (!origin.matches(Regex("[A-Z]{3}")) || !destination.matches(Regex("[A-Z]{3}")))
            return "Enter three-letter airport codes, such as BLR and JED."
        if (origin == destination) return "Choose different departure and destination airports."
        val depart = parseTravelDate(departure) ?: return "Enter departure as YYYY-MM-DD."
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startToday = format.parse(format.format(today)) ?: today
        if (depart.before(startToday)) return "Departure cannot be in the past."
        if (roundTrip) {
            val back = parseTravelDate(returnDate) ?: return "Enter return as YYYY-MM-DD."
            if (back.before(depart)) return "Return cannot be before departure."
        }
        val count = adults.toIntOrNull() ?: return "Choose between 1 and 9 adult passengers."
        if (count !in 1..9) return "Choose between 1 and 9 adult passengers."
        return null
    }
}

private fun parseTravelDate(value: String): Date? {
    if (!value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
    return runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value) }.getOrNull()
}

data class TravelPartner(val name: String, val template: String, val enabled: Boolean) {
    fun bookingUrl(search: TravelSearch? = null): String? {
        if (!enabled || name.isBlank()) return null
        val base = runCatching { URI(template) }.getOrNull() ?: return null
        if (base.scheme != "https" || base.host.isNullOrBlank() || base.userInfo != null || base.port !in listOf(-1, 443)) return null
        var result = template
        val values = search?.let { mapOf(
            "origin" to it.origin, "destination" to it.destination,
            "departure" to it.departure, "return" to if (it.roundTrip) it.returnDate else "",
            "adults" to it.adults, "trip" to if (it.roundTrip) "return" else "oneway"
        ) }.orEmpty()
        values.forEach { (key, value) -> result = result.replace("__${key.uppercase(Locale.US)}__", URLEncoder.encode(value, "UTF-8")) }
        if (Regex("__[A-Z]+__").containsMatchIn(result)) return null
        return result
    }
}
