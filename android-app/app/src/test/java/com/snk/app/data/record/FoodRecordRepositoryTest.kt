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
    fun `createRecord can send public visibility when user chooses to share`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "id": 57,
                      "userId": 100,
                      "foodItemId": 200,
                      "sourceType": "text_search",
                      "isPublic": true,
                      "rating": 4,
                      "comment": "share this",
                      "likeCount": 0,
                      "recordTime": "2026-06-13T23:40:00Z",
                      "createdAt": "2026-06-13T23:40:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.createRecord(
            userId = 100,
            foodItemId = 200,
            rating = 4,
            comment = "share this",
            sourceType = "text_search",
            isPublic = true,
            images = emptyList(),
        )

        assertTrue(result is FoodRecordCreateResult.Success)
        val requestBody = server.takeRequest().body.readUtf8()
        assertTrue(requestBody.contains("\"isPublic\":true"))
    }

    @Test
    fun `createRecord rejects overlong comment before calling backend`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody("""{"id":55,"userId":100,"foodItemId":200,"sourceType":"text_search","isPublic":false,"rating":5,"likeCount":0,"recordTime":"2026-06-13T23:40:00Z","createdAt":"2026-06-13T23:40:00Z"}"""),
        )

        val result = repository.createRecord(
            userId = 100,
            foodItemId = 200,
            rating = 5,
            comment = "a".repeat(501),
            sourceType = "text_search",
            images = emptyList(),
        )

        assertTrue(result is FoodRecordCreateResult.Failure)
        val failure = result as FoodRecordCreateResult.Failure
        assertSame(FoodRecordCreateFailureReason.UNKNOWN, failure.reason)
        assertEquals("备注最长支持 500 个字符，请缩短后再保存。", failure.message)
        assertEquals(0, server.requestCount)
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
    fun `listPublicRecords returns shared records with images`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "id": 56,
                        "userId": 101,
                        "foodItemId": 201,
                        "foodName": "Kangshifu Beef Noodles",
                        "foodItemType": "packaged_product",
                        "foodCategory": "instant_food",
                        "foodSubcategory": "noodles",
                        "foodBrand": "Kangshifu",
                        "foodCoverImageUrl": "https://snk.qiuxinmin.cn/images/noodle.png",
                        "sourceType": "text_search",
                        "isPublic": true,
                        "rating": 4,
                        "comment": "share this",
                        "likeCount": 7,
                        "recordTime": "2026-06-13T23:40:00Z",
                        "createdAt": "2026-06-13T23:40:00Z",
                        "images": [
                          {
                            "imageUrl": "https://snk.qiuxinmin.cn/uploads/records/noodle.jpg",
                            "thumbnailUrl": "https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"
                          }
                        ]
                      }
                    ]
                    """.trimIndent(),
                ),
        )

        val result = repository.listPublicRecords(limit = 5)

        assertTrue(result is FoodRecordHistoryResult.Success)
        val items = (result as FoodRecordHistoryResult.Success).items
        assertEquals(1, items.size)
        assertEquals("Kangshifu Beef Noodles", items.first().foodName)
        assertTrue(items.first().isPublic)
        assertEquals(
            "https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg",
            items.first().images.first().thumbnailUrl,
        )
        assertEquals("/api/records/public?limit=5", server.takeRequest().path)
    }

    @Test
    fun `listRecordComments returns comments for a shared record`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "id": 9,
                        "recordId": 56,
                        "userId": 100,
                        "content": "看起来不错",
                        "createdAt": "2026-06-21T12:00:00Z"
                      }
                    ]
                    """.trimIndent(),
                ),
        )

        val result = repository.listRecordComments(recordId = 56, limit = 3)

        assertTrue(result is FoodRecordCommentsResult.Success)
        val comments = (result as FoodRecordCommentsResult.Success).comments
        assertEquals(1, comments.size)
        assertEquals("看起来不错", comments.first().content)
        assertEquals("/api/records/56/comments?limit=3", server.takeRequest().path)
    }

    @Test
    fun `createRecordComment posts current user comment`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setResponseCode(201)
                .setBody(
                    """
                    {
                      "id": 9,
                      "recordId": 56,
                      "userId": 100,
                      "content": "看起来不错",
                      "createdAt": "2026-06-21T12:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.createRecordComment(
            recordId = 56,
            userId = 100,
            content = "  看起来不错  ",
        )

        assertTrue(result is FoodRecordCommentCreateResult.Success)
        assertEquals("看起来不错", (result as FoodRecordCommentCreateResult.Success).comment.content)
        val request = server.takeRequest()
        assertEquals("/api/records/56/comments", request.path)
        assertTrue(request.body.readUtf8().contains("\"content\":\"看起来不错\""))
    }

    @Test
    fun `createRecordComment rejects overlong comment before backend request`() = runTest {
        val result = repository.createRecordComment(
            recordId = 56,
            userId = 100,
            content = "a".repeat(501),
        )

        assertEquals(
            FoodRecordCommentCreateResult.Failure("评论最长支持 500 个字符，请缩短后再发送。"),
            result,
        )
        assertEquals(0, server.requestCount)
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
