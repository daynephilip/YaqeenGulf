package com.yaqeengulf.deepfake

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.yaqeengulf.deepfake.data.local.AppDatabase
import com.yaqeengulf.deepfake.data.preferences.UserPreferencesRepository

class YaqeenApp : Application() {

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var modelManager: com.yaqeengulf.deepfake.inference.ModelManager
        private set

    lateinit var verificationOrchestrator: com.yaqeengulf.deepfake.domain.orchestrator.VerificationOrchestrator
        private set

    lateinit var cloudRepository: com.yaqeengulf.deepfake.data.cloud.CloudSourceCheckRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = UserPreferencesRepository(this)
        database = AppDatabase.getInstance(this)
        modelManager = com.yaqeengulf.deepfake.inference.ModelManager(this)
        verificationOrchestrator = com.yaqeengulf.deepfake.domain.orchestrator.VerificationOrchestrator(
            context = this,
            modelManager = modelManager,
            database = database
        )
        cloudRepository = com.yaqeengulf.deepfake.data.cloud.CloudSourceCheckRepository()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_OVERLAY_SERVICE,
                "Yaqeen Floating Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active notification for Yaqeen Gulf floating verification overlay"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_OVERLAY_SERVICE = "yaqeen_overlay_channel"
        lateinit var instance: YaqeenApp
            private set
    }
}
