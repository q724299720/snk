package com.snk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.snk.app.BuildConfig
import com.snk.app.SnkApplication
import com.snk.app.data.draft.DraftSyncStatus
import com.snk.app.data.draft.FoodRecordDraft
import com.snk.app.data.food.FoodReportResult
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.food.FoodSearchResult
import com.snk.app.data.record.FoodRecordComment
import com.snk.app.data.record.FoodRecordCommentCreateResult
import com.snk.app.data.record.FoodRecordCommentsResult
import com.snk.app.data.record.FoodRecordHistoryItem
import com.snk.app.data.record.FoodRecordHistoryResult
import com.snk.app.data.record.FoodRecordLikeResult
import com.snk.app.data.record.toFoodSearchItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    sessionState: SessionUiState,
    onCreateRecord: (FoodSearchItem) -> Unit,
    onOpenManualCreate: (String) -> Unit,
    onOpenOcrRecognition: () -> Unit,
    externalQuerySeed: String? = null,
    externalSuggestedQueries: List<String> = emptyList(),
    onExternalQueryConsumed: () -> Unit = {},
) {
    val application = LocalContext.current.applicationContext as SnkApplication
    val coroutineScope = rememberCoroutineScope()
    val sessionUserId = sessionState.userIdOrNull()
    var recentQueries by remember { mutableStateOf<List<String>>(emptyList()) }
    val recentRecordState by produceState<FoodRecordHistoryResult?>(
        initialValue = null,
        key1 = sessionUserId,
    ) {
        value = if (sessionUserId == null) {
            null
        } else {
            application.container.foodRecordRepository.listRecentRecords(sessionUserId, HOME_RECENT_RECORD_LIMIT)
        }
    }
    val publicRecordState by produceState<FoodRecordHistoryResult?>(
        initialValue = null,
    ) {
        value = application.container.foodRecordRepository.listPublicRecords(HOME_PUBLIC_RECORD_LIMIT)
    }
    val drafts by application.container.draftRecordRepository
        .observeDrafts()
        .collectAsState(initial = emptyList())
    val pendingDrafts = remember(drafts) {
        drafts.filter { it.syncStatus != DraftSyncStatus.SYNCED }
    }
    var query by remember { mutableStateOf("") }
    var searchState by remember { mutableStateOf<FoodSearchResult?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf<String?>(null) }
    var ocrSuggestedQueries by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        recentQueries = application.container.recentSearchStore.getRecentQueries()
    }

    LaunchedEffect(externalQuerySeed, externalSuggestedQueries) {
        val seed = externalQuerySeed?.trim().orEmpty()
        if (seed.isNotBlank() || externalSuggestedQueries.isNotEmpty()) {
            if (seed.isNotBlank()) {
                query = seed
            }
            ocrSuggestedQueries = externalSuggestedQueries.ifEmpty {
                if (seed.isNotBlank()) listOf(seed) else emptyList()
            }
            onExternalQueryConsumed()
        }
    }

    LaunchedEffect(query, sessionUserId) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            isSearching = false
            searchState = null
            return@LaunchedEffect
        }
        delay(300)
        isSearching = true
        val result = application.container.foodSearchRepository.search(normalizedQuery, sessionUserId)
        searchState = result
        isSearching = false
        if (result is FoodSearchResult.Success) {
            recentQueries = application.container.recentSearchStore.rememberQuery(normalizedQuery)
        }
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "SNK 搜索与记录",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF2B1E18),
            )
        }
        item {
            Text(
                text = "输入名称，或者先从图片里提取文字，再用图片和评分确认条目。",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF5B4A42),
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("输入食物名称、品牌或口味") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onOpenOcrRecognition,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("从图片提取文字")
                }
            }
        }
        if (recentQueries.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8EEE2)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "最近搜索",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        application.container.recentSearchStore.clearRecentQueries()
                                        recentQueries = emptyList()
                                    }
                                },
                            ) {
                                Text("清空")
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            recentQueries.forEach { item ->
                                AssistChip(
                                    onClick = { query = item },
                                    label = { Text(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (ocrSuggestedQueries.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "OCR 识别词组",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ocrSuggestedQueries.forEach { suggestion ->
                                AssistChip(
                                    onClick = { query = suggestion },
                                    label = { Text(suggestion) },
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF1E6)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "服务端连接",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Base URL: ${BuildConfig.API_BASE_URL}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                    Text(
                        text = when (sessionState) {
                            SessionUiState.Loading -> "游客身份初始化中..."
                            is SessionUiState.Remote -> "当前游客 user_id: ${sessionState.session.userId}"
                            is SessionUiState.Cached -> "离线沿用缓存游客 user_id: ${sessionState.session.userId}"
                            is SessionUiState.Failure -> sessionState.reason
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                }
            }
        }
        item {
            FoodSearchResultsCard(
                searchState = searchState,
                isSearching = isSearching,
                emptyHint = "输入名称后即可自动联想已审核的基础食物条目。",
                onCreateRecord = onCreateRecord,
                onReportItem = { item ->
                    val userId = sessionUserId
                    if (userId == null) {
                        reportMessage = "游客身份尚未初始化，暂时无法提交纠错。"
                    } else {
                        coroutineScope.launch {
                            reportMessage = when (
                                val result = application.container.foodSearchRepository.reportFoodItem(
                                    userId = userId,
                                    foodItemId = item.id,
                                )
                            ) {
                                is FoodReportResult.Success -> "已提交纠错信号，当前 reportCount = ${result.reportCount}"
                                is FoodReportResult.Failure -> result.message
                            }
                        }
                    }
                },
                noResultActionLabel = if (query.isNotBlank()) "没有找到？手动创建" else null,
                onNoResultAction = if (query.isNotBlank()) {
                    { onOpenManualCreate(query.trim()) }
                } else {
                    null
                },
            )
        }
        if (pendingDrafts.isNotEmpty()) {
            item {
                Text(
                    text = "待上传草稿 / 失败重试",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8A2E1C),
                )
            }
            items(pendingDrafts, key = { "draft-${it.id}" }) { draft ->
                DraftItemCard(
                    draft = draft,
                    onRetry = {
                        coroutineScope.launch {
                            application.container.draftRecordRepository.requestRetry(draft.id)
                            application.container.scheduleDraftRetry(draft.id)
                        }
                    },
                )
            }
        }
        item {
            Text(
                text = "公开分享",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B1E18),
            )
        }
        when (val publicHistory = publicRecordState) {
            null -> item {
                Text(
                    text = "正在加载公开记录...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
            }

            is FoodRecordHistoryResult.Failure -> item {
                Text(
                    text = publicHistory.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A2E1C),
                )
            }

            is FoodRecordHistoryResult.Success -> {
                if (publicHistory.items.isEmpty()) {
                    item {
                        Text(
                            text = "暂无公开记录。保存记录时打开公开开关后会出现在这里。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                    }
                } else {
                    items(publicHistory.items, key = { "public-${it.id}" }) { record ->
                        RecentRecordCard(
                            record = record,
                            onReuseFood = {
                                onCreateRecord(record.toFoodSearchItem())
                            },
                            sessionUserId = sessionUserId,
                            onLoadComments = { recordId ->
                                application.container.foodRecordRepository.listRecordComments(
                                    recordId = recordId,
                                    limit = 3,
                                )
                            },
                            onSubmitComment = { recordId, content ->
                                val userId = sessionUserId
                                if (userId == null) {
                                    FoodRecordCommentCreateResult.Failure("游客身份尚未初始化，暂时无法评论。")
                                } else {
                                    application.container.foodRecordRepository.createRecordComment(
                                        recordId = recordId,
                                        userId = userId,
                                        content = content,
                                    )
                                }
                            },
                            onLikeRecord = { recordId ->
                                application.container.foodRecordRepository.likeRecord(recordId)
                            },
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = "最近记录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B1E18),
            )
        }
        when (val history = recentRecordState) {
            null -> item {
                Text(
                    text = "正在加载最近记录...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
            }

            is FoodRecordHistoryResult.Failure -> item {
                Text(
                    text = history.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A2E1C),
                )
            }

            is FoodRecordHistoryResult.Success -> {
                if (history.items.isEmpty()) {
                    item {
                        Text(
                            text = "还没有历史记录，先从上面的搜索开始记一笔。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5B4A42),
                        )
                    }
                } else {
                    items(history.items, key = { it.id }) { record ->
                        RecentRecordCard(
                            record = record,
                            onReuseFood = {
                                onCreateRecord(record.toFoodSearchItem())
                            },
                        )
                    }
                }
            }
        }
        if (reportMessage != null) {
            item {
                Text(
                    text = reportMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A5A44),
                )
            }
        }
    }
}

@Composable
private fun RecentRecordCard(
    record: FoodRecordHistoryItem,
    onReuseFood: () -> Unit,
    sessionUserId: Long? = null,
    onLoadComments: (suspend (Long) -> FoodRecordCommentsResult)? = null,
    onSubmitComment: (suspend (Long, String) -> FoodRecordCommentCreateResult)? = null,
    onLikeRecord: (suspend (Long) -> FoodRecordLikeResult)? = null,
) {
    val displayImageUrl = record.images.firstOrNull()?.thumbnailUrl
        ?: record.images.firstOrNull()?.imageUrl
        ?: record.foodCoverImageUrl
    val coroutineScope = rememberCoroutineScope()
    var comments by remember(record.id) { mutableStateOf<List<FoodRecordComment>>(emptyList()) }
    var commentMessage by remember(record.id) { mutableStateOf<String?>(null) }
    var commentInput by remember(record.id) { mutableStateOf("") }
    var likeState by remember(record.id, record.likeCount) {
        mutableStateOf(RecordLikeUiState(likeCount = record.likeCount))
    }
    var isLiking by remember(record.id) { mutableStateOf(false) }
    val commentsEnabled = record.isPublic && onLoadComments != null && onSubmitComment != null
    val likeEnabled = onLikeRecord != null

    LaunchedEffect(record.id, commentsEnabled) {
        if (!commentsEnabled) {
            return@LaunchedEffect
        }
        val loader = onLoadComments ?: return@LaunchedEffect
        when (val result = loader(record.id)) {
            is FoodRecordCommentsResult.Success -> {
                comments = result.comments
                commentMessage = null
            }

            is FoodRecordCommentsResult.Failure -> {
                comments = emptyList()
                commentMessage = result.message
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F2)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!displayImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = displayImageUrl,
                        contentDescription = record.foodName,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF2E3D3)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无图片",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A5A44),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = record.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "评分 ${record.rating} / 5",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8A5A44),
                    )
                    Text(
                        text = buildString {
                            append(record.foodCategory)
                            record.foodSubcategory?.takeIf { it.isNotBlank() }?.let {
                                append(" / ")
                                append(it)
                            }
                            record.foodBrand?.takeIf { it.isNotBlank() }?.let {
                                append(" / ")
                                append(it)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B4A42),
                    )
                    Text(
                        text = formatRecordTime(record.recordTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A6A61),
                    )
                }
            }
            record.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "点赞 ${likeState.likeCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A5A44),
                    )
                    likeState.message?.takeIf { it.isNotBlank() }?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A5A44),
                        )
                    }
                }
                if (likeEnabled) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val liker = onLikeRecord ?: return@launch
                                isLiking = true
                                likeState = likeState.afterLikeResult(liker(record.id))
                                isLiking = false
                            }
                        },
                        enabled = !isLiking,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (isLiking) "提交中" else "点赞")
                    }
                }
            }
            Button(
                onClick = onReuseFood,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("再记一笔")
            }
            if (commentsEnabled) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "评论",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B4A42),
                    )
                    if (comments.isEmpty()) {
                        Text(
                            text = commentMessage ?: "暂无评论，来写第一条。",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A5A44),
                        )
                    } else {
                        comments.forEach { comment ->
                            Text(
                                text = comment.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5B4A42),
                            )
                        }
                    }
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("写一句评论") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = commentMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A5A44),
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val userId = sessionUserId
                                    if (userId == null) {
                                        commentMessage = "游客身份尚未初始化，暂时无法评论。"
                                        return@launch
                                    }
                                    val content = commentInput.trim()
                                    val submitter = onSubmitComment ?: run {
                                        commentMessage = "评论入口暂不可用。"
                                        return@launch
                                    }
                                    when (val result = submitter(record.id, content)) {
                                        is FoodRecordCommentCreateResult.Success -> {
                                            comments = listOf(result.comment) + comments
                                            commentInput = ""
                                            commentMessage = "评论已发布。"
                                        }

                                        is FoodRecordCommentCreateResult.Failure -> {
                                            commentMessage = result.message
                                        }
                                    }
                                }
                            },
                            enabled = commentInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("发布")
                        }
                    }
                }
            }
        }
    }
}

/** 首页最近记录默认展示条数，对齐 PRD「首页最近记录默认展示最近 5 条」。 */
private const val HOME_RECENT_RECORD_LIMIT = 5
private const val HOME_PUBLIC_RECORD_LIMIT = 5

private fun formatRecordTime(recordTime: String): String {
    return recordTime.replace("T", " ").removeSuffix("Z").take(16)
}
