package com.optimusprime.orbit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Deliberately not one border-radius applied to everything. Cards get a calm,
 * soft rectangle; sheets that rise from the bottom get rounded top corners
 * only, so they read as sliding up rather than floating; small chips get a
 * tighter radius so they don't look like tiny pills.
 */
object OrbitShapes {
    val card = RoundedCornerShape(20.dp)
    val cardSmall = RoundedCornerShape(14.dp)
    val chip = RoundedCornerShape(10.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill = RoundedCornerShape(50)
    val button = RoundedCornerShape(16.dp)
}
