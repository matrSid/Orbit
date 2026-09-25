package com.optimusprime.wick

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.optimusprime.wick.engine.StudySessionService

class WickApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            StudySessionService.CHANNEL_ID,
            "Study session",
            NotificationManager.IMPORTANCE_LOW  // Low = no sound, but always visible
        ).apply {
            description = "Shows while a Wick study session is active."
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
