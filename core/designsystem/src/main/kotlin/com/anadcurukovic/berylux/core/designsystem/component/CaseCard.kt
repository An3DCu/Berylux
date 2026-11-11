package com.anadcurukovic.berylux.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.CaseCardShape
import com.anadcurukovic.berylux.core.designsystem.R
import com.anadcurukovic.berylux.core.designsystem.Spacing

/**
 * A sheet from the case file: paper with two corners cut, ink for its text. [caseNumber] is the
 * case's id, shown as "CASE NO. 0412"; a challenge that isn't a case yet has none. A [shaded]
 * sheet is on the darker paper, for one that isn't open to the player yet.
 */
@Composable
fun CaseCard(
    modifier: Modifier = Modifier,
    caseNumber: Long? = null,
    shaded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Berylux.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (shaded) colors.paperShade else colors.paper, CaseCardShape)
            .padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
    ) {
        if (caseNumber != null) {
            Text(
                text = stringResource(R.string.designsystem_case_number, caseNumber),
                style = BeryluxType.Label,
                color = colors.stamp,
            )
        }
        CompositionLocalProvider(LocalContentColor provides colors.ink) { content() }
    }
}
