package com.example.call_notification_sdk.messaging

import com.example.call_notification_sdk.controller.CallNotificationController
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CallNotificationFirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        CallNotificationController.initialize(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        if (data.isEmpty()) return

        CallNotificationController.handleRemoteMessage(data)
    }
}

