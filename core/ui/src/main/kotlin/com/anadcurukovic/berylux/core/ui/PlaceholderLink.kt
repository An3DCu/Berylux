package com.anadcurukovic.berylux.core.ui

/** A link on a placeholder screen: a label and what tapping it does. */
data class PlaceholderLink(val label: String, val onClick: () -> Unit)
