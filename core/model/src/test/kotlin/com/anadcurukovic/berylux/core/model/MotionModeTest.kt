package com.anadcurukovic.berylux.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MotionModeTest {
    @Test
    fun `following the phone is calm only when the phone removes animations`() {
        assertThat(MotionMode.FOLLOW_PHONE.isReduced(phoneRemovesAnimations = true)).isTrue()
        assertThat(MotionMode.FOLLOW_PHONE.isReduced(phoneRemovesAnimations = false)).isFalse()
    }

    @Test
    fun `full and reduced don't ask the phone`() {
        assertThat(MotionMode.FULL.isReduced(phoneRemovesAnimations = true)).isFalse()
        assertThat(MotionMode.REDUCED.isReduced(phoneRemovesAnimations = false)).isTrue()
    }

    @Test
    fun `a new install follows the phone`() {
        assertThat(UserSettings().motion).isEqualTo(MotionMode.FOLLOW_PHONE)
    }
}
