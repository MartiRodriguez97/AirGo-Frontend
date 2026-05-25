package com.airgo.airgoApp.features.notifications

import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationUtilsTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getBitmapFromUrl returns null for null or empty string`() {
        assertNull(NotificationUtils.getBitmapFromUrl(null))
        assertNull(NotificationUtils.getBitmapFromUrl(""))
    }
}
