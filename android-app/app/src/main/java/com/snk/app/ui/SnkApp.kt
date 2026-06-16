package com.snk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.snk.app.SnkApplication
import com.snk.app.data.auth.AnonymousSessionResult
import com.snk.app.data.food.FoodReportResult
import com.snk.app.data.food.FoodSearchItem
import kotlinx.coroutines.launch

private sealed class SnkDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : SnkDestination("home", "首页", Icons.Outlined.Home)
    data object Search : SnkDestination("search", "搜索", Icons.Outlined.ManageSearch)
    data object Drafts : SnkDestination("drafts", "草稿", Icons.Outlined.BookmarkBorder)
    data object Profile : SnkDestination("profile", "我的", Icons.Outlined.Person)
}

private val destinations = listOf(
    SnkDestination.Home,
    SnkDestination.Search,
    SnkDestination.Drafts,
    SnkDestination.Profile,
)

private fun SessionUiState.userIdOrNull(): Long? = when (this) {
    is SessionUiState.Remote -> session.userId
    is SessionUiState.Cached -> session.userId
    SessionUiState.Loading -> null
    is SessionUiState.Failure -> null
}

@Composable
fun SnkApp() {
    val application = LocalContext.current.applicationContext as SnkApplication
    var retryToken by remember { mutableIntStateOf(0) }
    var selectedFood: FoodSearchItem? by remember { mutableStateOf(null) }
    var selectedSourceType by remember { mutableStateOf("text_search") }
    var manualCreateSeedName by remember { mutableStateOf("") }
    var manualCreateSeedBarcode by remember { mutableStateOf("") }
    var candidateConfirmationState by remember { mutableStateOf<CandidateConfirmationState?>(null) }
    var candidateReportMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val sessionState by produceState<SessionUiState>(
        initialValue = SessionUiState.Loading,
        key1 = retryToken,
    ) {
        value = when (val result = application.container.anonymousSessionRepository.ensureSession()) {
            is AnonymousSessionResult.Remote -> SessionUiState.Remote(result.session)
            is AnonymousSessionResult.Cached -> SessionUiState.Cached(result.session, result.reason)
            is AnonymousSessionResult.Failure -> SessionUiState.Failure(result.reason)
        }
    }
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != "record_create" &&
        currentRoute != "candidate_confirm" &&
        currentRoute != "barcode_scan" &&
        currentRoute != "ocr_recognition" &&
        currentRoute != "manual_food_create"

    fun openRecordCreate(item: FoodSearchItem, sourceType: String) {
        selectedFood = item
        selectedSourceType = sourceType
        navController.navigate("record_create")
    }

    fun openManualCreate(seedName: String) {
        manualCreateSeedName = seedName
        manualCreateSeedBarcode = ""
        navController.navigate("manual_food_create")
    }

    fun openManualCreateFromBarcode(barcode: String) {
        manualCreateSeedName = ""
        manualCreateSeedBarcode = barcode
        navController.navigate("manual_food_create")
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF4E8),
                            Color(0xFFFFFBF7),
                            Color(0xFFF7F4EF),
                        ),
                    ),
                )
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = SnkDestination.Home.route,
            ) {
                composable(SnkDestination.Home.route) {
                    HomeScreen()
                }
                composable(SnkDestination.Search.route) {
                    SearchScreen(
                        sessionState = sessionState,
                        onCreateRecord = { item ->
                            openRecordCreate(item, "text_search")
                        },
                        onOpenManualCreate = ::openManualCreate,
                    )
                }
                composable(SnkDestination.Drafts.route) {
                    DraftsScreen()
                }
                composable(SnkDestination.Profile.route) {
                    ProfileScreen(
                        sessionState = sessionState,
                        onRetry = { retryToken++ },
                    )
                }
                composable("record_create") {
                    val food = selectedFood
                    if (food == null) {
                        SearchScreen(
                            sessionState = sessionState,
                            onCreateRecord = { item ->
                                openRecordCreate(item, "text_search")
                            },
                            onOpenManualCreate = ::openManualCreate,
                        )
                    } else {
                        RecordCreateScreen(
                            selectedFood = food,
                            sourceType = selectedSourceType,
                            sessionState = sessionState,
                            submissionCoordinator = application.container.foodRecordSubmissionCoordinator,
                            onSwitchRecommendedFood = { item ->
                                selectedFood = item
                                selectedSourceType = "related_recommendation"
                            },
                            onBackToSearch = {
                                navController.popBackStack()
                            },
                            onOpenDrafts = {
                                navController.popBackStack()
                                navController.navigate(SnkDestination.Drafts.route)
                            },
                        )
                    }
                }
                composable("candidate_confirm") {
                    val state = candidateConfirmationState
                    if (state == null) {
                        SearchScreen(
                            sessionState = sessionState,
                            onCreateRecord = { item ->
                                openRecordCreate(item, "text_search")
                            },
                            onOpenManualCreate = ::openManualCreate,
                        )
                    } else {
                        CandidateConfirmationScreen(
                            state = state,
                            onSelectCandidate = { item ->
                                selectedFood = item
                                selectedSourceType = state.sourceType
                                navController.navigate("record_create") {
                                    popUpTo("candidate_confirm") {
                                        inclusive = true
                                    }
                                }
                            },
                            onReportCandidate = { item ->
                                val userId = sessionState.userIdOrNull()
                                if (userId == null) {
                                    candidateReportMessage = "游客身份尚未初始化，暂时无法提交纠错。"
                                } else {
                                    coroutineScope.launch {
                                        candidateReportMessage = when (
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
                            onOpenManualCreate = ::openManualCreate,
                            onBack = {
                                navController.popBackStack()
                            },
                            reportMessage = candidateReportMessage,
                        )
                    }
                }
                composable("barcode_scan") {
                    BarcodeScanScreen(
                        onFoodMatched = { item ->
                            candidateConfirmationState = CandidateConfirmationState(
                                sourceLabel = "条码命中确认",
                                title = "已根据条形码精确命中条目",
                                description = "条码命中优先级最高，确认名称和分类无误后再继续记录。",
                                items = listOf(item),
                                qualitySignal = "strong",
                                sourceType = "image_search",
                                matchedQuery = item.barcode,
                            )
                            navController.navigate("candidate_confirm") {
                                popUpTo("barcode_scan") {
                                    inclusive = true
                                }
                            }
                        },
                        onOpenManualCreate = ::openManualCreateFromBarcode,
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }
                composable("ocr_recognition") {
                    OcrRecognitionScreen(
                        sessionUserId = sessionState.userIdOrNull(),
                        onCandidatesMatched = { state ->
                            candidateConfirmationState = state
                            navController.navigate("candidate_confirm") {
                                popUpTo("ocr_recognition") {
                                    inclusive = true
                                }
                            }
                        },
                        onOpenManualCreate = ::openManualCreate,
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }
                composable("manual_food_create") {
                    ManualFoodCreateScreen(
                        sessionState = sessionState,
                        initialName = manualCreateSeedName,
                        initialBarcode = manualCreateSeedBarcode,
                        onFoodCreated = { item ->
                            selectedFood = item
                            selectedSourceType = "manual"
                            navController.navigate("record_create") {
                                popUpTo("manual_food_create") {
                                    inclusive = true
                                }
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}
