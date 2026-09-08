package com.shadowuro.remindrop.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp

internal enum class CardPosition { SINGLE, FIRST, MIDDLE, LAST }

internal fun cardPosition(index: Int, count: Int): CardPosition = when {
    count <= 1 -> CardPosition.SINGLE
    index == 0 -> CardPosition.FIRST
    index == count - 1 -> CardPosition.LAST
    else -> CardPosition.MIDDLE
}

@Composable
internal fun groupedShape(position: CardPosition, pressed: Boolean = false): RoundedCornerShape {
    val outer by animateDpAsState(if (pressed) 14.dp else 22.dp, label = "outerCorner")
    val inner by animateDpAsState(if (pressed) 14.dp else 6.dp, label = "innerCorner")
    val top = if (position == CardPosition.FIRST || position == CardPosition.SINGLE) outer else inner
    val bottom = if (position == CardPosition.LAST || position == CardPosition.SINGLE) outer else inner
    return RoundedCornerShape(
        topStart = top,
        topEnd = top,
        bottomStart = bottom,
        bottomEnd = bottom,
    )
}
