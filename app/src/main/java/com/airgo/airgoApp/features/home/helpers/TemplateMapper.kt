package com.airgo.airgoApp.features.home.helpers

import com.airgo.airgoApp.R

object TemplateMapper {
    private val titlesMap =
        mapOf(
            // RUNNING
            "RUN_EASY_1" to R.string.template_run_easy_1_title,
            "RUN_EASY_2" to R.string.template_run_easy_2_title,
            "RUN_EASY_3" to R.string.template_run_easy_3_title,
            "RUN_MED_1" to R.string.template_run_med_1_title,
            "RUN_MED_2" to R.string.template_run_med_2_title,
            "RUN_MED_3" to R.string.template_run_med_3_title,
            "RUN_HARD_1" to R.string.template_run_hard_1_title,
            "RUN_HARD_2" to R.string.template_run_hard_2_title,
            "RUN_HARD_3" to R.string.template_run_hard_3_title,
            // CICLISMO
            "BIKE_EASY_1" to R.string.template_bike_easy_1_title,
            "BIKE_EASY_2" to R.string.template_bike_easy_2_title,
            "BIKE_EASY_3" to R.string.template_bike_easy_3_title,
            "BIKE_MED_1" to R.string.template_bike_med_1_title,
            "BIKE_MED_2" to R.string.template_bike_med_2_title,
            "BIKE_MED_3" to R.string.template_bike_med_3_title,
            "BIKE_HARD_1" to R.string.template_bike_hard_1_title,
            "BIKE_HARD_2" to R.string.template_bike_hard_2_title,
            "BIKE_HARD_3" to R.string.template_bike_hard_3_title,
        )

    private val descsMap =
        mapOf(
            // RUNNING
            "RUN_EASY_1" to R.string.template_run_easy_1_desc,
            "RUN_EASY_2" to R.string.template_run_easy_2_desc,
            "RUN_EASY_3" to R.string.template_run_easy_3_desc,
            "RUN_MED_1" to R.string.template_run_med_1_desc,
            "RUN_MED_2" to R.string.template_run_med_2_desc,
            "RUN_MED_3" to R.string.template_run_med_3_desc,
            "RUN_HARD_1" to R.string.template_run_hard_1_desc,
            "RUN_HARD_2" to R.string.template_run_hard_2_desc,
            "RUN_HARD_3" to R.string.template_run_hard_3_desc,
            // CICLISMO
            "BIKE_EASY_1" to R.string.template_bike_easy_1_desc,
            "BIKE_EASY_2" to R.string.template_bike_easy_2_desc,
            "BIKE_EASY_3" to R.string.template_bike_easy_3_desc,
            "BIKE_MED_1" to R.string.template_bike_med_1_desc,
            "BIKE_MED_2" to R.string.template_bike_med_2_desc,
            "BIKE_MED_3" to R.string.template_bike_med_3_desc,
            "BIKE_HARD_1" to R.string.template_bike_hard_1_desc,
            "BIKE_HARD_2" to R.string.template_bike_hard_2_desc,
            "BIKE_HARD_3" to R.string.template_bike_hard_3_desc,
        )

    fun getTitleResId(templateCode: String): Int = titlesMap[templateCode] ?: R.string.template_default_title

    fun getDescResId(templateCode: String): Int = descsMap[templateCode] ?: R.string.template_default_desc
}
