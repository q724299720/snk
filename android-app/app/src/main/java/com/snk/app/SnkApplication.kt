package com.snk.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.snk.app.data.AppContainer
import com.snk.app.data.auth.AuthenticatedImageInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class SnkApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        val apiHost = BuildConfig.API_BASE_URL.toHttpUrl().host
        val imageClient = OkHttpClient.Builder()
            .addInterceptor(
                AuthenticatedImageInterceptor(apiHost) {
                    container.authenticatedSessionManager.currentAccessToken()
                },
            )
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(imageClient)
            .build()
    }
}
