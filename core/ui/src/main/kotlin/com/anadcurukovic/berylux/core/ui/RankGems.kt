package com.anadcurukovic.berylux.core.ui

import com.anadcurukovic.berylux.core.designsystem.component.GemPalette
import com.anadcurukovic.berylux.core.designsystem.component.GemPalettes
import com.anadcurukovic.berylux.core.model.GemRank

/** Each rank's gem, in its mineral's color. */
fun GemRank.gemPalette(): GemPalette = when (this) {
    GemRank.GOSHENITE -> GemPalettes.Goshenite
    GemRank.HELIODOR -> GemPalettes.Heliodor
    GemRank.MORGANITE -> GemPalettes.Morganite
    GemRank.AQUAMARINE -> GemPalettes.Aquamarine
    GemRank.EMERALD -> GemPalettes.Emerald
    GemRank.RED_BERYL -> GemPalettes.RedBeryl
}
