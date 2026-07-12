package com.snk.app.data

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.snk.app.BuildConfig
import com.snk.app.data.auth.AnonymousAuthApi
import com.snk.app.data.auth.AnonymousSessionRepository
import com.snk.app.data.auth.InstallationIdStore
import com.snk.app.data.auth.LegacyClaimCoordinator
import com.snk.app.data.auth.AuthApi
import com.snk.app.data.auth.AuthRepository
import com.snk.app.data.auth.AuthenticatedSessionManager
import com.snk.app.data.auth.BearerTokenInterceptor
import com.snk.app.data.auth.PersistentDeviceIdProvider
import com.snk.app.data.auth.RefreshTokenAuthenticator
import com.snk.app.data.auth.SecureTokenStore
import com.snk.app.data.draft.DraftRecordRepository
import com.snk.app.data.food.FoodSearchApi
import com.snk.app.data.food.FoodSearchRepository
import com.snk.app.data.food.RecentSearchStore
import com.snk.app.data.local.SnkDatabase
import com.snk.app.data.record.FoodRecordApi
import com.snk.app.data.record.FoodRecordRepository
import com.snk.app.data.record.FoodRecordSubmissionCoordinator
import com.snk.app.sync.DraftSyncScheduler
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class AppContainer(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
    }

    private val publicOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            loggingInterceptor,
        )
        .build()

    private val publicRetrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(publicOkHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val authApi = publicRetrofit.create(AuthApi::class.java)
    private val secureTokenStore = SecureTokenStore(context)
    val authenticatedSessionManager = AuthenticatedSessionManager(authApi, secureTokenStore)

    private val businessOkHttpClient = publicOkHttpClient.newBuilder()
        .addInterceptor(BearerTokenInterceptor(authenticatedSessionManager))
        .authenticator(RefreshTokenAuthenticator(authenticatedSessionManager))
        .build()

    private val businessRetrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(businessOkHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val anonymousAuthApi = publicRetrofit.create(AnonymousAuthApi::class.java)
    private val foodSearchApi = businessRetrofit.create(FoodSearchApi::class.java)
    private val foodRecordApi = businessRetrofit.create(FoodRecordApi::class.java)
    private val installationIdStore = InstallationIdStore(context)
    private val database = Room.databaseBuilder(
        context,
        SnkDatabase::class.java,
        "snk-local.db",
    )
        .addMigrations(
            SnkDatabase.MIGRATION_1_2,
            SnkDatabase.MIGRATION_2_3,
            SnkDatabase.MIGRATION_3_4,
            SnkDatabase.MIGRATION_4_5,
        )
        .build()
    private val draftSyncScheduler = DraftSyncScheduler(context)

    val anonymousSessionRepository = AnonymousSessionRepository(
        api = anonymousAuthApi,
        installationIdStore = installationIdStore,
    )

    val authRepository = AuthRepository(
        api = authApi,
        tokenStore = secureTokenStore,
        deviceIdProvider = PersistentDeviceIdProvider(context),
        sessionManager = authenticatedSessionManager,
    )

    val foodSearchRepository = FoodSearchRepository(
        api = foodSearchApi,
    )

    val recentSearchStore = RecentSearchStore(context)

    val foodRecordRepository = FoodRecordRepository(
        api = foodRecordApi,
    )

    val draftRecordRepository = DraftRecordRepository(
        draftDao = database.foodRecordDraftDao(),
        sessionManager = authenticatedSessionManager,
    )

    val legacyClaimCoordinator = LegacyClaimCoordinator(
        remote = authRepository,
        installationStore = installationIdStore,
        draftMigrator = draftRecordRepository,
        currentUserId = authenticatedSessionManager,
    )

    val foodRecordSubmissionCoordinator = FoodRecordSubmissionCoordinator(
        remoteWriter = foodRecordRepository,
        draftSaver = draftRecordRepository,
        draftSyncTrigger = draftSyncScheduler,
    )

    fun scheduleDraftRetry(draftId: Long) {
        authenticatedSessionManager.currentUserId()?.let { ownerUserId ->
            draftSyncScheduler.scheduleDraftSync(draftId, ownerUserId)
        }
    }
}
