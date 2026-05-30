package com.hyperosunfcker

import android.app.Application
import com.hyperosunfcker.data.SettingsStore

class HyperOSUnfckerApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        // Initializing the SettingsStore
        SettingsStore.initialize(applicationContext)
    }
}