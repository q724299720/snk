package com.snk.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationSourceTest {
    @Test
    fun `primary navigation is home records discover and profile`() {
        val source = File("src/main/java/com/snk/app/ui/SnkApp.kt").readText()

        assertTrue(source.contains("Discover : SnkDestination(\"discover\", \"发现\""))
        assertTrue(source.contains("SnkDestination.Discover"))
        assertFalse(source.contains("data object Drafts : SnkDestination"))
    }
}
