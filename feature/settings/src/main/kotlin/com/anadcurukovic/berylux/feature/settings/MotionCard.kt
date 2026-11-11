package com.anadcurukovic.berylux.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.component.StampButton
import com.anadcurukovic.berylux.core.designsystem.component.StampOutlineButton
import com.anadcurukovic.berylux.core.model.MotionMode

/** Motion: follow the phone, full or reduced, picked like the theme. */
@Composable
internal fun MotionCard(selected: MotionMode, onMotion: (MotionMode) -> Unit) {
    CaseCard {
        Text(stringResource(R.string.settings_motion_title), style = BeryluxType.Title)
        FlowRow(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MotionMode.entries.forEach { motion ->
                val name = stringResource(motion.labelRes())
                // Like the theme: the picked one is a filled stamp, the others outlines.
                if (motion == selected) {
                    StampButton(name, onClick = {}, modifier = Modifier.semantics { this.selected = true })
                } else {
                    StampOutlineButton(name, onClick = { onMotion(motion) })
                }
            }
        }
        Text(
            text = stringResource(R.string.settings_motion_note),
            style = BeryluxType.Text,
            color = Berylux.colors.inkFaded,
        )
    }
}

private fun MotionMode.labelRes(): Int = when (this) {
    MotionMode.FOLLOW_PHONE -> R.string.settings_motion_phone
    MotionMode.FULL -> R.string.settings_motion_full
    MotionMode.REDUCED -> R.string.settings_motion_reduced
}
