package com.noorpro.app.data

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class TravelBookingTest {
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-10-01")!!
    private val valid = TravelSearch("BLR", "JED", "2026-10-02", "2026-10-10", "1", true)

    @Test fun acceptsValidReturnAndOneWayJourneys() {
        assertNull(valid.validationError(today))
        assertNull(valid.copy(roundTrip = false, returnDate = "").validationError(today))
    }

    @Test fun rejectsImpossiblePastAndReversedDates() {
        assertNotNull(valid.copy(departure = "2026-02-30").validationError(today))
        assertNotNull(valid.copy(departure = "2026-09-30").validationError(today))
        assertNotNull(valid.copy(returnDate = "2026-10-01").validationError(today))
    }

    @Test fun rejectsInvalidAirportsAndPassengers() {
        assertNotNull(valid.copy(origin = "Bangalore").validationError(today))
        assertNotNull(valid.copy(destination = "BLR").validationError(today))
        listOf("0", "10", "abc", "").forEach { assertNotNull(valid.copy(adults = it).validationError(today)) }
    }

    @Test fun blocksDisabledAndUnsafePartnerLinks() {
        listOf("http://example.com", "javascript:alert(1)", "https://user:password@example.com", "https://example.com:8080").forEach {
            assertNull(TravelPartner("Partner", it, true).bookingUrl(valid))
        }
        assertNull(TravelPartner("Partner", "https://example.com", false).bookingUrl(valid))
        assertNull(TravelPartner("", "https://example.com", true).bookingUrl(valid))
    }

    @Test fun expandsConfiguredSearchParametersAndClearsOneWayReturn() {
        val partner = TravelPartner("Partner", "https://example.com/search?from=__ORIGIN__&to=__DESTINATION__&date=__DEPARTURE__&back=__RETURN__&adults=__ADULTS__&trip=__TRIP__", true)
        assertEquals("https://example.com/search?from=BLR&to=JED&date=2026-10-02&back=2026-10-10&adults=1&trip=return", partner.bookingUrl(valid))
        assertTrue(partner.bookingUrl(valid.copy(roundTrip = false))!!.contains("back=&"))
        assertNull(TravelPartner("Partner", "https://example.com/?q=__UNKNOWN__", true).bookingUrl(valid))
    }
}
