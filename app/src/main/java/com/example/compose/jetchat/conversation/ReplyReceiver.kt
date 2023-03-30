package com.example.compose.jetchat.conversation

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutManagerCompat
import com.example.compose.jetchat.conversation.ConversationFragment.Companion.MSG_AUTHOR_IMG
import com.example.compose.jetchat.conversation.ConversationFragment.Companion.MSG_NOTIFICATION_ID
import com.example.compose.jetchat.conversation.ConversationFragment.Companion.MSG_NOTIFICATION_SHORTCUT_ID
import com.example.compose.jetchat.conversation.ConversationFragment.Companion.MSG_TIMESTAMP
import com.example.compose.jetchat.conversation.util.ConversationUtil
import com.example.compose.jetchat.data.exampleUiState
import java.text.SimpleDateFormat

/**
 * Handles the "Reply" action in the chat notification.
 */
class ReplyReceiver : BroadcastReceiver() {

    companion object {
        const val KEY_TEXT_REPLY = "reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.extras?.getInt(MSG_NOTIFICATION_ID)!!
        val shortcutId = intent.extras?.getString(MSG_NOTIFICATION_SHORTCUT_ID)!!
        // Create a shortcut for outgoing messages.
        generateAndPushShortcutForOutgoingMessage(context, shortcutId)
        val notificationManager : NotificationManager = context!!.getSystemService()!!
        // setRemoteInputHistory()
        notificationManager.cancel(notificationId)
    }

    private fun generateAndPushShortcutForOutgoingMessage(
        context: Context,
        shortcutId: String
    ) {
        // If there is a shortcut ID in the intent then this reply is in response to an already
        // existing notification that generated a shortcut
        if (shortcutId.isNotEmpty()) {
            val shortcut = ConversationUtil.generateShortcut(context, "me")
            ShortcutManagerCompat.pushDynamicShortcut(context!!, shortcut)
        }
    }

    private fun extractMessage(context: Context, intent: Intent): Message {
        val msg = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT_REPLY)
        return Message(
            "me",
            msg.toString(),
            ConversationUtil.dateFormat.format(intent.extras?.getLong(MSG_TIMESTAMP)!!),
            intent.extras?.getInt(MSG_AUTHOR_IMG)
        )
    }
}