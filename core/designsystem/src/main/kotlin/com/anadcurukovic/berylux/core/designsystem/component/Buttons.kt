package com.anadcurukovic.berylux.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.Spacing
import com.anadcurukovic.berylux.core.designsystem.StampShape

/** The main action: a filled red stamp. */
@Composable
fun StampButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = Berylux.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = StampShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.stamp, contentColor = colors.onStamp),
        contentPadding = ButtonPadding,
    ) { Text(text, style = BeryluxType.Button) }
}

/**
 * The second action, or one that's done: a red outline. On paper it's the stamp's deep red; on
 * the desk pass [onDesk], and it takes the bright red that reads there.
 */
@Composable
fun StampOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDesk: Boolean = false,
) {
    val colors = Berylux.colors
    val red = if (onDesk) colors.accent else colors.stamp
    val off = if (onDesk) colors.onBackgroundFaded else colors.inkFaded
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = StampShape,
        border = BorderStroke(1.dp, if (enabled) red else off),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = red, disabledContentColor = off),
        contentPadding = ButtonPadding,
    ) { Text(text, style = BeryluxType.Button) }
}

/**
 * The bottom line of a card: [lead] on the left (Lux, a note), the buttons on the right. When
 * the card is too narrow for both, the buttons move to their own line, still on the right, and
 * never past the card's edge.
 */
@Composable
fun CardActions(
    modifier: Modifier = Modifier,
    lead: @Composable FlowRowScope.() -> Unit = {},
    actions: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        lead()
        actions()
    }
}

private val ButtonPadding = PaddingValues(horizontal = Spacing.Large, vertical = Spacing.Small)
