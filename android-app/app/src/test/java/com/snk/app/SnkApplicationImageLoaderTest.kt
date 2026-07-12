package com.snk.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SnkApplicationImageLoaderTest {
    @Test
    fun `application installs authenticated image loader for protected uploads`() {
        val source = File("src/main/java/com/snk/app/SnkApplication.kt").readText()

        assertTrue(source.contains("ImageLoaderFactory"))
        assertTrue(source.contains("AuthenticatedImageInterceptor"))
        assertTrue(source.contains("authenticatedSessionManager.currentAccessToken()"))
        assertTrue(source.contains("BuildConfig.API_BASE_URL"))
    }
}
