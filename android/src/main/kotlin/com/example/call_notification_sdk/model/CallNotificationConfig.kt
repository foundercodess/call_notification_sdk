package com.example.call_notification_sdk.model

import android.os.Bundle

data class CallNotificationConfig(
    val androidChannelId: String,
    val androidChannelName: String,
    val fullScreenActivity: String,
    val notificationIcon: String?,
    val acceptActionLabel: String?,
    val declineActionLabel: String?,
    val ringtone: String?,
    val ringTimeoutMs: Long?,
    val enableFirestoreStatusListener: Boolean,
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString("androidChannelId", androidChannelId)
        putString("androidChannelName", androidChannelName)
        putString("fullScreenActivity", fullScreenActivity)
        putString("notificationIcon", notificationIcon)
        putString("acceptActionLabel", acceptActionLabel)
        putString("declineActionLabel", declineActionLabel)
        putString("ringtone", ringtone)
        putLong("ringTimeoutMs", ringTimeoutMs ?: 0)
        putBoolean("enableFirestoreStatusListener", enableFirestoreStatusListener)
    }

    companion object {
        private fun Any?.stringOrNull(): String? = this as? String

        fun fromBundle(bundle: Bundle): CallNotificationConfig {
            val timeout = bundle.getLong("ringTimeoutMs", -1)

            return CallNotificationConfig(
                androidChannelId = bundle.getString("androidChannelId")!!,
                androidChannelName = bundle.getString("androidChannelName")!!,
                fullScreenActivity = bundle.getString("fullScreenActivity")!!,
                notificationIcon = bundle.getString("notificationIcon"),
                acceptActionLabel = bundle.getString("acceptActionLabel"),
                declineActionLabel = bundle.getString("declineActionLabel"),
                ringtone = bundle.getString("ringtone"),
                ringTimeoutMs = timeout.takeIf { it > 0 },
                enableFirestoreStatusListener = bundle.getBoolean("enableFirestoreStatusListener"),
            )
        }

        fun fromMap(map: Map<*, *>): CallNotificationConfig {
            val channelId = map["androidChannelId"].stringOrNull()
            val channelName = map["androidChannelName"].stringOrNull()
            val fullScreenActivity = map["androidFullScreenIntentActivity"].stringOrNull()

            require(!channelId.isNullOrEmpty()) { "androidChannelId is required" }
            require(!channelName.isNullOrEmpty()) { "androidChannelName is required" }
            require(!fullScreenActivity.isNullOrEmpty()) {
                "androidFullScreenIntentActivity is required"
            }

            val ringTimeout = when (val value = map["ringTimeoutMs"]) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }

            val enableFirestore = when (val value = map["enableFirestoreStatusListener"]) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                else -> false
            }

            return CallNotificationConfig(
                androidChannelId = channelId,
                androidChannelName = channelName,
                fullScreenActivity = fullScreenActivity,
                notificationIcon = map["androidNotificationIcon"].stringOrNull(),
                acceptActionLabel = map["androidAcceptAction"].stringOrNull(),
                declineActionLabel = map["androidDeclineAction"].stringOrNull(),
                ringtone = map["androidRingtone"].stringOrNull(),
                ringTimeoutMs = ringTimeout,
                enableFirestoreStatusListener = enableFirestore,
            )
        }
    }
}

