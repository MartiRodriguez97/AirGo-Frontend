package com.airgo.airgoApp.features.notifications.service

import android.content.Context
import com.airgo.airgoApp.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NotificationActionBuilderTest {
    @Test
    fun `createActions returns reply and read actions with correct titles`() {
        val context = mockk<Context>(relaxed = true)

        // Mock strings
        every { context.getString(R.string.chat_reply) } returns "Reply"
        every { context.getString(R.string.chat_mark_as_read) } returns "Mark as Read"
        every { context.getString(R.string.chat_reply_label) } returns "Type here..."

        val (replyAction, readAction) = createActions(context, "chat_123", "456")

        assertNotNull(replyAction)
        assertNotNull(readAction)
        assertEquals("Reply", replyAction.title)
        assertEquals("Mark as Read", readAction.title)

        // Remote input check
        val remoteInputs = replyAction.remoteInputs
        assertEquals(1, remoteInputs?.size ?: 0)
        assertEquals("Type here...", remoteInputs?.first()?.label)
    }
}
