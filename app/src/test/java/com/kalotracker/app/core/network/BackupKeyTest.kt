package com.kalotracker.app.core.network

import com.kalotracker.app.core.settings.AiSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupKeyTest {
    private val ok = """{"candidates":[{"content":{"parts":[{"text":"ok"}]}}]}"""

    private class Recording(settings: AiSettings, val fail: Map<String, Exception>, val reply: String) :
        MealAnalysisService({ settings }) {
        val tried = mutableListOf<String>()
        override fun postWithKey(model: String, apiKey: String, body: String): String {
            tried += apiKey
            fail[apiKey]?.let { throw it }
            return reply
        }
    }

    @Test fun keysAreTrimmedOrderedAndDeduplicated() {
        assertEquals(listOf("main", "spare"), AiSettings(" main ", backupApiKey = "spare").keys)
        assertEquals(listOf("main"), AiSettings("main", backupApiKey = "main").keys)
        assertTrue(AiSettings("", backupApiKey = "spare").isConfigured)
        assertFalse(AiSettings("", backupApiKey = " ").isConfigured)
    }

    @Test fun rateLimitedMainKeyFallsBackToBackupKey() = runTest {
        val service = Recording(AiSettings("main", backupApiKey = "spare"),
            mapOf("main" to MealAnalysisException.RateLimited()), ok)
        assertEquals("ok", service.generateText("hi").getOrThrow())
        assertEquals(listOf("main", "spare"), service.tried)
    }

    @Test fun backupIsNotUsedWhenMainKeyWorks() = runTest {
        val service = Recording(AiSettings("main", backupApiKey = "spare"), emptyMap(), ok)
        service.generateText("hi").getOrThrow()
        assertEquals(listOf("main"), service.tried)
    }

    @Test fun lastErrorIsReportedWhenBothKeysFail() = runTest {
        val service = Recording(AiSettings("main", backupApiKey = "spare"),
            mapOf("main" to MealAnalysisException.RateLimited(), "spare" to MealAnalysisException.BadKey("invalid")), ok)
        assertTrue(service.generateText("hi").exceptionOrNull() is MealAnalysisException.BadKey)
        assertEquals(listOf("main", "spare"), service.tried)
    }

    @Test fun backupKeyAloneIsEnough() = runTest {
        val service = Recording(AiSettings("", backupApiKey = "spare"), emptyMap(), ok)
        assertEquals("ok", service.generateText("hi").getOrThrow())
        assertEquals(listOf("spare"), service.tried)
    }
}
