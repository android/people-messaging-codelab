/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.compose.jetchat.conversation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action.SEMANTIC_ACTION_REPLY
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.example.compose.jetchat.MainViewModel
import com.example.compose.jetchat.NavActivity
import com.example.compose.jetchat.R
import com.example.compose.jetchat.conversation.util.ConversationUtil
import com.example.compose.jetchat.data.exampleUiState
import com.example.compose.jetchat.theme.JetchatTheme
import java.util.Date


class ConversationFragment : Fragment() {

    companion object {
        private const val REQUEST_CONTENT = 1
        private const val SEND_NOTIFICATION_ACTION = "com.example.compose.jetchat.conversation.NOTIFICATION_ACTION"
        const val MSG_AUTHOR =  "author"
        const val MSG_AUTHOR_IMG =  "authorImg"
        const val MSG_NOTIFICATION_ID =  "notificationId"
        const val MSG_NOTIFICATION_SHORTCUT_ID = "shortcutId"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val MSG_TIMESTAMP =  "timestamp"
    }

    private val activityViewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestPermissionIfNecessary()
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

        setContent {
            CompositionLocalProvider(
                LocalBackPressedDispatcher provides requireActivity().onBackPressedDispatcher
            ) {
                JetchatTheme {
                    ConversationContent(
                        uiState = exampleUiState,
                        navigateToProfile = { user ->
                            // Click callback
                            val bundle = bundleOf("userId" to user)
                            findNavController().navigate(
                                R.id.nav_profile,
                                bundle
                            )
                        },
                        onNavIconPressed = {
                            activityViewModel.openDrawer()
                        },
                        // Add padding so that we are inset from any navigation bars
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets
                                .navigationBars
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)

                        )
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (exampleUiState.messages.isNotEmpty()) {
            ConversationUtil.setUpNotificationChannels(context!!)
            // Only simulate response if the most recent message was from user
            if (exampleUiState.messages.first().author == "me") {
                simulateResponseAsANotification()
            }
        }
    }

    private fun requestPermissionIfNecessary() {
        if (Build.VERSION.SDK_INT < 33) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                requireActivity().finish()
            }
        }.launch(permission)
    }

    /**
     * Simulates a response via a notification.
     */
    private fun simulateResponseAsANotification() {
        val time = Date().time
        val timestamp = ConversationUtil.getTimeFormatted(time)
        exampleUiState.addMessage(
            Message(
                "Taylor Brooks",
                "Welcome to the chat!",
                timestamp
            )
        )
        val message = exampleUiState.messages[0]
        if (message.author != "me") {
            val icon = IconCompat.createWithResource(context!!, R.drawable.someone_else)
            val notificationId = ConversationUtil.generateNotificationId()
            val person = Person.Builder()
                .setName(message.author)
                .setIcon(icon)
                .build()
            val notification = createNotification(notificationId, message, person, null, time)
            val notificationManager : NotificationManager = context!!.getSystemService()!!
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun createNotification(
        notificationId: Int,
        message: Message,
        person: Person,
        shortcut: ShortcutInfoCompat?,
        time: Long
    ): Notification {
        val replyAction = generateReplyAction(notificationId, message, shortcut)
        return NotificationCompat.Builder(context!!, ConversationUtil.CHANNEL_MESSAGES)
            .addPerson(person)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    REQUEST_CONTENT,
                    Intent(context, NavActivity::class.java)
                        .setAction(Intent.ACTION_VIEW),
                    ConversationUtil.flagUpdateCurrent(mutable = false)
                )
            )
            .setContentText(message.content)
            .setSmallIcon(R.drawable.ic_message)
            .addAction(replyAction)
            .setShowWhen(true)
            .build()
    }

    private fun generateReplyAction(
        notificationId: Int,
        message: Message,
        shortcut: ShortcutInfoCompat?
    ): NotificationCompat.Action {
        val timestamp = Date().time

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel(resources.getString(R.string.remote_input_label))
            build()
        }
        val intent = Intent(
            SEND_NOTIFICATION_ACTION,
            Uri.EMPTY,
            context,
            ReplyReceiver::class.java
        )
        intent.apply {
            putExtra(MSG_NOTIFICATION_ID, notificationId)
            putExtra(MSG_AUTHOR, message.author)
            putExtra(MSG_AUTHOR_IMG, message.authorImage)
            putExtra(MSG_TIMESTAMP, timestamp)
            if (shortcut != null) {
                putExtra(MSG_NOTIFICATION_SHORTCUT_ID, shortcut.id)
            }
        }
        val replyPendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CONTENT,
                intent,
                ConversationUtil.flagUpdateCurrent(mutable = true)
            )

        // Create the reply action and add the remote input.
        return NotificationCompat.Action.Builder(
            IconCompat.createWithResource(
                context!!, R.drawable.ic_reply_icon
            ), getString(R.string.reply_label), replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .setSemanticAction(SEMANTIC_ACTION_REPLY)
            .build()
    }
}
