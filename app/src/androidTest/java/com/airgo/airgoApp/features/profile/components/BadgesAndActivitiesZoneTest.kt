package com.airgo.airgoApp.features.profile.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.airgo.airgoApp.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BadgesAndActivitiesZoneTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        // Esta es la forma correcta de obtener el contexto en androidTest
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun badgesAndActivitiesZone_isDisplayed() {
        composeTestRule.setContent {
            badgesAndActivitiesZone(
                onActivityClick = {},
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun badgesAndActivitiesZone_titleTextIsDisplayed() {
        composeTestRule.setContent {
            badgesAndActivitiesZone(
                onActivityClick = {},
            )
        }
        composeTestRule
            .onNodeWithText(context.getString(R.string.profile_activities_recent_title), substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun badgesZone_titleTextIsDisplayed() {
        composeTestRule.setContent {
            badgesZone(badges = emptyList())
        }
        composeTestRule
            .onNodeWithText(context.getString(R.string.profile_badges_zone_title), substring = true, ignoreCase = true)
            .assertExists()
    }
}
