package com.example.call_notification_sdk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.call_notification_sdk.R
import com.example.call_notification_sdk.controller.CallNotificationController
import com.example.call_notification_sdk.model.CallNotificationConfig
import com.example.call_notification_sdk.model.CallNotificationPayload
import com.example.call_notification_sdk.ui.IncomingCallActivity

class CallNotificationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { CallNotificationController.onTimeout() }

    override fun onCreate() {
        super.onCreate()
        CallNotificationController.initialize(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_ACCEPT -> handleAccept()
            ACTION_DECLINE -> handleDecline()
            ACTION_TIMEOUT -> handleTimeout()
            else -> Log.d(TAG, "Unknown action ${intent?.action}")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleStart(intent: Intent) {
        val configBundle = intent.getBundleExtra(EXTRA_CONFIG)
        val payloadBundle = intent.getBundleExtra(EXTRA_PAYLOAD)

        if (configBundle == null || payloadBundle == null) {
            Log.w(TAG, "Missing config or payload")
            stopSelf()
            return
        }

        lastConfig = CallNotificationConfig.fromBundle(configBundle)
        lastPayload = CallNotificationPayload.fromBundle(payloadBundle)

        val config = lastConfig!!

        startForeground(NOTIFICATION_ID, buildNotification(config, lastPayload!!))
        scheduleTimeout(config)
        launchFullScreen(lastPayload!!)
    }

    private fun handleAccept() {
        CallNotificationController.onAccepted()
        stopSelf()
    }

    private fun handleDecline() {
        CallNotificationController.onDeclined()
        stopSelf()
    }

    private fun handleTimeout() {
        CallNotificationController.onTimeout()
        stopSelf()
    }

    private fun buildNotification(
        config: CallNotificationConfig,
        payload: CallNotificationPayload,
    ): Notification {
        val acceptIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, CallNotificationService::class.java).apply { action = ACTION_ACCEPT },
            pendingIntentFlags(),
        )

        val declineIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CallNotificationService::class.java).apply { action = ACTION_DECLINE },
            pendingIntentFlags(),
        )

        val fullScreenIntent = PendingIntent.getActivity(
            this,
            2,
            IncomingCallActivity.createIntent(this, payload),
            pendingIntentFlags(),
        )

        val builder = NotificationCompat.Builder(this, config.androidChannelId)
            .setContentTitle(payload.callerName)
            .setContentText(getString(R.string.call_notification_sdk_incoming_call))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(resolveIcon(config))
            .setFullScreenIntent(fullScreenIntent, true)
            .setOngoing(true)
            .addAction(
                R.drawable.call_notification_sdk_ic_decline,
                config.declineActionLabel ?: getString(R.string.call_notification_sdk_decline),
                declineIntent,
            )
            .addAction(
                R.drawable.call_notification_sdk_ic_accept,
                config.acceptActionLabel ?: getString(R.string.call_notification_sdk_accept),
                acceptIntent,
            )

        if (config.ringtone != null) {
            val soundUri = Uri.parse("android.resource://$packageName/${config.ringtone}")
            builder.setSound(soundUri, AudioManager.STREAM_RING)
        }

        lastNotification = builder.build()
        return lastNotification!!
    }

    private fun resolveIcon(config: CallNotificationConfig): Int {
        val iconName = config.notificationIcon ?: DEFAULT_NOTIFICATION_ICON
        val resId = resources.getIdentifier(iconName, "drawable", packageName)
        return if (resId != 0) resId else R.drawable.call_notification_sdk_ic_notification
    }

    private fun scheduleTimeout(config: CallNotificationConfig) {
        val timeout = config.ringTimeoutMs ?: DEFAULT_RING_TIMEOUT_MS
        if (timeout <= 0L) return

        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, timeout)
    }

    private fun launchFullScreen(payload: CallNotificationPayload) {
        val intent = IncomingCallActivity.createIntent(this, payload)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeoutRunnable)
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
    }

    companion object {
        const val EXTRA_CONFIG = "config"
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_ROOM_ID = "roomId"
        const val EXTRA_CALL_ID = "callId"
        const val EXTRA_CALLER_NAME = "callerName"
        const val EXTRA_CALLER_ID = "callerId"
        const val EXTRA_TYPE = "type"
        const val EXTRA_MEDIA_TYPE = "mediaType"

        private const val ACTION_START = "com.example.call_notification_sdk.action.START"
        private const val ACTION_ACCEPT = "com.example.call_notification_sdk.action.ACCEPT"
        private const val ACTION_DECLINE = "com.example.call_notification_sdk.action.DECLINE"
        private const val ACTION_TIMEOUT = "com.example.call_notification_sdk.action.TIMEOUT"

        private const val NOTIFICATION_ID = 424202
        private const val DEFAULT_RING_TIMEOUT_MS = 45_000L
        private const val DEFAULT_NOTIFICATION_ICON = "call_notification_sdk_ic_notification"
        private const val TAG = "CallNotificationSvc"

        private var lastConfig: CallNotificationConfig? = null
        private var lastPayload: CallNotificationPayload? = null
        private var lastNotification: Notification? = null

        private val handler = Handler(Looper.getMainLooper())

        private val timeoutRunnable = Runnable {
            lastConfig ?: return@Runnable
            CallNotificationController.onTimeout()
        }

        fun ensureChannel(context: Context, config: CallNotificationConfig) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(config.androidChannelId)
            if (existing != null) return

            val channel = NotificationChannel(
                config.androidChannelId,
                config.androidChannelName,
                NotificationManager.IMPORTANCE_HIGH,
            )

            if (config.ringtone != null) {
                val soundUri = Uri.parse("android.resource://${context.packageName}/${config.ringtone}")
                channel.setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }

            manager.createNotificationChannel(channel)
        }

        fun buildStartIntent(
            context: Context,
            config: CallNotificationConfig,
            payload: CallNotificationPayload,
        ): Intent {
            return Intent(context, CallNotificationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CONFIG, config.toBundle())
                putExtra(EXTRA_PAYLOAD, payload.toBundle())
            }
        }

        private fun pendingIntentFlags(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        }
    }
}

