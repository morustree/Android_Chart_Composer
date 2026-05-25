package com.rma.android_chart_composer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.rma.android_chart_composer.components.drawGrid
import com.rma.android_chart_composer.components.drawXLine
import com.rma.android_chart_composer.components.drawYLine
import com.rma.android_chart_composer.specs.*
import org.apache.commons.math3.analysis.UnivariateFunction
import java.util.Locale
import kotlin.math.roundToInt

private val OffsetSaver = Saver<Offset, List<Float>>(
    save = { listOf(it.x, it.y) },
    restore = { Offset(it[0], it[1]) }
)

// Data Class para evitar alocações de objetos anônimos e classes internas
private data class SeriesDrawData(
    val offsets: List<Offset>,
    val originalSeries: ChartSeries,
    val pointColor: Color,
    val pointSize: Float,
    val pointShape: MarkerShape,
    val showLines: Boolean,
    val lineColor: Color,
    val lineThickness: Float,
    val linePath: Path?,
    val markerPath: Path?,
    val pathEffect: PathEffect?
)

// Auxiliar para compartilhar dados entre draw e input sem disparar recomposições
private class DrawDataRef {
    var data: List<SeriesDrawData> = emptyList()
}

@Composable
fun ScatterChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    specs: ChartSpecs = ChartSpecs()
) {
    val legendOffsetState = rememberSaveable(stateSaver = OffsetSaver) { mutableStateOf(specs.legend.initialOffset) }
    val isLegendVisibleState = rememberSaveable { mutableStateOf(specs.legend.isVisible) }
    val reopenButtonOffsetState = rememberSaveable(stateSaver = OffsetSaver) { mutableStateOf(Offset(-1f, -1f)) }
    var selectedPointInfo by remember(series) { mutableStateOf<Pair<Offset, ChartSeries>?>(null) }

    val resolvedGridColor = specs.grid.resolveColor()
    val resolvedAxisColor = specs.axis.resolveLineColor()
    val resolvedBgColor = specs.resolveBackgroundColor()
    val primaryFallback = MaterialTheme.colorScheme.primary

    // Resolve series styles
    val resolvedSeriesStyles = series.map { s ->
        val pointColor = s.pointSpecs.resolveColor(primaryFallback)
        val lineColor = s.lineSpecs.resolveColor(pointColor)
        Pair(pointColor, lineColor)
    }

    // Cache limits
    val limits = remember(series) {
        val allPoints = series.flatMap { it.data }
        if (allPoints.isEmpty()) null
        else {
            val minX = allPoints.minOf { it.x }
            val maxX = allPoints.maxOf { it.x }
            val minY = allPoints.minOf { it.y }
            val maxY = allPoints.maxOf { it.y }
            val deltaX = if (maxX - minX == 0f) 1f else (maxX - minX)
            val deltaY = if (maxY - minY == 0f) 1f else (maxY - minY)
            object {
                val minX = minX
                val maxX = maxX
                val minY = minY
                val maxY = maxY
                val deltaX = deltaX
                val deltaY = deltaY
            }
        }
    }

    // Steps
    val steps = remember(limits, specs.axis.xAxisLabels.step, specs.axis.yAxisLabels.step) {
        limits?.let {
            val xSteps = if (specs.axis.xAxisLabels.step > 0.0) ((it.maxX - it.minX) / specs.axis.xAxisLabels.step).toInt() else 5
            val ySteps = if (specs.axis.yAxisLabels.step > 0.0) ((it.maxY - it.minY) / specs.axis.yAxisLabels.step).toInt() else 5
            Pair(xSteps, ySteps)
        } ?: Pair(0, 0)
    }

    // Ref para compartilhar dados calculados com o pointerInput
    val drawDataRef = remember { DrawDataRef() }

    BoxWithConstraints(
        modifier = modifier
            .background(resolvedBgColor)
            .padding(12.dp) // External padding
            .pointerInput(Unit) {
                detectTapGestures { selectedPointInfo = null }
            }
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        Column(modifier = Modifier.fillMaxSize()) {
            // Title
            if (specs.title.text != null && specs.title.isVisible) {
                Text(
                    text = specs.title.text,
                    style = specs.title.style.toTextStyle(),
                    modifier = Modifier.fillMaxWidth().padding(specs.title.padding)
                )
            }

            // X Axis Title Top
            if (specs.axis.xAxisTitle.isVisible && specs.axis.xAxisTitle.text != null && specs.axis.xAxisTitle.verticalPosition == VerticalPosition.Top) {
                Text(
                    text = specs.axis.xAxisTitle.text,
                    style = specs.axis.xAxisTitle.style.toTextStyle(),
                    modifier = Modifier.fillMaxWidth().padding(specs.axis.xAxisTitle.padding)
                )
            }

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Y Axis
                if (specs.axis.yAxisTitle.isVisible && specs.axis.yAxisTitle.text != null && specs.axis.yAxisTitle.horizontalPosition == HorizontalPosition.Left) {
                    Box(
                        modifier = Modifier.width(specs.axis.yAxisTitle.width),
                        contentAlignment = Alignment.Center
                    ) {
                        AxisTitle(specs.axis.yAxisTitle)
                    }
                    Spacer(modifier = Modifier.width(specs.axis.yAxisTitle.padding))
                }
                
                if (specs.axis.yAxisLabels.isVisible && specs.axis.yAxisTitle.horizontalPosition == HorizontalPosition.Left) {
                    YAxisLabels(
                        minY = limits?.minY ?: 0f,
                        maxY = limits?.maxY ?: 0f,
                        steps = steps.second,
                        specs = specs.axis.yAxisLabels,
                        modifier = Modifier.width(specs.axis.yAxisLabels.width).padding(top = specs.chartPadding)
                    )
                    Spacer(modifier = Modifier.width(specs.axis.yAxisLabels.padding))
                }

                // Plot Area
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = specs.chartPadding, end = specs.chartPadding) 
                            .drawWithCache {
                                val currentLimits = limits ?: return@drawWithCache onDrawBehind {}

                                val seriesDrawData = series.mapIndexed { index, s ->
                                    val (pointColor, lineColor) = resolvedSeriesStyles[index]
                                    val scaledOffsets = s.data.map { p ->
                                        val px = ((p.x - currentLimits.minX) / currentLimits.deltaX) * size.width
                                        val py = size.height - (((p.y - currentLimits.minY) / currentLimits.deltaY) * size.height)
                                        Offset(px, py)
                                    }
                                    
                                    val linePath = if (s.showLines && scaledOffsets.isNotEmpty()) {
                                        Path().apply {
                                            moveTo(scaledOffsets[0].x, scaledOffsets[0].y)
                                            for (i in 1 until scaledOffsets.size) {
                                                lineTo(scaledOffsets[i].x, scaledOffsets[i].y)
                                            }
                                        }
                                    } else null

                                    // PathEffect alocado estritamente no cache
                                    val pathEffect = when (s.lineSpecs.type) {
                                        LineType.Dashed -> PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                                        LineType.Dotted -> PathEffect.dashPathEffect(floatArrayOf(5f, 10f), 0f)
                                        else -> null
                                    }

                                    val markerPath = when (s.pointSpecs.shape) {
                                        MarkerShape.Triangle -> {
                                            val sz = s.pointSpecs.size.toPx()
                                            Path().apply {
                                                moveTo(0f, -sz / 2f)
                                                lineTo(-sz / 2f, sz / 2f)
                                                lineTo(sz / 2f, sz / 2f)
                                                close()
                                            }
                                        }
                                        MarkerShape.Square -> {
                                            val sz = s.pointSpecs.size.toPx()
                                            Path().apply {
                                                addRect(Rect(Offset(-sz / 2f, -sz / 2f), Size(sz, sz)))
                                            }
                                        }
                                        else -> null
                                    }

                                    SeriesDrawData(
                                        offsets = scaledOffsets,
                                        originalSeries = s,
                                        pointColor = pointColor,
                                        pointSize = s.pointSpecs.size.toPx(),
                                        pointShape = s.pointSpecs.shape,
                                        showLines = s.showLines,
                                        lineColor = lineColor,
                                        lineThickness = s.lineSpecs.thickness.toPx(),
                                        linePath = linePath,
                                        markerPath = markerPath,
                                        pathEffect = pathEffect
                                    )
                                }
                                
                                // Atualiza a referência para uso no pointerInput
                                drawDataRef.data = seriesDrawData

                                onDrawBehind {
                                    if (specs.grid.isVisible) {
                                        drawGrid(steps.first, steps.second, resolvedGridColor, specs.grid.thickness.toPx())
                                    }

                                    if (specs.axis.isVisible) {
                                        drawXLine(
                                            resolvedAxisColor,
                                            specs.axis.xAxisTitle.verticalPosition,
                                            specs.axis.thickness.toPx()
                                        )
                                        drawYLine(
                                            resolvedAxisColor,
                                            specs.axis.yAxisTitle.horizontalPosition,
                                            specs.axis.thickness.toPx()
                                        )
                                    }

                                    seriesDrawData.forEach { d ->
                                        if (d.showLines && d.linePath != null) {
                                            drawPath(
                                                path = d.linePath, 
                                                color = d.lineColor, 
                                                style = Stroke(width = d.lineThickness, pathEffect = d.pathEffect)
                                            )
                                        }
                                        d.offsets.forEach { off ->
                                            when (d.pointShape) {
                                                MarkerShape.Circle -> drawCircle(color = d.pointColor, radius = d.pointSize / 2f, center = off)
                                                MarkerShape.Square, MarkerShape.Triangle -> {
                                                    d.markerPath?.let { p -> translate(off.x, off.y) { drawPath(path = p, color = d.pointColor) } }
                                                }
                                                MarkerShape.Dash -> {
                                                    drawLine(color = d.pointColor, start = Offset(off.x - d.pointSize / 2f, off.y), end = Offset(off.x + d.pointSize / 2f, off.y), strokeWidth = 2.dp.toPx())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // detectTapGestures reutilizando offsets do cache
                            .pointerInput(series, limits) {
                                detectTapGestures { tapOffset ->
                                    val threshold = 40.dp.toPx()
                                    var closestPair: Pair<Offset, ChartSeries>? = null
                                    var minDistance = Float.MAX_VALUE
                                    
                                    // Itera sobre os dados já calculados no cache
                                    drawDataRef.data.forEach { d ->
                                        d.offsets.forEachIndexed { index, scaledOffset ->
                                            val dist = (tapOffset - scaledOffset).getDistance()
                                            if (dist < threshold && dist < minDistance) {
                                                minDistance = dist
                                                // Mapeia de volta para o ponto original
                                                closestPair = Pair(d.originalSeries.data[index], d.originalSeries)
                                            }
                                        }
                                    }
                                    selectedPointInfo = closestPair
                                }
                            }
                    )

                    // Tooltip Component
                    selectedPointInfo?.let { info ->
                        val currentLimits = limits ?: return@let
                        ChartTooltip(
                            info = info,
                            minX = currentLimits.minX,
                            minY = currentLimits.minY,
                            deltaX = currentLimits.deltaX,
                            deltaY = currentLimits.deltaY
                        )
                    }
                }

                // Right Y Axis
                if (specs.axis.yAxisLabels.isVisible && specs.axis.yAxisTitle.horizontalPosition == HorizontalPosition.Right) {
                    Spacer(modifier = Modifier.width(specs.axis.yAxisLabels.padding))
                    YAxisLabels(
                        minY = limits?.minY ?: 0f,
                        maxY = limits?.maxY ?: 0f,
                        steps = steps.second,
                        specs = specs.axis.yAxisLabels,
                        modifier = Modifier.width(specs.axis.yAxisLabels.width).padding(top = specs.chartPadding)
                    )
                }
                if (specs.axis.yAxisTitle.isVisible && specs.axis.yAxisTitle.text != null && specs.axis.yAxisTitle.horizontalPosition == HorizontalPosition.Right) {
                    Spacer(modifier = Modifier.width(specs.axis.yAxisTitle.padding))
                    Box(modifier = Modifier.width(specs.axis.yAxisTitle.width), contentAlignment = Alignment.Center) {
                        AxisTitle(specs.axis.yAxisTitle)
                    }
                }
            }

            // X Labels
            if (specs.axis.xAxisLabels.isVisible) {
                Spacer(modifier = Modifier.height(specs.axis.xAxisLabels.padding))
                Row(modifier = Modifier.fillMaxWidth()) {
                    val leftOffset = (if (specs.axis.yAxisTitle.isVisible && specs.axis.yAxisTitle.text != null && specs.axis.yAxisTitle.horizontalPosition == HorizontalPosition.Left) specs.axis.yAxisTitle.width + specs.axis.yAxisTitle.padding else 0.dp) +
                                    (if (specs.axis.yAxisLabels.isVisible && specs.axis.yAxisTitle.horizontalPosition == HorizontalPosition.Left) specs.axis.yAxisLabels.width + specs.axis.yAxisLabels.padding else 0.dp)
                    val rightOffset = (if (specs.axis.yAxisLabels.isVisible && specs.axis.yAxisTitle.horizontalPosition == HorizontalPosition.Right) specs.axis.yAxisLabels.width + specs.axis.yAxisLabels.padding else 0.dp) +
                                     (if (specs.axis.yAxisTitle.isVisible && specs.axis.yAxisTitle.text != null && specs.axis.yAxisTitle.horizontalPosition == HorizontalPosition.Right) specs.axis.yAxisTitle.width + specs.axis.yAxisTitle.padding else 0.dp)
                    
                    Spacer(modifier = Modifier.width(leftOffset))
                    XAxisLabels(
                        minX = limits?.minX ?: 0f,
                        maxX = limits?.maxX ?: 0f,
                        steps = steps.first,
                        specs = specs.axis.xAxisLabels,
                        modifier = Modifier.weight(1f).padding(end = specs.chartPadding)
                    )
                    Spacer(modifier = Modifier.width(rightOffset))
                }
            }

            // X Axis Title Bottom
            if (specs.axis.xAxisTitle.isVisible && specs.axis.xAxisTitle.text != null && specs.axis.xAxisTitle.verticalPosition == VerticalPosition.Bottom) {
                Text(
                    text = specs.axis.xAxisTitle.text,
                    style = specs.axis.xAxisTitle.style.toTextStyle(),
                    modifier = Modifier.fillMaxWidth().padding(specs.axis.xAxisTitle.padding)
                )
            }
        }

        // Legend with optimized offset lambda and isolated recomposition
        if (specs.legend.isVisible && isLegendVisibleState.value) {
            DraggableLegend(
                series = series,
                resolvedSeriesStyles = resolvedSeriesStyles,
                specs = specs.legend,
                offsetState = legendOffsetState,
                isVisibleState = isLegendVisibleState,
                containerWidth = containerWidth,
                containerHeight = containerHeight
            )
        }

        // Draggable Legend Reopen Button with optimized offset lambda and isolated recomposition
        if (specs.legend.isVisible && !isLegendVisibleState.value) {
            LegendReopenButton(
                offsetState = reopenButtonOffsetState,
                isVisibleState = isLegendVisibleState,
                containerWidth = containerWidth,
                containerHeight = containerHeight
            )
        }
    }
}

@Composable
private fun DraggableLegend(
    series: List<ChartSeries>,
    resolvedSeriesStyles: List<Pair<Color, Color>>,
    specs: LegendSpecs,
    offsetState: MutableState<Offset>,
    isVisibleState: MutableState<Boolean>,
    containerWidth: Float,
    containerHeight: Float
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val resolvedLegendBgColor = specs.resolveBackgroundColor()
    val resolvedLegendTextColor = specs.resolveTextColor()

    LaunchedEffect(containerWidth, containerHeight, size) {
        if (size != IntSize.Zero) {
            val clampedX = offsetState.value.x.coerceIn(0f, (containerWidth - size.width).coerceAtLeast(0f))
            val clampedY = offsetState.value.y.coerceIn(0f, (containerHeight - size.height).coerceAtLeast(0f))
            offsetState.value = Offset(clampedX, clampedY)
        }
    }

    Card(
        modifier = Modifier
            .offset { IntOffset(offsetState.value.x.roundToInt(), offsetState.value.y.roundToInt()) }
            .onSizeChanged { size = it }
            .padding(8.dp)
            .pointerInput(containerWidth, containerHeight) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newX = (offsetState.value.x + dragAmount.x).coerceIn(0f, (containerWidth - size.width).coerceAtLeast(0f))
                    val newY = (offsetState.value.y + dragAmount.y).coerceIn(0f, (containerHeight - size.height).coerceAtLeast(0f))
                    offsetState.value = Offset(newX, newY)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = specs.elevation),
        colors = CardDefaults.cardColors(containerColor = resolvedLegendBgColor.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(specs.padding)) {
            Text(
                text = "✕",
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isVisibleState.value = false }
                    .padding(bottom = 4.dp),
                style = MaterialTheme.typography.labelMedium.copy(color = resolvedLegendTextColor, fontWeight = FontWeight.Bold)
            )
            series.forEachIndexed { index, s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    val seriesColor = resolvedSeriesStyles[index].first
                    Box(modifier = Modifier.size(12.dp).background(seriesColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = s.label, style = MaterialTheme.typography.bodySmall.copy(color = resolvedLegendTextColor))
                }
            }
        }
    }
}

@Composable
private fun LegendReopenButton(
    offsetState: MutableState<Offset>,
    isVisibleState: MutableState<Boolean>,
    containerWidth: Float,
    containerHeight: Float
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(containerWidth, containerHeight, size) {
        if (size != IntSize.Zero) {
            if (offsetState.value == Offset(-1f, -1f)) {
                offsetState.value = Offset(containerWidth - size.width - 40f, containerHeight - size.height - 40f)
            } else {
                val clampedX = offsetState.value.x.coerceIn(0f, (containerWidth - size.width).coerceAtLeast(0f))
                val clampedY = offsetState.value.y.coerceIn(0f, (containerHeight - size.height).coerceAtLeast(0f))
                offsetState.value = Offset(clampedX, clampedY)
            }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetState.value.x.roundToInt(), offsetState.value.y.roundToInt()) }
            .onSizeChanged { size = it }
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { isVisibleState.value = true }
            .pointerInput(containerWidth, containerHeight) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val nx = (offsetState.value.x + dragAmount.x).coerceIn(0f, (containerWidth - size.width).coerceAtLeast(0f))
                    val ny = (offsetState.value.y + dragAmount.y).coerceIn(0f, (containerHeight - size.height).coerceAtLeast(0f))
                    offsetState.value = Offset(nx, ny)
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "L",
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun ChartTooltip(
    info: Pair<Offset, ChartSeries>,
    minX: Float,
    minY: Float,
    deltaX: Float,
    deltaY: Float
) {
    val (point, seriesItem) = info
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val px = ((point.x - minX) / deltaX) * constraints.maxWidth.toFloat()
        val py = constraints.maxHeight.toFloat() - (((point.y - minY) / deltaY) * constraints.maxHeight.toFloat())
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        (px - 40.dp.toPx()).roundToInt().coerceIn(0, (constraints.maxWidth - 80.dp.toPx()).roundToInt()),
                        (py - 50.dp.toPx()).roundToInt().coerceAtLeast(0)
                    )
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text(text = seriesItem.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "(${String.format(Locale.US, "%.4f", point.x)}, ${String.format(Locale.US, "%.4f", point.y)})",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun AxisTitle(specs: AxisTitleSpecs) {
    val text = specs.text ?: ""
    val style = specs.style.toTextStyle()
    
    when (specs.orientation) {
        AxisTitleOrientation.Horizontal -> {
            Text(text = text, style = style)
        }
        AxisTitleOrientation.VerticalStacked -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                text.forEach { char ->
                    Text(text = char.toString(), style = style)
                }
            }
        }
        AxisTitleOrientation.VerticalParallel -> {
            val rotation = if (specs.horizontalPosition == HorizontalPosition.Left) -90f else 90f
            Text(
                text = text,
                style = style.copy(textAlign = TextAlign.Center),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints()) 
                        layout(placeable.height, placeable.width) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun YAxisLabels(minY: Float, maxY: Float, steps: Int, specs: AxisLabelsSpecs, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxHeight()) {
        val deltaY = maxY - minY
        for (i in 0..steps + 1) {
            val fraction = i.toFloat() / (steps + 1)
            val value = maxY - (deltaY * i / (steps + 1))
            Text(
                text = String.format(Locale.US, specs.format, value),
                style = specs.style.toTextStyle(),
                modifier = Modifier.align(Alignment.TopEnd).layout { measurable: Measurable, constraints: Constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        val y = (constraints.maxHeight * fraction).toInt() - (placeable.height / 2)
                        placeable.placeRelative(0, y)
                    }
                }
            )
        }
    }
}

@Composable
private fun XAxisLabels(minX: Float, maxX: Float, steps: Int, specs: AxisLabelsSpecs, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        val deltaX = maxX - minX
        for (i in 0..steps + 1) {
            val fraction = i.toFloat() / (steps + 1)
            val value = minX + (deltaX * i / (steps + 1))
            Text(
                text = String.format(Locale.US, specs.format, value),
                style = specs.style.toTextStyle(),
                modifier = Modifier.align(Alignment.TopStart).layout { measurable: Measurable, constraints: Constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        val x = (constraints.maxWidth * fraction).toInt() - (placeable.width / 2)
                        placeable.placeRelative(x, 0)
                    }
                }
            )
        }
    }
}

@Composable
fun ScatterChart(
    data: List<Offset>,
    modifier: Modifier = Modifier,
    label: String = "Data Series",
    showLines: Boolean = false,
    pointSpecs: PointSpecs = PointSpecs(),
    chartSpecs: ChartSpecs = ChartSpecs()
) {
    val singleSeries = listOf(ChartSeries(data, label, showLines, pointSpecs))
    ScatterChart(singleSeries, modifier, chartSpecs)
}

@JvmName("ScatterChartFloat")
@Composable
fun ScatterChart(
    data: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    label: String = "Data Series",
    showLines: Boolean = false,
    chartSpecs: ChartSpecs = ChartSpecs()
) {
    val offsets = remember(data) { data.map { Offset(it.first, it.second) } }
    ScatterChart(offsets, modifier, label, showLines, PointSpecs(), chartSpecs)
}

@JvmName("ScatterChartApache")
@Composable
fun ScatterChart(
    mathematicalFunction: UnivariateFunction, // aceita polynomial function, splines e outros tipos
    minX: Double,
    maxX: Double,
    steps: Int = 100,
    modifier: Modifier = Modifier,
    label: String = "Function",
    showLines: Boolean = false,
    chartSpecs: ChartSpecs = ChartSpecs()
) {
    val generatedPoints = remember(mathematicalFunction, minX, maxX, steps) {
        val stepSize = (maxX - minX) / steps
        List(steps) { index ->
            val x = minX + (index * stepSize)
            Offset(x.toFloat(), mathematicalFunction.value(x).toFloat())
        }
    }
    ScatterChart(generatedPoints, modifier, label, showLines, PointSpecs(), chartSpecs)
}

@JvmName("ScatterChartKMath")
@Composable
fun ScatterChart(
    // exemplo: mathematicalFunction = {x -> Math.sin(x) * 2}
    mathematicalFunction: (Double) -> Double,
    minX: Double,
    maxX: Double,
    steps: Int = 100,
    modifier: Modifier = Modifier,
    label: String = "Function",
    showLines: Boolean = false,
    chartSpecs: ChartSpecs = ChartSpecs()
) {
    val generatedPoints = remember(mathematicalFunction, minX, maxX, steps) {
        val stepSize = (maxX - minX) / steps
        List(steps) { index ->
            val x = minX + (index * stepSize)
            Offset(x.toFloat(), mathematicalFunction(x).toFloat())
        }
    }
    ScatterChart(generatedPoints, modifier, label, showLines, PointSpecs(), chartSpecs)
}
