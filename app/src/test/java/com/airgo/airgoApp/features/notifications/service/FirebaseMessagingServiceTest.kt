package com.airgo.airgoApp.features.notifications.service

import com.airgo.airgoApp.core.network.SessionManager
import com.google.firebase.messaging.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class FirebaseMessagingServiceTest {
    private lateinit var service: FirebaseMessagingService

    @Before
    fun setUp() {
        service = FirebaseMessagingService()
        mockkObject(SessionManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `resolveChatId returns symmetric ID when emissorId is present`() {
        val remoteMessage = mockk<RemoteMessage>()
        SessionManager.userId = 10

        val method: Method =
            FirebaseMessagingService::class.java.getDeclaredMethod(
                "resolveChatId",
                RemoteMessage::class.java,
                String::class.java,
            )
        method.isAccessible = true

        // Case myId < otherId -> "10-20"
        val id1 = method.invoke(service, remoteMessage, "20") as String
        assertEquals("10-20", id1)

        // Case myId > otherId -> "5-10"
        val id2 = method.invoke(service, remoteMessage, "5") as String
        assertEquals("5-10", id2)
    }

    @Test
    fun `resolveChatId returns chat_id from data if emissorId is null`() {
        val remoteMessage = mockk<RemoteMessage>()
        every { remoteMessage.data } returns mapOf("chat_id" to "unique_chat_123")
        SessionManager.userId = 10

        val method: Method =
            FirebaseMessagingService::class.java.getDeclaredMethod(
                "resolveChatId",
                RemoteMessage::class.java,
                String::class.java,
            )
        method.isAccessible = true

        val id = method.invoke(service, remoteMessage, null) as String
        assertEquals("unique_chat_123", id)
    }

    @Test
    fun `isNotificationForOtherUser returns true when receiverId does not match current user`() {
        SessionManager.userId = 100

        val method: Method =
            FirebaseMessagingService::class.java.getDeclaredMethod(
                "isNotificationForOtherUser",
                java.lang.Integer::class.java,
            )
        method.isAccessible = true

        val result = method.invoke(service, 200) as Boolean
        assertTrue(result)
    }

    @Test
    fun `isNotificationForOtherUser returns false when receiverId matches current user`() {
        SessionManager.userId = 100

        val method: Method =
            FirebaseMessagingService::class.java.getDeclaredMethod(
                "isNotificationForOtherUser",
                java.lang.Integer::class.java,
            )
        method.isAccessible = true

        val result = method.invoke(service, 100) as Boolean
        assertFalse(result)
    }

    @Test
    fun `isNotificationForOtherUser returns false when receiverId is null`() {
        SessionManager.userId = 100

        val method: Method =
            FirebaseMessagingService::class.java.getDeclaredMethod(
                "isNotificationForOtherUser",
                java.lang.Integer::class.java,
            )
        method.isAccessible = true

        val result = method.invoke(service, null) as Boolean
        assertFalse(result)
    }
}
