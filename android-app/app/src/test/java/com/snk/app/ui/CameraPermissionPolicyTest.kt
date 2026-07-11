package com.snk.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPermissionPolicyTest {
    @Test
    fun `first denial degrades and repeated permanent denial opens settings path`() {
        assertEquals(CameraDenialKind.FIRST, classifyCameraDenial(denialCount = 0, shouldShowRationale = false))
        assertEquals(CameraDenialKind.RETRYABLE, classifyCameraDenial(denialCount = 1, shouldShowRationale = true))
        assertEquals(CameraDenialKind.PERMANENT, classifyCameraDenial(denialCount = 1, shouldShowRationale = false))
    }
}
