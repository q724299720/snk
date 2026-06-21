package com.snk.app.data

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.snk.app.BuildConfig
import com.snk.app.data.auth.AnonymousAuthApi
import com.snk.app.data.auth.AnonymousSessionRepository
import com.snk.app.data.auth.InstallationIdStore
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

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val anonymousAuthApi = retrofit.create(AnonymousAuthApi::class.java)
    private val foodSearchApi = retrofit.create(FoodSearchApi::class.java)
    private val foodRecordApi = retrofit.create(FoodRecordApi::class.java)
    private val installationIdStore = InstallationIdStore(context)
    private val database = Room.databaseBuilder(
        context,
        SnkDatabase::class.java,
        "snk-local.db",
    )
        .addMigrations(SnkDatabase.MIGRATION_1_2)
        .build()
    private val draftSyncScheduler = DraftSyncScheduler(context)

    val anonymousSessionRepository = AnonymousSessionRepository(
        api = anonymousAuthApi,
        installationIdStore = installationIdStore,
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
    )

    val foodRecordSubmissionCoordinator = FoodRecordSubmissionCoordinator(
        remoteWriter = foodRecordRepository,
        draftSaver = draftRecordRepository,
        draftSyncTrigger = draftSyncScheduler,
    )

    fun scheduleDraftRetry(draftId: Long) {
        draftSyncScheduler.scheduleDraftSync(draftId)
    }
}
