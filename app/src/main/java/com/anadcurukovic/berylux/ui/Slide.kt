package com.anadcurukovic.berylux.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import com.anadcurukovic.berylux.core.navigation.tab

/** How the screen changes, from the tab it leaves and the tab it lands on. */
internal enum class Slide {
    /** A new screen in the same tab slides over from the right. */
    OVER,

    /** Back in the same tab: the screen on top slides out to the right. */
    BACK,

    /** To a tab further right: both move left together. */
    TAB_RIGHT,

    /** To a tab further left: both move right together. */
    TAB_LEFT,
}

/** [fromTab] and [toTab] are tab positions, left to right; null when a screen has none. */
internal fun slideFor(fromTab: Int?, toTab: Int?, back: Boolean): Slide = when {
    fromTab != null && toTab != null && fromTab != toTab -> if (toTab > fromTab) Slide.TAB_RIGHT else Slide.TAB_LEFT
    back -> Slide.BACK
    else -> Slide.OVER
}

/** The motion from the scene on screen to the next one, read from the tabs of their top entries. */
internal fun AnimatedContentTransitionScope<Scene<NavKey>>.sceneTransition(back: Boolean, calm: Boolean) =
    transitionFor(slideFor(initialState.entries.last().tab, targetState.entries.last().tab, back), calm)

/** The motion for [slide]; with [calm] on, a short crossfade with no movement instead. */
internal fun transitionFor(slide: Slide, calm: Boolean): ContentTransform {
    if (calm) return fadeIn(tween(CALM_MS)) togetherWith fadeOut(tween(CALM_MS))
    val move = tween<IntOffset>(SLIDE_MS, easing = FastOutSlowInEasing)
    val fade = tween<Float>(SLIDE_MS, easing = FastOutSlowInEasing)
    // The screen underneath shifts a little and dims, the way a file slides over another.
    val underLeaves = slideOutHorizontally(move) { -it / UNDER_SHIFT } + fadeOut(fade, targetAlpha = UNDER_ALPHA)
    val underReturns = slideInHorizontally(move) { -it / UNDER_SHIFT } + fadeIn(fade, initialAlpha = UNDER_ALPHA)
    return when (slide) {
        Slide.OVER -> slideInHorizontally(move) { it } togetherWith underLeaves
        // The screen leaving stays on top until it's gone.
        Slide.BACK -> (underReturns togetherWith slideOutHorizontally(move) { it }).apply { targetContentZIndex = -1f }
        Slide.TAB_RIGHT -> slideInHorizontally(move) { it } togetherWith slideOutHorizontally(move) { -it }
        Slide.TAB_LEFT -> slideInHorizontally(move) { -it } togetherWith slideOutHorizontally(move) { it }
    }
}

private const val SLIDE_MS = 300
private const val CALM_MS = 150

/** The screen underneath moves a quarter of the width and keeps half its strength. */
private const val UNDER_SHIFT = 4
private const val UNDER_ALPHA = 0.5f
