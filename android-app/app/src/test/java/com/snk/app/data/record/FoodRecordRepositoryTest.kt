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
import org.junit.Assert.assertSame
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
    fun `createRecord sends images and returns success when backend accepts request`() = runTest {
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
                      "comment": "good",
                      "likeCount": 0,
                      "recordTime": "2026-06-13T23:40:00Z",
                      "createdAt": "2026-06-13T23:40:00Z",
                      "images": [
                        {
                          "imageUrl": "https://snk.qiuxinmin.cn/uploads/records/noodle.jpg",
                          "thumbnailUrl": "https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.createRecord(
            userId = 100,
            foodItemId = 200,
            rating = 5,
            comment = "good",
            sourceType = "text_search",
            images = listOf(
                FoodRecordImageAttachment(
                    imageUrl = "https://snk.qiuxinmin.cn/uploads/records/noodle.jpg",
                    thumbnailUrl = "https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg",
                ),
            ),
        )

        assertTrue(result is FoodRecordCreateResult.Success)
        assertEquals(55L, (result as FoodRecordCreateResult.Success).recordId)
        assertEquals(0, result.likeCount)
        val requestBody = server.takeRequest().body.readUtf8()
        assertTrue(requestBody.contains("\"images\""))
        assertTrue(requestBody.contains("https://snk.qiuxinmin.cn/uploads/records/noodle.jpg"))
    }

    @Test
    fun `uploadRecordImage returns uploaded image urls`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "resourceUrl": "https://snk.qiuxinmin.cn/uploads/records/noodle.jpg",
                      "thumbnailUrl": "https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.uploadRecordImage(
            imageBytes = "image-content".toByteArray(),
            fileName = "noodle.jpg",
            contentType = "image/jpeg",
        )

        assertTrue(result is RecordImageUploadResult.Success)
        assertEquals(
            "https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg",
            (result as RecordImageUploadResult.Success).image.thumbnailUrl,
        )
        assertTrue(server.takeRequest().path.orEmpty().contains("/api/upload/image"))
    }

    @Test
    fun `likeRecord returns updated like count`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "id": 55,
                      "userId": 100,
                      "foodItemId": 200,
                      "sourceType": "text_search",
                      "isPublic": false,
                      "rating": 5,
                      "comment": "good",
                      "likeCount": 4,
                      "recordTime": "2026-06-13T23:40:00Z",
                      "createdAt": "2026-06-13T23:40:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.likeRecord(55L)

        assertTrue(result is FoodRecordLikeResult.Success)
        assertEquals(4, (result as FoodRecordLikeResult.Success).likeCount)
    }

    @Test
    fun `createRecord returns network failure when backend is unreachable`() = runTest {
        server.shutdown()

        val result = repository.createRecord(
            userId = 100,
            foodItemId = 200,
            rating = 5,
            comment = "good",
            sourceType = "text_search",
            images = emptyList(),
        )

        assertTrue(result is FoodRecordCreateResult.Failure)
        assertSame(
            FoodRecordCreateFailureReason.NETWORK,
            (result as FoodRecordCreateResult.Failure).reason,
        )
    }
}
