package com.example.call_notification_sdk

import android.content.Context
import android.util.Log
import com.example.call_notification_sdk.controller.CallNotificationController
import com.example.call_notification_sdk.model.CallEvent
import com.example.call_notification_sdk.model.CallNotificationConfig
import com.example.call_notification_sdk.model.CallStatus
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

private const val METHOD_CHANNEL_NAME = "call_notification_sdk/methods"
private const val EVENT_CHANNEL_NAME = "call_notification_sdk/events"
private const val LOG_TAG = "CallNotificationSDK"

class CallNotificationSdkPlugin :
    FlutterPlugin,
    MethodCallHandler,
    EventChannel.StreamHandler,
    CallNotificationController.EventListener {

    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel

    private lateinit var applicationContext: Context

    private var eventSink: EventChannel.EventSink? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext

        methodChannel = MethodChannel(binding.binaryMessenger, METHOD_CHANNEL_NAME)
        methodChannel.setMethodCallHandler(this)

        eventChannel = EventChannel(binding.binaryMessenger, EVENT_CHANNEL_NAME)
        eventChannel.setStreamHandler(this)

        CallNotificationController.initialize(applicationContext)
        CallNotificationController.registerEventListener(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "initialize" -> handleInitialize(call, result)
            "handleRemoteMessage" -> handleRemoteMessage(call, result)
            "updateStatus" -> handleUpdateStatus(call, result)
            else -> result.notImplemented()
        }
    }

    private fun handleInitialize(call: MethodCall, result: Result) {
        val configMap = call.arguments as? Map<*, *>
        if (configMap == null) {
            result.error("INVALID_ARGUMENT", "Config payload is missing", null)
            return
        }

        try {
            val config = CallNotificationConfig.fromMap(configMap)
            CallNotificationController.configure(config)
            result.success(null)
        } catch (e: IllegalArgumentException) {
            Log.e(LOG_TAG, "Failed to parse config", e)
            result.error("INVALID_CONFIG", e.message, null)
        }
    }

    private fun handleRemoteMessage(call: MethodCall, result: Result) {
        val data = call.arguments as? Map<*, *>
        if (data == null) {
            result.error("INVALID_ARGUMENT", "Remote message data is missing", null)
            return
        }

        val payload = data.mapNotNull { (key, value) ->
            val stringKey = key as? String
            val stringValue = value as? String
            if (stringKey != null && stringValue != null) stringKey to stringValue else null
        }.toMap()

        CallNotificationController.handleRemoteMessage(payload)
        result.success(null)
    }

    private fun handleUpdateStatus(call: MethodCall, result: Result) {
        val arguments = call.arguments as? Map<*, *>
        if (arguments == null) {
            result.error("INVALID_ARGUMENT", "Arguments are missing", null)
            return
        }

        val roomId = arguments["roomId"] as? String
        val statusName = arguments["status"] as? String

        if (roomId.isNullOrEmpty() || statusName.isNullOrEmpty()) {
            result.error("INVALID_ARGUMENT", "roomId or status missing", null)
            return
        }

        val status = try {
            CallStatus.valueOf(statusName)
        } catch (e: IllegalArgumentException) {
            Log.w(LOG_TAG, "Unknown status: $statusName")
            null
        }

        if (status == null) {
            result.error("INVALID_STATUS", "Unknown status: $statusName", null)
            return
        }

        CallNotificationController.updateStatus(roomId, status)
        result.success(null)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        CallNotificationController.unregisterEventListener(this)
        eventSink = null
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    override fun onEvent(event: CallEvent) {
        eventSink?.success(event.toMap())
    }
}
