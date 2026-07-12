package com.snk.app.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftSyncWorkerTest {
    @Test
    fun `only the currently signed in owner may sync a draft`() {
        val policyClass = runCatching {
            Class.forName("com.snk.app.sync.DraftSyncOwnershipPolicy")
        }.getOrNull()

        assertNotNull("Draft sync must have an explicit ownership policy.", policyClass)
        val instance = requireNotNull(policyClass).getField("INSTANCE").get(null)
        val canSync = policyClass.getMethod(
            "canSync",
            Long::class.javaPrimitiveType,
            Long::class.javaObjectType,
        )

        assertTrue(canSync.invoke(instance, 101L, 101L) as Boolean)
        assertFalse(canSync.invoke(instance, 101L, 202L) as Boolean)
        assertFalse(canSync.invoke(instance, 101L, null) as Boolean)
    }
}
