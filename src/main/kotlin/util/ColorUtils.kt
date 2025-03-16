package util

import kotlin.math.roundToInt

fun lightenColor(hex: String, factor: Float): String {
    // Ensure the factor is between 0 and 1
    val clampedFactor = factor.coerceIn(0f, 1f)

    // Parse the hex color
    val color = hex.removePrefix("#").toInt(16)
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF

    // Lighten each color component
    val newR = (r + (255 - r) * clampedFactor).roundToInt()
    val newG = (g + (255 - g) * clampedFactor).roundToInt()
    val newB = (b + (255 - b) * clampedFactor).roundToInt()

    // Combine the new color components back into a hex string
    return String.format("#%02X%02X%02X", newR, newG, newB)
}