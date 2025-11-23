package com.example.call_notification_sdk_example

import android.content.Intent
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.example.call_notification_sdk.service.CallNotificationService

class MainActivity : FlutterActivity() {
    private val CHANNEL = "call_notification_sdk/get_intent_extras"
    private var pendingIntentExtras: Map<String, Any?>? = null
    private var methodChannel: MethodChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Store intent extras if present
        intent?.extras?.let { extras ->
            val extrasMap = mutableMapOf<String, Any?>()
            extras.keySet().forEach { key ->
                when (val value = extras.get(key)) {
                    is String -> extrasMap[key] = value
                    is Int -> extrasMap[key] = value
                    is Long -> extrasMap[key] = value
                    is Boolean -> extrasMap[key] = value
                    is Bundle -> {
                        // Store Bundle directly - we'll extract it in the method channel
                        extrasMap[key] = value
                    }
                }
            }
            if (extrasMap.isNotEmpty()) {
                pendingIntentExtras = extrasMap
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Update pending extras when new intent arrives
        intent.extras?.let { extras ->
            val extrasMap = mutableMapOf<String, Any?>()
            extras.keySet().forEach { key ->
                when (val value = extras.get(key)) {
                    is String -> extrasMap[key] = value
                    is Int -> extrasMap[key] = value
                    is Long -> extrasMap[key] = value
                    is Boolean -> extrasMap[key] = value
                    is Bundle -> {
                        // Store Bundle directly - we'll extract it in the method channel
                        extrasMap[key] = value
                    }
                }
            }
            if (extrasMap.isNotEmpty()) {
                pendingIntentExtras = extrasMap
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel?.setMethodCallHandler { call, result ->
            if (call.method == "getIntentExtras") {
                // Always check current intent first, then stored extras
                val currentIntent = intent
                val currentExtras = currentIntent?.extras
                
                // First, try to get from current intent
                if (currentExtras != null) {
                    val hasCallExtras = currentExtras.containsKey(CallNotificationService.EXTRA_ROOM_ID) ||
                            currentExtras.containsKey(CallNotificationService.EXTRA_CALL_ID) ||
                            currentExtras.containsKey(CallNotificationService.EXTRA_PAYLOAD)
                    
                    if (hasCallExtras) {
                        // Check EXTRA_PAYLOAD bundle first
                        val payloadBundle = currentExtras.getBundle(CallNotificationService.EXTRA_PAYLOAD)
                        if (payloadBundle != null) {
                            val payloadMap = mutableMapOf<String, Any?>()
                            payloadBundle.keySet().forEach { key ->
                                payloadMap[key] = payloadBundle.get(key)
                            }
                            result.success(payloadMap)
                            return@setMethodCallHandler
                        }
                        
                        // Otherwise, return all extras
                        val extrasMap = mutableMapOf<String, Any?>()
                        currentExtras.keySet().forEach { key ->
                            extrasMap[key] = currentExtras.get(key)
                        }
                        if (extrasMap.isNotEmpty()) {
                            result.success(extrasMap)
                            return@setMethodCallHandler
                        }
                    }
                }
                
                // Fallback to stored extras
                val extras = pendingIntentExtras
                pendingIntentExtras = null
                
                if (extras == null || extras.isEmpty()) {
                    result.success(null)
                    return@setMethodCallHandler
                }
                
                // Check if we have call-related extras directly
                val hasDirectExtras = extras.containsKey(CallNotificationService.EXTRA_ROOM_ID) ||
                        extras.containsKey(CallNotificationService.EXTRA_CALL_ID)
                
                if (hasDirectExtras) {
                    result.success(extras)
                    return@setMethodCallHandler
                }
                
                // Check EXTRA_PAYLOAD bundle
                val payloadBundleObj = extras[CallNotificationService.EXTRA_PAYLOAD]
                if (payloadBundleObj is Bundle) {
                    val payloadMap = mutableMapOf<String, Any?>()
                    payloadBundleObj.keySet().forEach { key ->
                        payloadMap[key] = payloadBundleObj.get(key)
                    }
                    result.success(payloadMap)
                } else {
                    result.success(extras)
                }
            } else {
                result.notImplemented()
            }
        }
    }
}
