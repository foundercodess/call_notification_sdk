package com.example.call_notification_sdk.model

data class CallEvent(
    val status: CallStatus,
    val payload: CallNotificationPayload,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "status" to status.name,
        "payload" to payload.toMap(),
    )
}

