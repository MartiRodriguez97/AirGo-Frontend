package com.airgo.airgoApp.features.notifications.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.airgo.airgoApp.R

fun createActions(
    context: Context,
    chatId: String,
    emissorId: String,
): Pair<NotificationCompat.Action, NotificationCompat.Action> {
    val replyIntent =
        Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.airgo.airgoApp.ACTION_REPLY"
            putExtra("CHAT_ID", chatId)
            putExtra("OTHER_USER_ID", emissorId.toIntOrNull() ?: 0)
            putExtra("NOTIFICATION_ID", chatId.hashCode())
        }
    val replyPendingIntent =
        PendingIntent.getBroadcast(
            context,
            chatId.hashCode() + 2,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

    val readIntent =
        Intent(
            context,
            NotificationActionReceiver::class.java,
        ).apply {
            action = "com.airgo.airgoApp.ACTION_MARK_AS_READ"
            putExtra("CHAT_ID", chatId)
            putExtra("NOTIFICATION_ID", chatId.hashCode())
        }
    val readPendingIntent =
        PendingIntent.getBroadcast(
            context,
            chatId.hashCode() + 1,
            readIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    val replyAction =
        NotificationCompat.Action
            .Builder(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.chat_reply),
                replyPendingIntent,
            ).addRemoteInput(
                RemoteInput
                    .Builder("KEY_TEXT_REPLY")
                    .setLabel(context.getString(R.string.chat_reply_label))
                    .build(),
            ).build()
    val readAction =
        NotificationCompat.Action
            .Builder(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.chat_mark_as_read),
                readPendingIntent,
            ).build()

    return Pair(replyAction, readAction)
}
