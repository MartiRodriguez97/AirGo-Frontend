package com.airgo.airgoApp.core.utils.translation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.airgo.airgoApp.R

object TranslationMapper {
    fun getBadgeNameResId(backendName: String): Int? =
        when (backendName) {
            "Iniciació a la constància" -> R.string.badge_constancy_name
            "De passeig" -> R.string.badge_walk_name
            "Benvingut a AirGo!" -> R.string.badge_welcome_name
            else -> null
        }

    fun getBadgeDescriptionResId(backendName: String): Int? =
        when (backendName) {
            "Iniciació a la constància" -> R.string.badge_constancy_desc
            "De passeig" -> R.string.badge_walk_desc
            "Benvingut a AirGo!" -> R.string.badge_welcome_desc
            else -> null
        }

    // Para los títulos en EditTitolScreen, si también vienen del backend
    fun getTitolNameResId(backendName: String): Int? =
        when (backendName) {
            // Añadir aquí si hay títulos específicos que no coinciden con las insignias
            "Iniciació a la constància" -> R.string.badge_constancy_name
            "De passeig" -> R.string.badge_walk_name
            "Benvingut a AirGo!" -> R.string.badge_welcome_name
            else -> null
        }

    fun getTitolDescriptionResId(backendName: String): Int? =
        when (backendName) {
            "Iniciació a la constància" -> R.string.badge_constancy_desc
            "De passeig" -> R.string.badge_walk_desc
            "Benvingut a AirGo!" -> R.string.badge_welcome_desc
            else -> null
        }
}

@Composable
fun translateBadgeName(backendName: String): String {
    val resId = TranslationMapper.getBadgeNameResId(backendName)
    return resId?.let { stringResource(it) } ?: backendName
}

@Composable
fun translateBadgeDescription(backendName: String): String {
    val resId = TranslationMapper.getBadgeDescriptionResId(backendName)
    return resId?.let { stringResource(it) } ?: backendName
}

@Composable
fun translateTitolName(backendName: String): String {
    val resId = TranslationMapper.getTitolNameResId(backendName)
    return resId?.let { stringResource(it) } ?: backendName
}

@Composable
fun translateTitolDescription(backendName: String): String {
    val resId = TranslationMapper.getTitolDescriptionResId(backendName)
    return resId?.let { stringResource(it) } ?: backendName
}
