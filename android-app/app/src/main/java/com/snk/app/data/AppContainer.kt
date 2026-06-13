package com.snk.app.data

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.snk.app.BuildConfig
import com.snk.app.data.auth.AnonymousAuthApi
import com.snk.app.data.auth.AnonymousSessionRepository
import com.snk.app.data.auth.InstallationIdStore
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
    private val installationIdStore = InstallationIdStore(context)

    val anonymousSessionRepository = AnonymousSessionRepository(
        api = anonymousAuthApi,
        installationIdStore = installationIdStore,
    )
}
