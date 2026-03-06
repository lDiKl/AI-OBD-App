package com.avyrox.service

import android.app.Application
import com.avyrox.service.util.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AvyroxServiceApp : Application() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        syncManager.start()
    }
}
