package com.airgo.airgoApp.features.home.helpers

import com.airgo.airgoApp.R

object TemplateMapper {
    // agafem el nom del template
    fun getTitleResId(templateCode: String): Int =
        when (templateCode) {
            "RUN_EASY_1" -> R.string.template_run_easy_1_title
            "RUN_EASY_2" -> R.string.template_run_easy_2_title
            "RUN_MED_1" -> R.string.template_run_med_1_title
            "RUN_MED_2" -> R.string.template_run_med_2_title
            "RUN_HARD_1" -> R.string.template_run_hard_1_title
            "RUN_HARD_2" -> R.string.template_run_hard_2_title

            "BIKE_EASY_1" -> R.string.template_bike_easy_1_title
            "BIKE_EASY_2" -> R.string.template_bike_easy_2_title
            "BIKE_MED_1" -> R.string.template_bike_med_1_title
            "BIKE_MED_2" -> R.string.template_bike_med_2_title
            "BIKE_HARD_1" -> R.string.template_bike_hard_1_title
            "BIKE_HARD_2" -> R.string.template_bike_hard_2_title
            else -> R.string.template_default_title
        }

    fun getDescResId(templateCode: String): Int =
        when (templateCode) {
            "RUN_EASY_1" -> R.string.template_run_easy_1_desc
            "RUN_EASY_2" -> R.string.template_run_easy_2_desc
            "RUN_MED_1" -> R.string.template_run_med_1_desc
            "RUN_MED_2" -> R.string.template_run_med_2_desc
            "RUN_HARD_1" -> R.string.template_run_hard_1_desc
            "RUN_HARD_2" -> R.string.template_run_hard_2_desc

            "BIKE_EASY_1" -> R.string.template_bike_easy_1_desc
            "BIKE_EASY_2" -> R.string.template_bike_easy_2_desc
            "BIKE_MED_1" -> R.string.template_bike_med_1_desc
            "BIKE_MED_2" -> R.string.template_bike_med_2_desc
            "BIKE_HARD_1" -> R.string.template_bike_hard_1_desc
            "BIKE_HARD_2" -> R.string.template_bike_hard_2_desc
            else -> R.string.template_default_desc
        }
}
