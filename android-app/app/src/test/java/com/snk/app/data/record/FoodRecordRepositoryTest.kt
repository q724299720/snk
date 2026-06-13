package com.snk.app.data.record

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.net.Proxy

class FoodRecordRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: FoodRecordRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        repository = FoodRecordRepository(
            api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(
                    OkHttpClient.Builder()
                        .proxy(Proxy.NO_PROXY)
                        .build(),
                )
                .addConverterFactory(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }.asConverterFactory("application/json".toMediaType()),
                )
                .build()
                .create(FoodRecordApi::class.java),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `createRecord returns success when backend accepts request`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "id": 55,
                      "userId": 100,
                      "foodItemId": 200,
                      "sourceType": "text_search",
                      "isPublic": false,
                      "rating": 5,
                      "comment": "很好吃",
                      "recordTime": "2026-06-13T23:40:00Z",
                      "createdAt": "2026-06-13T23:40:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.createRecord(
            userId = 100,
            foodItemId = 200,
            rating = 5,
            comment = "很好吃",
        )

        assertTrue(result is FoodRecordCreateResult.Success)
        assertEquals(55L, (result as FoodRecordCreateResult.Success).recordId)
    }
}
