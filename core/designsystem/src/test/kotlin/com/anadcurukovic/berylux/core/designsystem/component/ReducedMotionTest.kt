package com.anadcurukovic.berylux.core.designsystem.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The stamp with full and with reduced motion, timed on the test clock. */
@RunWith(RobolectricTestRunner::class)
class ReducedMotionTest {
    @get:Rule
    val compose = createComposeRule()

    private val haptics = object : HapticFeedback {
        val felt = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            felt += hapticFeedbackType
        }
    }

    private fun landStamp(reducedMotion: Boolean) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            BeryluxTheme(reducedMotion = reducedMotion) {
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    ClosedStamp(label = "CLOSED", date = "08 NOV 2026", land = true)
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `with full motion the haptic comes when the stamp lands, after its 180 ms drop`() {
        landStamp(reducedMotion = false)
        assertThat(haptics.felt).isEmpty()

        compose.mainClock.advanceTimeBy(DROP_MS + FRAME_MS)
        assertThat(haptics.felt).containsExactly(HapticFeedbackType.Confirm)
    }

    @Test
    fun `with reduced motion the stamp is down at once, and the haptic still comes`() {
        landStamp(reducedMotion = true)
        assertThat(haptics.felt).containsExactly(HapticFeedbackType.Confirm)
    }

    private companion object {
        const val DROP_MS = 180L
        const val FRAME_MS = 16L
    }
}
