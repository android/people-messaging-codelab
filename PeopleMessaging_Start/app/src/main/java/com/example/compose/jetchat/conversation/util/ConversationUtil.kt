package com.example.compose.jetchat.conversation.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.example.compose.jetchat.R
import java.text.SimpleDateFormat

class ConversationUtil {

    companion object {
        const val REQUEST_BUBBLE = 2;
        const val CHANNEL_MESSAGES = "jetChatNotificationChannel"
        private const val CATEGORY_SHARE = "com.example.compose.jetchat.share.TEXT_SHARE_TARGET"
        val dateFormat = SimpleDateFormat("hh:mm aa")
        fun flagUpdateCurrent(mutable: Boolean): Int {
            return if (mutable) {
                if (Build.VERSION.SDK_INT >= 31) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            }
        }

        fun getTimeFormatted(time: Long): String {
            return dateFormat.format(time)
        }

        fun setUpNotificationChannels(context: Context) {
            val notificationManager : NotificationManager = context.getSystemService()!!
            if (notificationManager.getNotificationChannel(CHANNEL_MESSAGES) == null) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_MESSAGES,
                        context.getString(R.string.channel_messages),
                        // The importance must be IMPORTANCE_HIGH to show Bubbles.
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context!!.getString(R.string.channel_messages_description)
                    }
                )
            }
        }

        fun generateNotificationId(): Int {
            // In a real world example, this would obviously not suffice as a robust
            // solution.
            return Math.random().toInt()
        }

    }

}