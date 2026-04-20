package com.airgo.airgoApp.ui.main.ui

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry

import com.airgo.airgoApp.features.shell.mainShellScreen
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class MainShellScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // Contexto de la aplicación para obtener recursos
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Test en INGLÉS
     */
    @Test
    fun bottomNavigation_displaysEnglishLabels() {
        setupMainShellContent("en")

        // Verificamos strings en inglés
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Social").assertIsDisplayed()
        composeTestRule.onNodeWithText("Activity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calendar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    /**
     * Test en CASTELLANO
     */
    @Test
    fun bottomNavigation_displaysSpanishLabels() {
        setupMainShellContent("es")

        // Verificamos strings en español
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Social").assertIsDisplayed()
        composeTestRule.onNodeWithText("Actividad").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calendario").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    /**
     * Test en CATALÁN
     */
    @Test
    fun bottomNavigation_displaysCatalanLabels() {
        setupMainShellContent("ca")

        // Verificamos strings en catalán
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Social").assertIsDisplayed()
        composeTestRule.onNodeWithText("Activitat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calendari").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    // --- Funciones de ayuda ---

    /**
     * Configura el contenido de la pantalla forzando un Locale específico
     */
    private fun setupMainShellContent(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        // Creamos una nueva configuración con el idioma deseado
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        // Creamos un contexto específico que use esa configuración
        val localizedContext = context.createConfigurationContext(config)

        composeTestRule.setContent {
            // Inyectamos el contexto localizado en el árbol de Compose
            CompositionLocalProvider(LocalContext provides localizedContext) {
                mainShellScreen(
                    email = "test@airgo.com",
                    pictureUrl = null,
                    onLogout = {}
                )
            }
        }
    }
}
