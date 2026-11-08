package com.anadcurukovic.berylux.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.sp
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType

/** BERYLUX in red Bebas Neue, spaced wide. TalkBack reads [contentDescription] as a heading. */
@Composable
fun Wordmark(contentDescription: String, modifier: Modifier = Modifier) {
    Text(
        text = WORD,
        style = BeryluxType.Heading.copy(fontSize = SIZE.sp, lineHeight = SIZE.sp, letterSpacing = SPACING.sp),
        color = Berylux.colors.accent,
        modifier = modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
            heading()
        },
    )
}

private const val WORD = "BERYLUX"
private const val SIZE = 56
private const val SPACING = 4
