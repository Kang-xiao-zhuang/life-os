package com.zk.lifeos

import android.app.Application

/** Holds the app-wide object graph. Nothing else belongs here. */
class LifeOsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
