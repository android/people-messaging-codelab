package com.example.compose.jetchat.conversation

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.example.compose.jetchat.conversation.ConversationFragment.Companion.MSG_NOTIFICATION_ID

/**
 * Handles the "Reply" action in the chat notification.
 */
class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.extras?.getInt(MSG_NOTIFICATION_ID)!!
        val notificationManager : NotificationManager = context!!.getSystemService()!!
        notificationManager.cancel(notificationId)
    }
}