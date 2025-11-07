package com.example.call_notification_sdk.model

import android.os.Bundle
import org.json.JSONObject

data class CallNotificationPayload(
    val callId: String,
    val roomId: String,
    val callerId: String,
    val callerName: String,
    val receiverId: String,
    val type: String,
    val avatarUrl: String?,
    val mediaType: String?,
    val metadata: Map<String, Any?>,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "callId" to callId,
        "roomId" to roomId,
        "callerId" to callerId,
        "callerName" to callerName,
        "receiverId" to receiverId,
        "type" to type,
        "avatarUrl" to avatarUrl,
        "mediaType" to mediaType,
        "metadata" to metadata,
    )

    fun toBundle(): Bundle = Bundle().apply {
        putString("callId", callId)
        putString("roomId", roomId)
        putString("callerId", callerId)
        putString("callerName", callerName)
        putString("receiverId", receiverId)
        putString("type", type)
        putString("avatarUrl", avatarUrl)
        putString("mediaType", mediaType)
        putString("metadata", JSONObject(metadata).toString())
    }

    companion object {
        private fun Map<String, String>.requireString(key: String): String {
            return this[key]?.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Missing required key: $key")
        }

        fun fromData(data: Map<String, String>): CallNotificationPayload {
            val metadata = data["metadata"]?.let { raw ->
                runCatching {
                    val json = JSONObject(raw)
                    json.keys().asSequence().associateWith { key -> json.opt(key) }
                }.getOrDefault(emptyMap())
            } ?: emptyMap()

            return CallNotificationPayload(
                callId = data["callId"] ?: data.requireString("roomId"),
                roomId = data.requireString("roomId"),
                callerId = data.requireString("callerId"),
                callerName = data.requireString("callerName"),
                receiverId = data.requireString("receiverId"),
                type = data.requireString("type"),
                avatarUrl = data["avatarUrl"],
                mediaType = data["mediaType"],
                metadata = metadata,
            )
        }

        fun fromBundle(bundle: Bundle): CallNotificationPayload {
            val metadata = bundle.getString("metadata")?.let { raw ->
                runCatching {
                    val json = JSONObject(raw)
                    json.keys().asSequence().associateWith { key -> json.opt(key) }
                }.getOrDefault(emptyMap())
            } ?: emptyMap()

            return CallNotificationPayload(
                callId = bundle.getString("callId") ?: bundle.getString("roomId")!!,
                roomId = bundle.getString("roomId")!!,
                callerId = bundle.getString("callerId")!!,
                callerName = bundle.getString("callerName")!!,
                receiverId = bundle.getString("receiverId")!!,
                type = bundle.getString("type")!!,
                avatarUrl = bundle.getString("avatarUrl"),
                mediaType = bundle.getString("mediaType"),
                metadata = metadata,
            )
        }
    }
}

