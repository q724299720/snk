package com.snk.app.ui.auth

import com.snk.app.data.auth.AuthErrorCode
import com.snk.app.data.auth.AuthResult
import com.snk.app.data.auth.AuthenticatedAccount
import com.snk.app.data.auth.RegistrationResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthStatePolicyTest {
    @Test fun `successful login enters authenticated or forced password state`() {
        assertEquals(AuthUiState.Authenticated(account(false)), AuthStatePolicy.fromLogin(AuthResult.Success(account(false))))
        assertEquals(AuthUiState.MustChangePassword(account(true)), AuthStatePolicy.fromLogin(AuthResult.Success(account(true))))
    }

    @Test fun `account errors become explicit non business states`() {
        assertEquals(AuthUiState.Pending(null), AuthStatePolicy.fromLogin(AuthResult.Failure(AuthErrorCode.ACCOUNT_PENDING)))
        assertEquals(AuthUiState.Rejected, AuthStatePolicy.fromLogin(AuthResult.Failure(AuthErrorCode.ACCOUNT_REJECTED)))
        assertEquals(AuthUiState.Disabled, AuthStatePolicy.fromLogin(AuthResult.Failure(AuthErrorCode.ACCOUNT_DISABLED)))
    }

    @Test fun `registration always enters pending with approval ticket`() {
        val response = RegistrationResponse(7, "alice", "USER", "PENDING", "ticket-secret")
        assertEquals(AuthUiState.Pending("ticket-secret"), AuthStatePolicy.fromRegistration(AuthResult.Success(response)))
    }

    private fun account(mustChange: Boolean) = AuthenticatedAccount(7, "alice", "USER", mustChange)
}
