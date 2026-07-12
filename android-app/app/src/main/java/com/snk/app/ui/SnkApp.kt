package com.snk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.snk.app.ui.theme.ChiliRed
import com.snk.app.ui.theme.Paper
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.snk.app.SnkApplication
import com.snk.app.data.auth.AuthenticatedAccount
import com.snk.app.data.food.FoodSearchItem
import com.snk.app.data.record.FoodRecordHistoryItem
import com.snk.app.data.record.FoodRecordDetailResult
import com.snk.app.ui.auth.AuthUiState
import com.snk.app.ui.auth.AuthViewModel
import com.snk.app.ui.auth.LoginScreen
import com.snk.app.ui.auth.PendingApprovalScreen
import com.snk.app.ui.auth.RegisterScreen
import com.snk.app.ui.auth.ChangePasswordScreen
import com.snk.app.ui.auth.LegacyClaimDialog
import com.snk.app.data.auth.LegacyClaimAvailability
import kotlinx.coroutines.launch

private sealed class SnkDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Search : SnkDestination("search", "首页", Icons.Outlined.Home)
    data object Gallery : SnkDestination("gallery", "记录", Icons.Outlined.Collections)
    data object Discover : SnkDestination("discover", "发现", Icons.Outlined.Explore)
    data object Profile : SnkDestination("profile", "我的", Icons.Outlined.Person)
}

private val destinations = listOf(
    SnkDestination.Search,
    SnkDestination.Gallery,
    SnkDestination.Discover,
    SnkDestination.Profile,
)

internal data class TopLevelNavigationStatePolicy(
    val saveState: Boolean,
    val restoreState: Boolean,
)

internal val flatTopLevelNavigationStatePolicy = TopLevelNavigationStatePolicy(
    saveState = false,
    restoreState = false,
)

@Composable
fun SnkApp() {
    val application = LocalContext.current.applicationContext as SnkApplication
    val authViewModel: AuthViewModel = viewModel {
        AuthViewModel(application.container.authRepository, application.container.authenticatedSessionManager)
    }
    val authState by authViewModel.state.collectAsState()
    var showRegistration by remember { mutableStateOf(false) }
    LaunchedEffect(authState) {
        if (authState is AuthUiState.SignedOut && (authState as AuthUiState.SignedOut).message != null) {
            showRegistration = false
        }
    }
    when (val state = authState) {
        AuthUiState.Restoring -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is AuthUiState.SignedOut -> if (showRegistration) {
            RegisterScreen(onRegister = authViewModel::register, onBack = { showRegistration = false })
        } else {
            LoginScreen(
                onLogin = authViewModel::login,
                onOpenRegistration = { showRegistration = true },
                message = state.message,
            )
        }
        is AuthUiState.Pending -> PendingApprovalScreen(
            onCheckNow = authViewModel::checkApproval,
            onBackToLogin = { authViewModel.backToLogin() },
        )
        AuthUiState.Rejected -> AuthStatusScreen("注册申请已被拒绝", "请联系主账户确认后重新注册。") {
            authViewModel.backToLogin()
        }
        AuthUiState.Disabled -> AuthStatusScreen("账号已停用", "请联系主账户恢复账号。") {
            authViewModel.backToLogin()
        }
        is AuthUiState.MustChangePassword -> ChangePasswordScreen(onSubmit = authViewModel::changePassword)
        is AuthUiState.Authenticated -> AuthenticatedSnkApp(state.account, authViewModel)
    }
}

@Composable
private fun AuthStatusScreen(title: String, message: String, onBack: (() -> Unit)?) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Text(message)
        if (onBack != null) Button(onBack, modifier = Modifier.fillMaxWidth()) { Text("返回登录") }
    }
}

@Composable
private fun AuthenticatedSnkApp(account: AuthenticatedAccount, authViewModel: AuthViewModel) {
    val application = LocalContext.current.applicationContext as SnkApplication
    var selectedFood: FoodSearchItem? by remember { mutableStateOf(null) }
    var selectedEditRecord: FoodRecordHistoryItem? by remember { mutableStateOf(null) }
    var recordRefreshToken by remember { mutableIntStateOf(0) }
    var selectedSourceType by remember { mutableStateOf("text_search") }
    var manualCreateSeedName by remember { mutableStateOf("") }
    var searchQuerySeed by remember { mutableStateOf<String?>(null) }
    var searchSuggestedQueries by remember { mutableStateOf<List<String>>(emptyList()) }
    var showLegacyClaim by remember(account.userId) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(account.userId) {
        showLegacyClaim = application.container.legacyClaimCoordinator.availability() == LegacyClaimAvailability.AVAILABLE
    }
    val sessionState = remember(account.userId) { SessionUiState.Authenticated(account.userId) }
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != "record_create" &&
        currentRoute != "record/{recordId}/edit" &&
        currentRoute != "ocr_recognition" &&
        currentRoute != "manual_food_create"
        && currentRoute != "change_password"

    fun openRecordCreate(item: FoodSearchItem, sourceType: String) {
        selectedFood = item
        selectedSourceType = sourceType
        navController.navigate("record_create")
    }

    fun openManualCreate(seedName: String) {
        manualCreateSeedName = seedName
        navController.navigate("manual_food_create")
    }

    fun openRecordEdit(record: FoodRecordHistoryItem) {
        selectedEditRecord = record
        navController.navigate("record/${record.id}/edit")
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Paper,
                    tonalElevation = 3.dp,
                ) {
                    destinations.take(2).forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = flatTopLevelNavigationStatePolicy.saveState
                                    }
                                    launchSingleTop = true
                                    restoreState = flatTopLevelNavigationStatePolicy.restoreState
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                    FloatingActionButton(
                        onClick = { openManualCreate("") },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        containerColor = ChiliRed,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "记录美食")
                    }
                    destinations.drop(2).forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = flatTopLevelNavigationStatePolicy.saveState
                                    }
                                    launchSingleTop = true
                                    restoreState = flatTopLevelNavigationStatePolicy.restoreState
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
                .background(Paper)
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = SnkDestination.Search.route,
            ) {
                composable(SnkDestination.Search.route) {
                    SearchScreen(
                        sessionState = sessionState,
                        username = account.username,
                        onCreateRecord = { item ->
                            openRecordCreate(item, "text_search")
                        },
                        onEditRecord = { record ->
                            openRecordEdit(record)
                        },
                        onOpenManualCreate = ::openManualCreate,
                        onOpenOcrRecognition = {
                            navController.navigate("ocr_recognition")
                        },
                        externalQuerySeed = searchQuerySeed,
                        externalSuggestedQueries = searchSuggestedQueries,
                        recentRefreshToken = recordRefreshToken,
                        onExternalQueryConsumed = {
                            searchQuerySeed = null
                            searchSuggestedQueries = emptyList()
                        },
                    )
                }
                composable(SnkDestination.Gallery.route) {
                    GalleryScreen(
                        sessionUserId = sessionState.userIdOrNull(),
                        refreshToken = recordRefreshToken,
                        onEditRecord = ::openRecordEdit,
                    )
                }
                composable(SnkDestination.Discover.route) {
                    DiscoverScreen()
                }
                composable(SnkDestination.Profile.route) {
                    ProfileScreen(
                        account = account,
                        sessionState = sessionState,
                        onChangePassword = { navController.navigate("change_password") },
                        onClaimLegacyHistory = {
                            coroutineScope.launch {
                                showLegacyClaim = application.container.legacyClaimCoordinator.availability() == LegacyClaimAvailability.AVAILABLE
                            }
                        },
                        onLogout = authViewModel::logout,
                        recordHistoryLoader = application.container.foodRecordRepository::listRecentRecords,
                    )
                }
                composable("change_password") { ChangePasswordScreen(onSubmit = authViewModel::changePassword, onBack = { navController.popBackStack() }) }
                composable("record_create") {
                    val food = selectedFood
                    if (food == null) {
                        SearchScreen(
                            sessionState = sessionState,
                            username = account.username,
                            onCreateRecord = { item ->
                                openRecordCreate(item, "text_search")
                            },
                            onEditRecord = { record ->
                                openRecordEdit(record)
                            },
                            onOpenManualCreate = ::openManualCreate,
                            onOpenOcrRecognition = {
                                navController.navigate("ocr_recognition")
                            },
                            externalQuerySeed = searchQuerySeed,
                            externalSuggestedQueries = searchSuggestedQueries,
                            recentRefreshToken = recordRefreshToken,
                            onExternalQueryConsumed = {
                                searchQuerySeed = null
                                searchSuggestedQueries = emptyList()
                            },
                        )
                    } else {
                        RecordCreateScreen(
                            selectedFood = food,
                            sourceType = selectedSourceType,
                            sessionState = sessionState,
                            submissionCoordinator = application.container.foodRecordSubmissionCoordinator,
                            onSwitchRecommendedFood = { item ->
                                selectedFood = item
                                selectedSourceType = "text_search"
                            },
                            onBackToSearch = {
                                navController.popBackStack()
                            },
                            onOpenDrafts = {
                                navController.popBackStack()
                                navController.navigate(SnkDestination.Gallery.route)
                            },
                            onSaved = { recordRefreshToken++ },
                        )
                    }
                }
                composable(
                    route = "record/{recordId}/edit",
                    arguments = listOf(navArgument("recordId") { type = NavType.LongType }),
                ) { backStackEntry ->
                    val recordId = backStackEntry.arguments?.getLong("recordId")
                    val record = selectedEditRecord
                    var detailRecord by remember(recordId) { mutableStateOf<FoodRecordHistoryItem?>(record?.takeIf { it.id == recordId }) }
                    var detailError by remember(recordId) { mutableStateOf<String?>(null) }
                    LaunchedEffect(recordId) {
                        val selected = record?.takeIf { it.id == recordId } ?: return@LaunchedEffect
                        when (val result = application.container.foodRecordRepository.getRecordForEdit(selected)) {
                            is FoodRecordDetailResult.Success -> detailRecord = result.record
                            is FoodRecordDetailResult.Failure -> detailError = result.message
                        }
                    }
                    if (record == null || record.id != recordId) {
                        SearchScreen(
                            sessionState = sessionState,
                            username = account.username,
                            onCreateRecord = { item ->
                                openRecordCreate(item, "text_search")
                            },
                            onEditRecord = { item ->
                                openRecordEdit(item)
                            },
                            onOpenManualCreate = ::openManualCreate,
                            onOpenOcrRecognition = {
                                navController.navigate("ocr_recognition")
                            },
                            externalQuerySeed = searchQuerySeed,
                            externalSuggestedQueries = searchSuggestedQueries,
                            recentRefreshToken = recordRefreshToken,
                            onExternalQueryConsumed = {
                                searchQuerySeed = null
                                searchSuggestedQueries = emptyList()
                            },
                        )
                    } else if (detailError != null) {
                        AuthStatusScreen("无法打开记录", detailError.orEmpty(), onBack = { navController.popBackStack() })
                    } else {
                        RecordEditScreen(
                            record = detailRecord ?: record,
                            onBack = {
                                navController.popBackStack()
                            },
                            onUpdated = { updated ->
                                selectedEditRecord = updated
                                recordRefreshToken++
                            },
                        )
                    }
                }
                composable("ocr_recognition") {
                    OcrRecognitionScreen(
                        sessionState = sessionState,
                        onFillSearchQuery = { query, suggestions ->
                            searchQuerySeed = query
                            searchSuggestedQueries = suggestions
                            navController.navigate(SnkDestination.Search.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenManualCreate = ::openManualCreate,
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }
                composable("manual_food_create") {
                    QuickRecordScreen(
                        sessionState = sessionState,
                        initialName = manualCreateSeedName,
                        onSaved = {
                            recordRefreshToken++
                            navController.navigate(SnkDestination.Gallery.route) {
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
    if (showLegacyClaim) {
        LegacyClaimDialog(application.container.legacyClaimCoordinator) { showLegacyClaim = false }
    }
}
