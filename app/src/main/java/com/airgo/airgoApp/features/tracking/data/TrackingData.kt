package com.airgo.airgoApp.features.tracking.data
import androidx.compose.ui.graphics.Color
import com.airgo.airgoApp.R

data class TrackingData(
    val distanceMeters: Double = 0.0,
    val elapsedSeconds: Long = 0L,
    val speedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
)

private const val COLOR_RED = 0xFFD32F2F
private const val COLOR_ORANGE = (0xFFF57C00)
private const val COLOR_YELLOW = (0xFFFBC02D)
private const val COLOR_LIGHT_GREEN = (0xFF689F38)
private const val COLOR_DARK_GREEN = (0xFF2E7D32)
private const val LEVEL_ONE = 1
private const val LEVEL_TWO = 2
private const val LEVEL_THREE = 3
private const val LEVEL_FOUR = 4
private const val LEVEL_FIVE = 5

enum class ActivitySensation(
    val value: Int,
    val color: Color,
    val labelRes: Int,
) {
    VERY_BAD(LEVEL_ONE, Color(COLOR_RED), R.string.sensation_very_bad),
    BAD(LEVEL_TWO, Color(COLOR_ORANGE), R.string.sensation_bad),
    NORMAL(LEVEL_THREE, Color(COLOR_YELLOW), R.string.sensation_normal),
    GOOD(LEVEL_FOUR, Color(COLOR_LIGHT_GREEN), R.string.sensation_good),
    VERY_GOOD(LEVEL_FIVE, Color(COLOR_DARK_GREEN), R.string.sensation_very_good),
}
