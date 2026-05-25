package com.airgo.airgoApp.features.notifications.service

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
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

class FirebaseMessagingHelperTest {
    private lateinit var context: Context
    private lateinit var userPerson: Person

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkObject(SessionManager)
        SessionManager.profilePictureUrl = null

        every { context.getString(R.string.chat_user_me) } returns "Me"
        userPerson = Person.Builder().setName("Me").build()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `createUserPerson returns person with correct name`() {
        val person = createUserPerson(context)
        assertEquals("Me", person.name)
    }

    @Test
    fun `isDuplicateMessage returns true for identical message within 1 second`() {
        val style = NotificationCompat.MessagingStyle(userPerson)
        style.addMessage("Hello", 1000L, userPerson)

        // 1500ms - 1000ms = 500ms (within 1s range)
        assertTrue(isDuplicateMessage(style, "Hello", 1500L))
    }

    @Test
    fun `isDuplicateMessage returns false for different content`() {
        val style = NotificationCompat.MessagingStyle(userPerson)
        style.addMessage("Hello", 1000L, userPerson)

        assertFalse(isDuplicateMessage(style, "World", 1500L))
    }

    @Test
    fun `isDuplicateMessage returns false for same content after 1 second`() {
        val style = NotificationCompat.MessagingStyle(userPerson)
        style.addMessage("Hello", 1000L, userPerson)

        // 2500ms - 1000ms = 1500ms (outside 1s range)
        assertFalse(isDuplicateMessage(style, "Hello", 2500L))
    }

    @Test
    fun `trimMessagingStyleIfNeeded keeps only last 10 messages`() {
        val style = NotificationCompat.MessagingStyle(userPerson)
        for (i in 1..15) {
            style.addMessage("Msg $i", i * 1000L, userPerson)
        }

        val trimmed = trimMessagingStyleIfNeeded(style, userPerson)
        assertEquals(10, trimmed.messages.size)
        assertEquals("Msg 6", trimmed.messages.first().text)
        assertEquals("Msg 15", trimmed.messages.last().text)
    }

    @Test
    fun `shouldResetStyle returns true if last message is from me`() {
        val style = NotificationCompat.MessagingStyle(userPerson)
        style.addMessage("Reply", 1000L, userPerson)

        assertTrue(shouldResetStyle(context, style))
    }
}
