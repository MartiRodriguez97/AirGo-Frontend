package com.airgo.airgoApp.features.profile.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.profile.domain.ProfileUiState
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteAccountSectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // ─── deleteAccountSection ───────────────────────────────────────────────

    @Test
    fun deleteAccountSection_idleState_showsDeleteButton() {
        composeTestRule.setContent {
            deleteAccountSection(
                uiState = ProfileUiState.Idle,
                onDeleteClicked = {},
            )
        }
        composeTestRule.onNodeWithTag("delete_account_button").assertIsDisplayed()
    }

    @Test
    fun deleteAccountSection_loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            deleteAccountSection(
                uiState = ProfileUiState.Loading,
                onDeleteClicked = {},
            )
        }
        composeTestRule
            .onNodeWithContentDescription("CircularProgressIndicator", substring = true)
            .assertExists()
        composeTestRule.onNodeWithTag("delete_account_button").assertDoesNotExist()
    }

    @Test
    fun deleteAccountSection_errorState_showsErrorMessage() {
        val errorMessage = "Error al eliminar la cuenta"
        composeTestRule.setContent {
            deleteAccountSection(
                uiState = ProfileUiState.Error(UiText.DynamicString(errorMessage)),
                onDeleteClicked = {},
            )
        }
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun deleteAccountSection_onDeleteClicked_callsCallback() {
        val onDeleteClicked: () -> Unit = mockk(relaxed = true)

        composeTestRule.setContent {
            deleteAccountSection(
                uiState = ProfileUiState.Idle,
                onDeleteClicked = onDeleteClicked,
            )
        }
        composeTestRule.onNodeWithTag("delete_account_button").performClick()
        verify(exactly = 1) { onDeleteClicked() }
    }

    // ─── deleteAccountButton ────────────────────────────────────────────────

    @Test
    fun deleteAccountButton_isDisplayed() {
        composeTestRule.setContent {
            deleteAccountButton(onDeleteClicked = {})
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun deleteAccountButton_onClick_firesCallback() {
        val onClick: () -> Unit = mockk(relaxed = true)

        composeTestRule.setContent {
            deleteAccountButton(onDeleteClicked = onClick)
        }
        composeTestRule.onRoot().performClick()
        verify(exactly = 1) { onClick() }
    }

    // ─── deleteAccountDialog ────────────────────────────────────────────────

    @Test
    fun deleteAccountDialog_isDisplayed() {
        composeTestRule.setContent {
            deleteAccountDialog(onDismiss = {}, onConfirm = {})
        }
        composeTestRule
            .onNodeWithText("Eliminar", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun deleteAccountDialog_confirmButton_callsOnConfirm() {
        val onConfirm: () -> Unit = mockk(relaxed = true)

        composeTestRule.setContent {
            deleteAccountDialog(onDismiss = {}, onConfirm = onConfirm)
        }
        composeTestRule
            .onNodeWithText("Confirmar", substring = true, ignoreCase = true)
            .performClick()

        verify(exactly = 1) { onConfirm() }
    }

    @Test
    fun deleteAccountDialog_dismissButton_callsOnDismiss() {
        val onDismiss: () -> Unit = mockk(relaxed = true)

        composeTestRule.setContent {
            deleteAccountDialog(onDismiss = onDismiss, onConfirm = {})
        }
        composeTestRule
            .onNodeWithText("Cancelar", substring = true, ignoreCase = true)
            .performClick()

        verify(exactly = 1) { onDismiss() }
    }

    @Test
    fun deleteAccountDialog_onDismissRequest_callsOnDismiss() {
        val onDismiss: () -> Unit = mockk(relaxed = true)

        composeTestRule.setContent {
            deleteAccountDialog(onDismiss = onDismiss, onConfirm = {})
        }
        verify(atLeast = 1) { onDismiss() }
    }
}
