package com.anadcurukovic.berylux.core.designsystem

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** A case card: the top right and bottom left corners cut off. */
val CaseCardShape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)

/** Buttons, chips and stamps: square, with the edge barely softened. */
val StampShape = RoundedCornerShape(2.dp)

internal val BeryluxShapes = Shapes(
    extraSmall = StampShape,
    small = StampShape,
    medium = CaseCardShape,
    large = CaseCardShape,
    extraLarge = StampShape,
)
