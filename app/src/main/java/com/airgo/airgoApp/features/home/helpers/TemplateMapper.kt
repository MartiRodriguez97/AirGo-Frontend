package com.airgo.airgoApp.features.home.helpers

import com.airgo.airgoApp.R

object TemplateMapper {
    // agafem el nom del template
    fun getTitleResId(templateCode: String): Int =
        when (templateCode) {
            "WALK_EASY_1" -> R.string.template_walk_easy_1_title
            "RUN_FREE_1" -> R.string.template_run_free_title
            else -> R.string.template_default_title
        }

    fun getDescResId(templateCode: String): Int =
        when (templateCode) {
            "WALK_EASY_1" -> R.string.template_walk_easy_1_desc
            "RUN_FREE_1" -> R.string.template_run_free_desc
            else -> R.string.template_default_desc
        }
}
