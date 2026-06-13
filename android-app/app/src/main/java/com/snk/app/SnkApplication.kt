package com.snk.app

import android.app.Application
import com.snk.app.data.AppContainer

class SnkApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
