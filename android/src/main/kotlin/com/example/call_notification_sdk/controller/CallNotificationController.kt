package com.example.call_notification_sdk.controller

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.call_notification_sdk.model.CallEvent
import com.example.call_notification_sdk.model.CallNotificationConfig
import com.example.call_notification_sdk.model.CallNotificationPayload
import com.example.call_notification_sdk.model.CallStatus
import com.example.call_notification_sdk.service.CallNotificationService
import java.util.concurrent.CopyOnWriteArraySet

private const val TAG = "CallNotificationController"

object CallNotificationController {

    interface EventListener {
        fun onEvent(event: CallEvent)
    }

    private val listeners = CopyOnWriteArraySet<EventListener>()

    private lateinit var applicationContext: Context
    private var config: CallNotificationConfig? = null

    @Volatile
    private var currentPayload: CallNotificationPayload? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun configure(config: CallNotificationConfig) {
        this.config = config
        ensureNotificationChannel()
    }

    fun registerEventListener(listener: EventListener) {
        listeners.add(listener)
    }

    fun unregisterEventListener(listener: EventListener) {
        listeners.remove(listener)
    }

    fun handleRemoteMessage(data: Map<String, String>) {
        val callType = data["type"] ?: return
        if (callType != "call_audio" && callType != "call_video") {
            Log.d(TAG, "Ignoring message type=$callType")
            return
        }

        if (!::applicationContext.isInitialized) {
            Log.w(TAG, "handleRemoteMessage called before initialization")
            return
        }

        val payload = runCatching {
            CallNotificationPayload.fromData(data)
        }.getOrElse { error ->
            Log.e(TAG, "Failed to parse payload", error)
            return
        }

        stopService()
        currentPayload = payload
        emitEvent(CallEvent(CallStatus.ringing, payload))
        startService(payload)
    }

    fun updateStatus(roomId: String, status: CallStatus) {
        val payload = currentPayload
        if (payload == null || payload.roomId != roomId) {
            Log.d(TAG, "updateStatus ignored for roomId=$roomId")
            return
        }

        when (status) {
            CallStatus.cancelled -> onCallerCancelled()
            CallStatus.timeout, CallStatus.missed -> onTimeout()
            CallStatus.answered -> onAccepted()
            CallStatus.declined -> onDeclined()
            else -> Log.d(TAG, "updateStatus no-op for status=$status")
        }
    }

    fun onAccepted() {
        val payload = currentPayload ?: return
        stopService()
        emitEvent(CallEvent(CallStatus.answered, payload))
        currentPayload = null
        launchConfiguredActivity(payload)
    }

    fun onDeclined() {
        val payload = currentPayload ?: return
        stopService()
        emitEvent(CallEvent(CallStatus.declined, payload))
        currentPayload = null
    }

    fun onTimeout() {
        val payload = currentPayload ?: return
        stopService()
        emitEvent(CallEvent(CallStatus.timeout, payload))
        currentPayload = null
    }

    fun onCallerCancelled() {
        val payload = currentPayload ?: return
        stopService()
        emitEvent(CallEvent(CallStatus.cancelled, payload))
        currentPayload = null
    }

    private fun emitEvent(event: CallEvent) {
        listeners.forEach { listener ->
            runCatching { listener.onEvent(event) }.onFailure {
                Log.e(TAG, "Event listener error", it)
            }
        }
    }

    private fun ensureNotificationChannel() {
        val cfg = config ?: return
        CallNotificationService.ensureChannel(applicationContext, cfg)
    }

    private fun startService(payload: CallNotificationPayload) {
        val cfg = config
        if (cfg == null) {
            Log.w(TAG, "CallNotificationController not configured")
            return
        }

        val intent = CallNotificationService.buildStartIntent(applicationContext, cfg, payload)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }

    private fun stopService() {
        val intent = Intent(applicationContext, CallNotificationService::class.java)
        applicationContext.stopService(intent)
    }

    private fun launchConfiguredActivity(payload: CallNotificationPayload) {
        val cfg = config ?: return
        if (cfg.fullScreenActivity.isBlank()) return

        val component = ComponentName.unflattenFromString(cfg.fullScreenActivity)
            ?: runCatching {
                ComponentName(applicationContext, cfg.fullScreenActivity)
            }.getOrElse {
                Log.w(TAG, "Invalid fullScreenActivity ${cfg.fullScreenActivity}")
                return
            }

        val intent = Intent().apply {
            setClassName(component.packageName, component.className)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CallNotificationService.EXTRA_ROOM_ID, payload.roomId)
            putExtra(CallNotificationService.EXTRA_CALL_ID, payload.callId)
            putExtra(CallNotificationService.EXTRA_CALLER_NAME, payload.callerName)
            putExtra(CallNotificationService.EXTRA_CALLER_ID, payload.callerId)
            putExtra(CallNotificationService.EXTRA_MEDIA_TYPE, payload.mediaType)
            putExtra(CallNotificationService.EXTRA_TYPE, payload.type)
            putExtra(CallNotificationService.EXTRA_PAYLOAD, payload.toBundle())
        }

        applicationContext.startActivity(intent)
    }
}

