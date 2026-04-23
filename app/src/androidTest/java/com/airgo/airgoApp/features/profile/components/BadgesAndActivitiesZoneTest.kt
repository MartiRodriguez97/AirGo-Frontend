package com.airgo.airgoApp.features.profile.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airgo.airgoApp.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry

@RunWith(AndroidJUnit4::class)
class BadgesAndActivitiesZoneTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        // Esta es la forma correcta de obtener el contexto en androidTest
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }
    private lateinit var context: android.content.Context

    @Test
    fun badgesAndActivitiesZone_isDisplayed() {
        composeTestRule.setContent {
            badgesAndActivitiesZone()
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun badgesAndActivitiesZone_titleTextIsDisplayed() {
        composeTestRule.setContent {
            badgesAndActivitiesZone()
        }
        composeTestRule
            .onNodeWithText(context.getString(R.string.profile_badges_title), substring = true, ignoreCase = true)
            .assertExists()
    }
}
