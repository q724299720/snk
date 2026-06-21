package com.snk.app.data.food

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.Proxy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
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
                          "name": "Lays Cucumber Chips",
                          "itemType": "packaged_product",
                          "category": "snack",
                          "subcategory": "chips",
                          "brand": "Lays",
                          "barcode": "6900000000011",
                          "coverImageUrl": null,
                          "auditStatus": "approved"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.search("Lays")

        assertTrue(result is FoodSearchResult.Success)
        val success = result as FoodSearchResult.Success
        assertEquals("strong", success.qualitySignal)
        assertEquals(1, success.items.size)
        assertEquals("Lays Cucumber Chips", success.items.first().name)
        assertEquals("approved", success.items.first().auditStatus)
    }

    @Test
    fun `search sends user id when available so backend can include own pending items`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "qualitySignal": "strong",
                      "items": [
                        {
                          "id": 9,
                          "name": "Mango Cake",
                          "itemType": "packaged_product",
                          "category": "snack",
                          "subcategory": "cake",
                          "brand": "SNK Bakery",
                          "barcode": null,
                          "coverImageUrl": null,
                          "auditStatus": "pending"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.search("Mango Cake", userId = 2L)

        assertTrue(result is FoodSearchResult.Success)
        val success = result as FoodSearchResult.Success
        assertEquals("pending", success.items.first().auditStatus)
        assertEquals("/api/foods/search?q=Mango%20Cake&userId=2", server.takeRequest().path)
    }

    @Test
    fun `search rejects overlong query before calling backend`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"qualitySignal":"strong","items":[]}"""),
        )

        val result = repository.search("a".repeat(129))

        assertTrue(result is FoodSearchResult.Failure)
        val failure = result as FoodSearchResult.Failure
        assertEquals("搜索词最长支持 128 个字符，请缩短后再试。", failure.message)
        assertEquals(0, server.requestCount)
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
                      "name": "Mango Cake",
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
            name = "Mango Cake",
            itemType = "packaged_product",
            category = "snack",
            subcategory = "chips",
            brand = "SNK Bakery",
            barcode = "6900000000099",
        )

        assertTrue(result is ManualFoodCreateResult.Success)
        val success = result as ManualFoodCreateResult.Success
        assertEquals("pending", success.item.auditStatus)
        val request = server.takeRequest()
        assertEquals("/api/foods/manual", request.path)
        val body = Buffer().write(request.body.readByteArray()).readUtf8()
        assertTrue(body.contains("\"barcode\":\"6900000000099\""))
    }

    @Test
    fun `reportFoodItem returns report summary when backend succeeds`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "foodItemId": 18,
                      "reportCount": 3,
                      "auditStatus": "pending"
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.reportFoodItem(
            userId = 2L,
            foodItemId = 18L,
        )

        assertTrue(result is FoodReportResult.Success)
        val success = result as FoodReportResult.Success
        assertEquals(18L, success.foodItemId)
        assertEquals(3, success.reportCount)
        assertEquals("pending", success.auditStatus)

        val request = server.takeRequest()
        assertEquals("/api/foods/18/report", request.path)
        val body = Buffer().write(request.body.readByteArray()).readUtf8()
        assertTrue(body.contains("\"userId\":2"))
    }

    @Test
    fun `searchByRecognizedText falls back to compact query when first query misses`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"qualitySignal":"weak","items":[]}"""),
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
                          "name": "LaysCucumberChips",
                          "itemType": "packaged_product",
                          "category": "snack",
                          "subcategory": "chips",
                          "brand": "Lays",
                          "barcode": "6900000000011",
                          "coverImageUrl": null,
                          "auditStatus": "approved"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.searchByRecognizedText("Lays\nCucumber Chips")

        assertTrue(result is FoodOcrSearchResult.Success)
        val success = result as FoodOcrSearchResult.Success
        assertEquals("Lays Cucumber Chips", success.attemptedQueries.first())
        assertEquals("LaysCucumberChips", success.matchedQuery)
        assertEquals("LaysCucumberChips", success.result.items.first().name)
        assertEquals("/api/foods/search?q=Lays%20Cucumber%20Chips", server.takeRequest().path)
        assertEquals("/api/foods/search?q=LaysCucumberChips", server.takeRequest().path)
    }

    @Test
    fun `searchByRecognizedText skips overlong query and keeps trying shorter candidates`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "qualitySignal": "strong",
                      "items": [
                        {
                          "id": 18,
                          "name": "牛肉面",
                          "itemType": "packaged_product",
                          "category": "instant_food",
                          "subcategory": "noodles",
                          "brand": "康师傅",
                          "barcode": null,
                          "coverImageUrl": null,
                          "auditStatus": "approved"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val recognizedText = "康师傅红烧牛肉面".repeat(17) + "\n牛肉面"

        val result = repository.searchByRecognizedText(recognizedText)

        assertTrue(result is FoodOcrSearchResult.Success)
        val success = result as FoodOcrSearchResult.Success
        assertEquals("牛肉面", success.matchedQuery)
        assertEquals("牛肉面", success.result.items.first().name)
        assertEquals(1, server.requestCount)
        assertEquals("/api/foods/search?q=%E7%89%9B%E8%82%89%E9%9D%A2", server.takeRequest().path)
    }

    @Test
    fun `searchByRecognizedText returns no match when all queries miss`() = runTest {
        repeat(4) {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"qualitySignal":"weak","items":[]}"""),
            )
        }

        val result = repository.searchByRecognizedText("Lays Cucumber")

        assertTrue(result is FoodOcrSearchResult.NoMatch)
    }

    @Test
    fun `searchByServerOcr returns candidates when backend succeeds`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "recognizedText": "Lays Cucumber Chips",
                      "attemptedQueries": ["Lays Cucumber Chips", "LaysCucumberChips"],
                      "matchedQuery": "LaysCucumberChips",
                      "qualitySignal": "strong",
                      "items": [
                        {
                          "id": 1,
                          "name": "LaysCucumberChips",
                          "itemType": "packaged_product",
                          "category": "snack",
                          "subcategory": "chips",
                          "brand": "Lays",
                          "barcode": "6900000000011",
                          "coverImageUrl": null,
                          "auditStatus": "approved"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.searchByServerOcr(
            imageBytes = "png-content".toByteArray(),
            fileName = "chips.png",
            contentType = "image/png",
            clientRecognizedText = "Lays Cucumber Chips",
        )

        assertTrue(result is FoodOcrSearchResult.Success)
        val success = result as FoodOcrSearchResult.Success
        assertEquals("LaysCucumberChips", success.matchedQuery)
        assertEquals("LaysCucumberChips", success.result.items.first().name)
        val request = server.takeRequest()
        assertEquals("/api/recognition/ocr", request.path)
        val body = Buffer().write(request.body.readByteArray()).readUtf8()
        assertTrue(body.contains("name=\"clientRecognizedText\""))
        assertTrue(body.contains("Lays Cucumber Chips"))
        assertTrue(body.contains("filename=\"chips.png\""))
    }

    @Test
    fun `recommendRelatedFoods returns related items when backend succeeds`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "qualitySignal": "related",
                      "items": [
                        {
                          "id": 2,
                          "name": "乐事番茄味薯片",
                          "itemType": "packaged_product",
                          "category": "snack",
                          "subcategory": "chips",
                          "brand": "乐事",
                          "barcode": "6900000000022",
                          "coverImageUrl": null,
                          "auditStatus": "approved"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.recommendRelatedFoods(1L)

        assertTrue(result is FoodSearchResult.Success)
        val success = result as FoodSearchResult.Success
        assertEquals("related", success.qualitySignal)
        assertEquals("乐事番茄味薯片", success.items.first().name)
        assertEquals("/api/foods/1/related?limit=5", server.takeRequest().path)
    }
}
