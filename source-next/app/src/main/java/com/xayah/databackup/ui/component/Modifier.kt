package com.xayah.databackup.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

fun Modifier.horizontalFadingEdges(
    scrollState: ScrollState,
    fadingEdge: Dp = 72.dp,
): Modifier = fadingEdges(Direction.HORIZONTAL, scrollState, fadingEdge)

fun Modifier.verticalFadingEdges(
    scrollState: ScrollState,
    fadingEdge: Dp = 72.dp,
): Modifier = fadingEdges(Direction.VERTICAL, scrollState, fadingEdge)

private enum class Direction {
    HORIZONTAL,
    VERTICAL,
}

/**
 * @see <a href="https://medium.com/@helmersebastian/fading-edges-modifier-in-jetpack-compose-af94159fdf1f">Fading Edges Modifier in Jetpack Compose</a>
 */
private fun Modifier.fadingEdges(
    direction: Direction,
    scrollState: ScrollState,
    fadingEdge: Dp,
): Modifier = this.then(
    Modifier
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val startColors = listOf(Color.Transparent, Color.Black)
            var start = scrollState.value.toFloat()
            var end = start + min(fadingEdge.toPx(), start)
            drawRect(
                brush = when (direction) {
                    Direction.HORIZONTAL -> Brush.horizontalGradient(
                        colors = startColors,
                        startX = start,
                        endX = end
                    )

                    Direction.VERTICAL -> Brush.verticalGradient(
                        colors = startColors,
                        startY = start,
                        endY = end
                    )
                },
                blendMode = BlendMode.DstIn
            )

            val endColors = listOf(Color.Black, Color.Transparent)
            val edgeSize = min(fadingEdge.toPx(), scrollState.maxValue.toFloat() - scrollState.value)
            end = when (direction) {
                Direction.HORIZONTAL -> size.width
                Direction.VERTICAL -> size.height
            } - scrollState.maxValue + scrollState.value
            start = end - edgeSize
            if (edgeSize != 0f) {
                drawRect(
                    brush = when (direction) {
                        Direction.HORIZONTAL -> Brush.horizontalGradient(
                            colors = endColors,
                            startX = start,
                            endX = end
                        )

                        Direction.VERTICAL -> Brush.verticalGradient(
                            colors = endColors,
                            startY = start,
                            endY = end
                        )
                    },
                    blendMode = BlendMode.DstIn
                )
            }
        }
)
