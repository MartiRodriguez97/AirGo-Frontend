package com.airgo.airgoApp.features.notifications.service

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.notifications.NotificationUtils.getBitmapFromUrl
import com.airgo.airgoApp.features.notifications.NotificationUtils.getCircularBitmap
import kotlin.math.abs

// Variables constantes necesarias para las funciones movidas
private const val SIZE_N = 10
private const val ONE_SECOND_MS = 1000L

fun createUserPerson(context: Context): Person {
    val myProfilePicUrl = SessionManager.profilePictureUrl
    val myBitmap = getBitmapFromUrl(myProfilePicUrl)?.let { getCircularBitmap(it) }
    val myIcon = myBitmap?.let { IconCompat.createWithBitmap(it) }
    val userName = context.getString(R.string.chat_user_me)
    return Person
        .Builder()
        .setName(userName)
        .apply { if (myIcon != null) setIcon(myIcon) }
        .build()
}

fun createSenderPerson(
    title: String,
    circularBitmap: Bitmap?,
): Person {
    val avatarIcon = circularBitmap?.let { IconCompat.createWithBitmap(it) }
    return Person
        .Builder()
        .setName(title)
        .apply { if (avatarIcon != null) setIcon(avatarIcon) }
        .build()
}

fun getOrInitMessagingStyle(
    context: Context,
    chatId: String,
    userPerson: Person,
): NotificationCompat.MessagingStyle {
    val activeNotif =
        context
            .getSystemService(
                NotificationManager::class.java,
            ).activeNotifications
            .find {
                it.id == chatId.hashCode()
            }
    val existingStyle =
        activeNotif?.let {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it.notification)
        }
    return existingStyle ?: NotificationCompat.MessagingStyle(userPerson)
}

fun shouldResetStyle(
    context: Context,
    messagingStyle: NotificationCompat.MessagingStyle,
): Boolean =
    messagingStyle.messages.isNotEmpty() &&
        messagingStyle.messages
            .last()
            .person
            ?.name ==
        context.getString(R.string.chat_user_me)

fun isDuplicateMessage(
    style: NotificationCompat.MessagingStyle,
    body: String,
    timestamp: Long,
): Boolean =
    style.messages.any {
        it.text == body && abs(it.timestamp - timestamp) < ONE_SECOND_MS
    }

fun trimMessagingStyleIfNeeded(
    style: NotificationCompat.MessagingStyle,
    userPerson: Person,
): NotificationCompat.MessagingStyle {
    if (style.messages.size <= SIZE_N) return style
    val newStyle = NotificationCompat.MessagingStyle(userPerson)
    for (msg in style.messages.takeLast(SIZE_N)) newStyle.addMessage(msg)
    return newStyle
}
