package com.snk.app.data.food

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.Proxy
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
                          "coverImageUrl": null,
                          "auditStatus": "approved"
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
        assertEquals("approved", success.items.first().auditStatus)
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
                      "coverImageUrl": null,
                      "auditStatus": "approved"
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

    @Test
    fun `createManualFoodItem returns pending item when backend succeeds`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "id": 9,
                      "name": "杨枝鲜花饼",
                      "itemType": "dish",
                      "category": "dessert",
                      "subcategory": "cake",
                      "brand": "SNK Bakery",
                      "barcode": null,
                      "coverImageUrl": null,
                      "auditStatus": "pending"
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.createManualFoodItem(
            userId = 2L,
            name = "杨枝鲜花饼",
            itemType = "dish",
            category = "dessert",
            subcategory = "cake",
            brand = "SNK Bakery",
        )

        assertTrue(result is ManualFoodCreateResult.Success)
        val success = result as ManualFoodCreateResult.Success
        assertEquals("pending", success.item.auditStatus)
        assertEquals("/api/foods/manual", server.takeRequest().path)
    }

    @Test
    fun `searchByRecognizedText falls back to compact query when first query misses`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "qualitySignal": "weak",
                      "items": []
                    }
                    """.trimIndent(),
                ),
        )
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
                          "coverImageUrl": null,
                          "auditStatus": "approved"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.searchByRecognizedText("乐事\n黄瓜味 薯片")

        assertTrue(result is FoodOcrSearchResult.Success)
        val success = result as FoodOcrSearchResult.Success
        assertEquals("乐事 黄瓜味 薯片", success.attemptedQueries.first())
        assertEquals("乐事黄瓜味薯片", success.matchedQuery)
        assertEquals("乐事黄瓜味薯片", success.result.items.first().name)
        assertEquals("/api/foods/search?q=%E4%B9%90%E4%BA%8B%20%E9%BB%84%E7%93%9C%E5%91%B3%20%E8%96%AF%E7%89%87", server.takeRequest().path)
        assertEquals("/api/foods/search?q=%E4%B9%90%E4%BA%8B%E9%BB%84%E7%93%9C%E5%91%B3%E8%96%AF%E7%89%87", server.takeRequest().path)
    }

    @Test
    fun `searchByRecognizedText returns no match when all queries miss`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"qualitySignal":"weak","items":[]}"""),
        )
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"qualitySignal":"weak","items":[]}"""),
        )

        val result = repository.searchByRecognizedText("乐事 黄瓜味")

        assertTrue(result is FoodOcrSearchResult.NoMatch)
    }
}
