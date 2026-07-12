package com.snk.app.ui.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordChangePolicyTest {
    @Test fun `requires old password twelve character new password and matching confirmation`() {
        assertFalse(PasswordChangePolicy.isValid("", "new-password-12", "new-password-12"))
        assertFalse(PasswordChangePolicy.isValid("old-password-12", "short", "short"))
        assertFalse(PasswordChangePolicy.isValid("old-password-12", "new-password-12", "different-12"))
        assertTrue(PasswordChangePolicy.isValid("old-password-12", "new-password-12", "new-password-12"))
    }
}
