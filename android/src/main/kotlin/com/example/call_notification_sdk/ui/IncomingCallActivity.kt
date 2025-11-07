package com.example.call_notification_sdk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.call_notification_sdk.R
import com.example.call_notification_sdk.controller.CallNotificationController
import com.example.call_notification_sdk.model.CallNotificationPayload
import com.example.call_notification_sdk.service.CallNotificationService

class IncomingCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_notification_sdk_activity_incoming_call)

        val payloadBundle = intent.getBundleExtra(CallNotificationService.EXTRA_PAYLOAD)
        val payload = payloadBundle?.let { CallNotificationPayload.fromBundle(it) }

        val callerNameView: TextView = findViewById(R.id.call_notification_sdk_caller_name)
        val mediaTypeView: TextView = findViewById(R.id.call_notification_sdk_media_type)
        val acceptButton: ImageButton = findViewById(R.id.call_notification_sdk_button_accept)
        val declineButton: ImageButton = findViewById(R.id.call_notification_sdk_button_decline)

        callerNameView.text = payload?.callerName ?: getString(R.string.call_notification_sdk_unknown_caller)
        mediaTypeView.text = when (payload?.mediaType ?: payload?.type) {
            "call_video", "video" -> getString(R.string.call_notification_sdk_video_call)
            else -> getString(R.string.call_notification_sdk_audio_call)
        }

        acceptButton.setOnClickListener {
            CallNotificationController.onAccepted()
            finish()
        }

        declineButton.setOnClickListener {
            CallNotificationController.onDeclined()
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Ignore back press to avoid dismissing without explicit action.
            }
        })
    }

    companion object {
        fun createIntent(context: Context, payload: CallNotificationPayload): Intent {
            return Intent(context, IncomingCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(CallNotificationService.EXTRA_PAYLOAD, payload.toBundle())
            }
        }
    }
}

