package com.airgo.airgoApp.features.settings.account.username.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class UsernameViewModelTest {
    private lateinit var viewModel: UsernameViewModel
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        viewModel = UsernameViewModel()
    }

    @Test
    fun changeUsername_emptyField_showsErrorInSpanish() {
        // 1. Forzamos el idioma a Castellano
        setLocale("es")

        // 2. Ejecutamos la acción que dispara el error
        viewModel.changeUsername("")

        // 3. Obtenemos el estado y resolvemos el string
        val state = viewModel.uiState.value
        if (state is UsernameUiState.Error) {
            val messageText = state.message.asString(context)

            // 4. Verificamos que el texto sea el de tu strings.xml (es)
            // Cambia "El nombre de usuario no puede estar vacío" por tu traducción real
            assertEquals("El nombre de usuario no puede estar vacío.", messageText)
        } else {
            fail("El estado debería ser Error")
        }
    }

    @Test
    fun changeUsername_emptyField_showsErrorInCatalan() {
        // 1. Forzamos el idioma a Catalán
        setLocale("ca")

        // 2. Acción
        viewModel.changeUsername("")

        // 3. Verificación
        val state = viewModel.uiState.value
        if (state is UsernameUiState.Error) {
            val messageText = state.message.asString(context)

            // Cambia esto por tu traducción en values-ca/strings.xml
            assertEquals("El nom d'usuari no pot estar buit.", messageText)
        } else {
            fail("El estado debería ser Error")
        }
    }

    @Test
    fun changeUsername_emptyField_showsErrorInDefaultEnglish() {
        // 1. Forzamos el idioma a Inglés (o default)
        setLocale("en")

        viewModel.changeUsername("")

        val state = viewModel.uiState.value
        if (state is UsernameUiState.Error) {
            val messageText = state.message.asString(context)

            // Cambia esto por tu traducción en values/strings.xml
            assertEquals("Username cannot be empty.", messageText)
        } else {
            fail("El estado debería ser Error")
        }
    }

    // Función auxiliar para cambiar el idioma durante el test
    private fun setLocale(language: String) {
        // Usamos java.util.Locale
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration

        // Seteamos el locale en la configuración de Android
        config.setLocale(locale)

        // Actualizamos el contexto para que los próximos getString() lean el XML correcto
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
