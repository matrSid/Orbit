package com.optimusprime.orbit.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Orbit's palette is built around one image: a desk lamp left on after dark.
 * Warm near-black instead of true black or cool slate, a brass glow instead
 * of a stock Material purple/blue, and just enough green and clay to mark
 * "good" and "away" without reaching for neon.
 */
object OrbitColors {
    // Grounds
    val ink = Color(0xFF171512)          // app background — warm charcoal, not pure black
    val surface = Color(0xFF1D1A15)      // resting card surface
    val surfaceRaised = Color(0xFF262119) // sheets, dialogs, anything that sits above a card
    val hairline = Color(0xFF3A3527)     // the only "border", used instead of drop shadows

    // Ink on paper
    val bone = Color(0xFFECE3D0)         // primary text — warm off-white
    val ash = Color(0xFF9C9382)          // secondary text
    val ashFaint = Color(0xFF6B6354)     // placeholders, disabled, timestamps

    // The flame
    val brass = Color(0xFFD6A24A)        // primary accent — active timer, CTAs, the orbit mark
    val brassSoft = Color(0xFF4A3A22)    // brass at rest — unfilled ring track, subtle fills
    val brassDim = Color(0xFF8C6C34)     // brass text on dark surfaces where full brass is too loud

    // States
    val moss = Color(0xFF7C9473)         // goals done, positive delta
    val mossSoft = Color(0xFF2A3226)
    val oxide = Color(0xFFB05A3F)        // away / paused-by-camera, a warning without being alarm-red
    val oxideSoft = Color(0xFF3A2620)
}
