package com.noorpro.app.ui.screens

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthErrorTest {
    @Test
    fun cancellationDoesNotClaimTheUserClosedThePicker() {
        val message = friendlyGoogleAuthError(GetCredentialCancellationException())
        assertTrue(message.contains("did not finish"))
        assertFalse(message.contains("was closed"))
    }

    @Test
    fun missingCredentialDoesNotClaimNoGoogleAccountExists() {
        val message = friendlyGoogleAuthError(NoCredentialException())
        assertFalse(message.contains("No Google account was found"))
    }

    @Test
    fun unrelatedNumberIsNotTreatedAsAnOAuthConfigurationError() {
        val message = friendlyGoogleAuthError(Exception("Service unavailable after 10 attempts"))
        assertFalse(message.contains("configured"))
        assertFalse(message.contains("SHA"))
    }

    @Test
    fun developerErrorExplainsAppConfigurationFailure() {
        assertTrue(friendlyGoogleAuthError(Exception("DEVELOPER_ERROR")).contains("not configured correctly"))
    }
}
