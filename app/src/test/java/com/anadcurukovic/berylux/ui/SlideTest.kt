package com.anadcurukovic.berylux.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Tabs from left to right: Today 0, Cases 1, Catalog 2, Progress 3. */
class SlideTest {
    @Test
    fun `a new screen in the same tab slides over, and back slides it away`() {
        // Catalog to a category, then back.
        assertThat(slideFor(fromTab = 2, toTab = 2, back = false)).isEqualTo(Slide.OVER)
        assertThat(slideFor(fromTab = 2, toTab = 2, back = true)).isEqualTo(Slide.BACK)
    }

    @Test
    fun `tabs push in tab order`() {
        assertThat(slideFor(fromTab = 0, toTab = 1, back = false)).isEqualTo(Slide.TAB_RIGHT)
        assertThat(slideFor(fromTab = 3, toTab = 1, back = false)).isEqualTo(Slide.TAB_LEFT)
    }

    @Test
    fun `back from another tab to Today pushes left, like tapping Today`() {
        assertThat(slideFor(fromTab = 2, toTab = 0, back = true)).isEqualTo(Slide.TAB_LEFT)
    }

    @Test
    fun `a screen without a tab slides over`() {
        assertThat(slideFor(fromTab = null, toTab = 1, back = false)).isEqualTo(Slide.OVER)
    }
}
