package com.snk.app.data.food

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.net.Proxy

class FoodSearchRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: FoodSearchRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        repository = FoodSearchRepository(
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
                .create(FoodSearchApi::class.java),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search returns items when backend succeeds`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "qualitySignal": "strong",
                      "items": [
                        {
                          "id": 1,
                          "name": "乐事黄瓜味薯片",
                          "itemType": "packaged_product",
                          "category": "snack",
                          "subcategory": "chips",
                          "brand": "乐事",
                          "barcode": "6900000000011",
                          "coverImageUrl": null
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.search("乐事")

        assertTrue(result is FoodSearchResult.Success)
        val success = result as FoodSearchResult.Success
        assertEquals("strong", success.qualitySignal)
        assertEquals(1, success.items.size)
        assertEquals("乐事黄瓜味薯片", success.items.first().name)
    }

    @Test
    fun `lookupByBarcode returns item when backend succeeds`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 1,
                      "name": "乐事黄瓜味薯片",
                      "itemType": "packaged_product",
                      "category": "snack",
                      "subcategory": "chips",
                      "brand": "乐事",
                      "barcode": "6900000000011",
                      "coverImageUrl": null
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.lookupByBarcode("6900000000011")

        assertTrue(result is FoodBarcodeLookupResult.Success)
        assertEquals("乐事黄瓜味薯片", (result as FoodBarcodeLookupResult.Success).item.name)
    }

    @Test
    fun `lookupByBarcode returns not found when backend misses`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val result = repository.lookupByBarcode("0000000000000")

        assertTrue(result is FoodBarcodeLookupResult.NotFound)
    }

    @Test
    fun `lookupByBarcode returns failure for blank input`() = runTest {
        val result = repository.lookupByBarcode(" ")

        assertTrue(result is FoodBarcodeLookupResult.Failure)
    }
}
