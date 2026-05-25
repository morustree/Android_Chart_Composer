package com.rma.android_chart_composer.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.rma.android_chart_composer.specs.VerticalPosition
import com.rma.android_chart_composer.specs.HorizontalPosition

/**
 * Shared drawing components for different types of charts.
 */

fun DrawScope.drawGrid(
    linhasVerticais: Int,
    linhasHorizontais: Int,
    color: Color,
    thickness: Float = 1.dp.toPx()
) {
    // Linhas Verticais da Grade
    val espacoVertical = size.width / (linhasVerticais + 1)
    for (i in 0..linhasVerticais + 1) {
        val x = espacoVertical * i
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = thickness
        )
    }

    // Linhas Horizontais da Grade
    val espacoHorizontal = size.height / (linhasHorizontais + 1)
    for (i in 0..linhasHorizontais + 1) {
        val y = espacoHorizontal * i
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = thickness
        )
    }
}

fun DrawScope.drawXLine(color: Color, xPosition: VerticalPosition, thickness: Float = 2.dp.toPx()) {
    val y = if (xPosition == VerticalPosition.Bottom) size.height else 0f
    
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = thickness
    )
}

fun DrawScope.drawYLine(color: Color, yPosition: HorizontalPosition, thickness: Float = 2.dp.toPx()) {
    val x = if (yPosition == HorizontalPosition.Right) size.width else 0f

    drawLine(
        color = color,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = thickness
    )
}
