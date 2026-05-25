package com.rma.android_chart_composer.specs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

/**
 * Represents a single data series in a chart.
 */
@Immutable
data class ChartSeries(
    val data: List<Offset>,
    val label: String = "Data Series",
    val showLines: Boolean = false,
    val pointSpecs: PointSpecs = PointSpecs(),
    val lineSpecs: LineSpecs = LineSpecs()
)

@Composable
fun TextStyleSpecs.toTextStyle(): TextStyle {
    return MaterialTheme.typography.labelSmall.copy(
        color = if (color.isUnspecified) MaterialTheme.colorScheme.onSurface else color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        textDecoration = textDecoration,
        textAlign = textAlign,
        background = if (backgroundColor.isUnspecified) Color.Transparent else backgroundColor
    )
}

@Immutable
data class TextStyleSpecs(
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = 12.sp,
    val fontWeight: FontWeight = FontWeight.Normal,
    val fontStyle: FontStyle = FontStyle.Normal,
    val fontFamily: FontFamily = FontFamily.SansSerif,
    val textDecoration: TextDecoration = TextDecoration.None,
    val textAlign: TextAlign = TextAlign.Center,
    val backgroundColor: Color = Color.Unspecified
)

@Immutable
data class TitleSpecs(
    val isVisible: Boolean = true,
    val text: String? = null,
    val style: TextStyleSpecs = TextStyleSpecs(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    val padding: Dp = 16.dp
)

@Immutable
data class AxisTitleSpecs(
    val isVisible: Boolean = true,
    val text: String? = null,
    val style: TextStyleSpecs = TextStyleSpecs(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    val padding: Dp = 4.dp, // Espaçamento entre o título e o label
    val verticalPosition: VerticalPosition = VerticalPosition.Bottom,
    val horizontalPosition: HorizontalPosition = HorizontalPosition.Left,
    val orientation: AxisTitleOrientation = AxisTitleOrientation.Horizontal,
    val width: Dp = 16.dp // Largura da área do título (para eixos verticais)
)

@Immutable
data class AxisLabelsSpecs(
    val isVisible: Boolean = true,
    val style: TextStyleSpecs = TextStyleSpecs(fontSize = 10.sp),
    val format: String = "%.2f",
    val step: Double = 0.0,
    val width: Dp = 56.dp, // Largura da área dos rótulos (para eixos verticais)
    val padding: Dp = 8.dp  // Espaçamento entre o rótulo e o gráfico
)

@Immutable
data class AxisSpecs(
    val isVisible: Boolean = true,
    val xAxisTitle: AxisTitleSpecs = AxisTitleSpecs(),
    val yAxisTitle: AxisTitleSpecs = AxisTitleSpecs(),
    val xAxisLabels: AxisLabelsSpecs = AxisLabelsSpecs(),
    val yAxisLabels: AxisLabelsSpecs = AxisLabelsSpecs(),
    val lineColor: Color = Color.Unspecified,
    val thickness: Dp = 2.dp
)

@Immutable
data class GridSpecs(
    val isVisible: Boolean = true,
    val color: Color = Color.Unspecified,
    val thickness: Dp = 1.dp,
    val gridType: LineType = LineType.Solid
)

@Immutable
data class LegendSpecs(
    val isVisible: Boolean = true,
    val backgroundColor: Color = Color.Unspecified,
    val textColor: Color = Color.Unspecified,
    val padding: Dp = 8.dp,
    val elevation: Dp = 6.dp,
    val initialOffset: Offset = Offset(20f, 20f)
)

@Immutable
data class PointSpecs(
    val color: Color = Color.Unspecified,
    val size: Dp = 6.dp,
    val shape: MarkerShape = MarkerShape.Circle
)

@Immutable
data class LineSpecs(
    val color: Color = Color.Unspecified,
    val thickness: Dp = 2.dp,
    val type: LineType = LineType.Solid
)

@Immutable
data class ChartSpecs(
    val backgroundColor: Color = Color.Unspecified,
    val chartPadding: Dp = 16.dp,
    val title: TitleSpecs = TitleSpecs(),
    val axis: AxisSpecs = AxisSpecs(),
    val grid: GridSpecs = GridSpecs(),
    val legend: LegendSpecs = LegendSpecs()
)

@Composable
fun GridSpecs.resolveColor(): Color = if (color.isUnspecified) MaterialTheme.colorScheme.outlineVariant else color

@Composable
fun AxisSpecs.resolveLineColor(): Color = if (lineColor.isUnspecified) MaterialTheme.colorScheme.outline else lineColor

@Composable
fun LegendSpecs.resolveBackgroundColor(): Color = if (backgroundColor.isUnspecified) MaterialTheme.colorScheme.surfaceVariant else backgroundColor

@Composable
fun LegendSpecs.resolveTextColor(): Color = if (textColor.isUnspecified) MaterialTheme.colorScheme.onSurfaceVariant else textColor

@Composable
fun PointSpecs.resolveColor(defaultColor: Color): Color = if (color.isUnspecified) defaultColor else color

@Composable
fun LineSpecs.resolveColor(defaultColor: Color): Color = if (color.isUnspecified) defaultColor else color

@Composable
fun ChartSpecs.resolveBackgroundColor(): Color = if (backgroundColor.isUnspecified) MaterialTheme.colorScheme.surface else backgroundColor

enum class MarkerShape { Circle, Dash, Square, Triangle }
enum class LineType { Dashed, Dotted, Solid }
enum class VerticalPosition { Top, Bottom, Center }
enum class HorizontalPosition { Left, Right, Center }
enum class AxisTitleOrientation { Horizontal, VerticalStacked, VerticalParallel }
