package com.optimusprime.orbit.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.optimusprime.orbit.MainActivity
import com.optimusprime.orbit.R

/**
 * A minimal foreground service whose only job is to keep the process alive
 * while a study session is running. The actual clock logic stays in
 * [SessionViewModel]; this service just holds the foreground notification
 * so Android doesn't kill the process when the user switches apps.
 *
 * Start it with [startSession] and stop it with [stopSession].
 */
class StudySessionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification(this))
            ACTION_STOP  -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID     = "orbit_session"
        const val ACTION_START   = "orbit.START_SESSION"
        const val ACTION_STOP    = "orbit.STOP_SESSION"

        fun startSession(context: Context) {
            val intent = Intent(context, StudySessionService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stopSession(context: Context) {
            val intent = Intent(context, StudySessionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun buildNotification(context: Context): Notification {
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val tapPending = PendingIntent.getActivity(
                context, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Orbit — session in progress")
                .setContentText("Your study timer is running.")
                .setContentIntent(tapPending)
                .setOngoing(true)
                .setShowWhen(false)
                .build()
        }
    }
}
