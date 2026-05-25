package com.airgo.airgoApp.core.utils.translation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.airgo.airgoApp.R

object TranslationMapper {
    private val badgeMap =
        mapOf(
            "Benvingut a AirGo!" to (R.string.badge_welcome_name to R.string.badge_welcome_desc),
            "Llegenda AirGo" to (R.string.badge_legend_name to R.string.badge_legend_desc),
            "Ambaixador AirGo" to (R.string.badge_ambassador_name to R.string.badge_ambassador_desc),
            "Elit verd" to (R.string.badge_elite_name to R.string.badge_elite_desc),
            "Atleta urbà" to (R.string.badge_athlete_name to R.string.badge_athlete_desc),
            "En forma" to (R.string.badge_fit_name to R.string.badge_fit_desc),
            "Primeres passes" to (R.string.badge_first_steps_name to R.string.badge_first_steps_desc),
            "Travessia catalana" to (R.string.badge_catalan_crossing_name to R.string.badge_catalan_crossing_desc),
            "Mestre del pedal" to (R.string.badge_pedal_master_name to R.string.badge_pedal_master_desc),
            "Centurió" to (R.string.badge_centurion_name to R.string.badge_centurion_desc),
            "Voltant Barcelona" to (R.string.badge_around_barcelona_name to R.string.badge_around_barcelona_desc),
            "Explorador urbà" to (R.string.badge_urban_explorer_name to R.string.badge_urban_explorer_desc),
            "Primers 10 quilòmetres" to (R.string.badge_first_10km_name to R.string.badge_first_10km_desc),
            "De passeig" to (R.string.badge_walk_name to R.string.badge_walk_desc),
            "Mig any de ratxa" to (R.string.badge_half_year_streak_name to R.string.badge_half_year_streak_desc),
            "Llegenda de la constància" to (
                R.string.badge_constancy_legend_name to R.string.badge_constancy_legend_desc
            ),
            "Un mes sense parar" to (R.string.badge_one_month_nonstop_name to R.string.badge_one_month_nonstop_desc),
            "Dues setmanes imparable" to (
                R.string.badge_two_weeks_unstoppable_name to R.string.badge_two_weeks_unstoppable_desc
            ),
            "Setmana de ferro" to (R.string.badge_iron_week_name to R.string.badge_iron_week_desc),
            "Iniciació a la constància" to (R.string.badge_constancy_name to R.string.badge_constancy_desc),
        )

    fun getBadgeNameResId(backendName: String): Int? = badgeMap[backendName]?.first

    fun getBadgeDescriptionResId(backendName: String): Int? = badgeMap[backendName]?.second

    fun getTitolNameResId(backendName: String): Int? = getBadgeNameResId(backendName)

    fun getTitolDescriptionResId(backendName: String): Int? = getBadgeDescriptionResId(backendName)
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
