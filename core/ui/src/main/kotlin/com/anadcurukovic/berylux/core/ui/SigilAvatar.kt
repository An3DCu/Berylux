package com.anadcurukovic.berylux.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anadcurukovic.berylux.core.domain.Sigil

/**
 * Draws the player's sigil from their seed. Plain for now; the character round gives it its look.
 */
@Composable
fun SigilAvatar(
    seed: Long,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val sigil = remember(seed) { Sigil.from(seed) }
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(size * MARGIN),
    ) {
        Canvas(modifier = Modifier.size(size * (1 - 2 * MARGIN))) {
            val cell = this.size.width / Sigil.SIZE
            val gap = cell * GAP
            for (row in 0 until Sigil.SIZE) {
                for (column in 0 until Sigil.SIZE) {
                    if (sigil.isOn(row, column)) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(column * cell + gap, row * cell + gap),
                            size = Size(cell - 2 * gap, cell - 2 * gap),
                            cornerRadius = CornerRadius(gap, gap),
                        )
                    }
                }
            }
        }
    }
}

/** The space around the pattern, as a share of the avatar's size. */
private const val MARGIN = 0.2f

/** The space between cells, as a share of a cell. */
private const val GAP = 0.125f

@Preview
@Composable
private fun SigilAvatarPreview() {
    MaterialTheme { SigilAvatar(seed = 1234, size = 96.dp) }
}
