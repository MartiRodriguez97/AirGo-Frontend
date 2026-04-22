package com.airgo.airgoApp.features.profile.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BadgesAndActivitiesZoneTest {
    @get:Rule
    val composeTestRule = createComposeRule()

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
            .onNodeWithText("Badges", substring = true, ignoreCase = true)
            .assertExists()
    }
}
