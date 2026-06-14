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
    fun `lookupByBarcode returns item when backend succeeds`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
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
                    """.trimIndent(),
                ),
        )

        val result = repository.lookupByBarcode("6900000000011")

        assertTrue(result is FoodBarcodeLookupResult.Success)
        assertEquals("Lays Cucumber Chips", (result as FoodBarcodeLookupResult.Success).item.name)
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
    fun `searchByImageRecognition uploads image and returns candidates when backend succeeds`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "objectKey": "images/demo.png",
                      "resourceUrl": "https://snk.qiuxinmin.cn/uploads/images/demo.png",
                      "contentType": "image/png",
                      "size": 11
                    }
                    """.trimIndent(),
                ),
        )
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "id": 18,
                      "userId": 2,
                      "inputImageUrl": "https://snk.qiuxinmin.cn/uploads/images/demo.png",
                      "status": "completed",
                      "topCandidates": [
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
                      ],
                      "selectedFoodItemId": 1,
                      "confidence": "0.8500",
                      "createdAt": "2026-06-14T12:00:00Z",
                      "finishedAt": "2026-06-14T12:00:01Z",
                      "statusReason": null
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.searchByImageRecognition(
            userId = 2L,
            imageBytes = "png-content".toByteArray(),
            fileName = "chips.png",
            contentType = "image/png",
        )

        assertTrue(result is FoodImageRecognitionResult.Success)
        val success = result as FoodImageRecognitionResult.Success
        assertEquals("0.8500", success.confidence)
        assertEquals("Lays Cucumber Chips", success.result.items.first().name)

        val uploadRequest = server.takeRequest()
        assertEquals("/api/upload/image", uploadRequest.path)
        val createTaskRequest = server.takeRequest()
        assertEquals("/api/recognition/tasks", createTaskRequest.path)
        val body = Buffer().write(createTaskRequest.body.readByteArray()).readUtf8()
        assertTrue(body.contains("\"userId\":2"))
        assertTrue(body.contains("\"inputImageUrl\":\"https://snk.qiuxinmin.cn/uploads/images/demo.png\""))
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
