package com.example.call_notification_sdk.model

enum class CallStatus {
    ringing,
    answering,
    answered,
    declined,
    cancelled,
    timeout,
    missed;

    companion object {
        fun fromString(value: String?): CallStatus? {
            if (value.isNullOrEmpty()) return null
            return try {
                valueOf(value)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}

