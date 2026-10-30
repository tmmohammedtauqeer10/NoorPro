package com.noorpro.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneAuthErrorTest {
    @Test
    fun connectionAbortIsShownAsActionableRetryMessage() {
        val error = RuntimeException("An internal error has occurred. [ Software caused connection abort ]")

        assertTrue(isTransientPhoneNetworkError(error))
        assertEquals(
            "Couldn't reach the SMS verification service. Check your internet, turn off VPN or Private DNS, then tap Resend code.",
            friendlyPhoneError(error)
        )
    }

    @Test
    fun unknownFirebaseFailureDoesNotExposeInternalText() {
        val error = RuntimeException("INTERNAL ASSERTION FAILED: secret implementation detail")

        assertFalse(isTransientPhoneNetworkError(error))
        assertEquals(
            "Unable to send or verify the code right now. Please try again, or continue with Google or email.",
            friendlyPhoneError(error)
        )
    }
}
