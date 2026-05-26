package com.rma.biblioteca_graficos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import com.rma.biblioteca_graficos.ui.theme.Biblioteca_GraficosTheme

import com.rma.android_chart_composer.ScatterChart
import com.rma.android_chart_composer.specs.AxisSpecs
import com.rma.android_chart_composer.specs.AxisTitleSpecs
import com.rma.android_chart_composer.specs.ChartSeries
import com.rma.android_chart_composer.specs.TitleSpecs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rma.android_chart_composer.specs.AxisLabelsSpecs
import com.rma.android_chart_composer.specs.AxisTitleOrientation
import com.rma.android_chart_composer.specs.ChartSpecs
import com.rma.android_chart_composer.specs.GridSpecs
import com.rma.android_chart_composer.specs.HorizontalPosition
import com.rma.android_chart_composer.specs.LegendSpecs
import com.rma.android_chart_composer.specs.LineSpecs
import com.rma.android_chart_composer.specs.LineType
import com.rma.android_chart_composer.specs.MarkerShape
import com.rma.android_chart_composer.specs.PointSpecs
import com.rma.android_chart_composer.specs.TextStyleSpecs
import com.rma.android_chart_composer.specs.VerticalPosition
import kotlinx.coroutines.delay
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen()
        setContent {
            Biblioteca_GraficosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {

                        val sineFunction = mutableListOf<Offset>()
                        val phaseShift = mutableListOf<Offset>()
                        for (degrees in 0..360 step 10) {
                            val x = degrees.toFloat()

                            val y1 = sin(toRadians(degrees.toDouble())).toFloat()
                            sineFunction.add(Offset(x, y1))

                            val y2 = sin(toRadians(degrees.toDouble() - 45.0)).toFloat()
                            phaseShift.add(Offset(x, y2))
                        }

                        val chartSeries: List<ChartSeries> = listOf(
                            ChartSeries(
                                label = "Alpha Phase",
                                pointSpecs = PointSpecs(color = Color.Blue, shape = MarkerShape.Square),
                                data = sineFunction
                            ),
                            ChartSeries(
                                label = "Beta Phase",
                                pointSpecs = PointSpecs(color = Color.Red, shape = MarkerShape.Circle),
                                data = phaseShift
                            )
                        )

                        ScatterChart(
                            series = chartSeries,
                            modifier = Modifier.fillMaxSize(),
                            specs = ChartSpecs(
                                title = TitleSpecs(text = "Waves"),
                                axis = AxisSpecs(
                                    yAxisTitle = AxisTitleSpecs(
                                        text = "y-axis",
                                        orientation = AxisTitleOrientation.VerticalParallel
                                    ),
                                    xAxisTitle = AxisTitleSpecs(
                                        text = "x-axis"
                                    )
                                ),
                                grid = GridSpecs(color = Color.LightGray.copy(alpha = 0.4f)),
                            )
                        )


                        // Plots a static Sine function using Kotlin's native math lambdas
                        /*ScatterChart(
                            mathematicalFunction = { x -> Math.sin(x) },
                            minX = 0.0,
                            maxX = 2.0 * Math.PI, // Plots a complete sine period (0 to 2π)
                            steps = 100,          // Automatically computes 100 precise plotting points
                            label = "Sine wave",
                            showLines = true,
                            modifier = Modifier.fillMaxWidth().height(350.dp)
                        )*/


                        /*var passoAnimacao by remember { mutableStateOf(0f) }

                        LaunchedEffect(Unit) {
                            while (true) {
                                passoAnimacao += 0.05f
                                delay(16)
                            }
                        }

                        val seriesDemonstracao = remember(passoAnimacao) {
                            listOf(
                                ChartSeries(
                                    label = "Phase A",
                                    showLines = true,
                                    pointSpecs = PointSpecs(color = Color.Cyan, size = 5.dp),
                                    lineSpecs = LineSpecs(
                                        color = Color.Cyan.copy(alpha = 0.8f),
                                        thickness = 3.dp
                                    ),
                                    data = List(150) { i ->
                                        val x = i.toFloat()
                                        val y =
                                            (sin((i * 0.15) - passoAnimacao) * 80).toFloat()
                                        Offset(x, y)
                                    }
                                ),
                                ChartSeries(
                                    label = "Phase B",
                                    showLines = true,
                                    pointSpecs = PointSpecs(color = Color.Magenta, size = 5.dp),
                                    lineSpecs = LineSpecs(
                                        color = Color.Magenta.copy(alpha = 0.8f),
                                        thickness = 3.dp
                                    ),
                                    data = List(150) { i ->
                                        val x = i.toFloat()
                                        val y = (-cos((i * 0.15) - passoAnimacao) * 80).toFloat()
                                        Offset(x, y)
                                    }
                                )
                            )
                        }

                        ScatterChart(
                            series = seriesDemonstracao,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp)
                        )*/

                    }
                }
            }



            // benchmark; testarEstresseERenderizacaoDoGrafico()
            /*var seriesDeEstresse by remember { mutableStateOf(gerarNovosPontos(0f)) }

            LaunchedEffect(Unit) {
                var passo = 0f
                while(true) {
                    seriesDeEstresse = gerarNovosPontos(passo)
                    passo += 0.1f
                    delay(16)
                }
            }
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                ScatterChart(
                    series = seriesDeEstresse,
                    modifier = Modifier.fillMaxSize()
                )
            }*/
        }
    }

    // benchmark; testarEstresseERenderizacaoDoGrafico()
    /*private fun gerarNovosPontos(passo: Float): List<ChartSeries> {
        return List(5) { idDaSerie ->
            ChartSeries(
                label = "Série Dinâmica $idDaSerie",
                showLines = true,
                data = List(2000) { indice ->
                    val x = indice.toFloat()
                    val y = (Math.sin((indice.toDouble() * 0.05) + passo) * 100 + (idDaSerie * 50)).toFloat()
                    Offset(x, y)
                }
            )
        }
    }*/

}


@Preview(name = "Claro", showBackground = true)
@Preview(
    name = "Escuro",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Preview(
    name = "Paisagem",
    device = "spec:parent=pixel_5,orientation=landscape",
    showBackground = true
)
@Composable
fun ScatterChartPreview() {
    Biblioteca_GraficosTheme {
        ScatterChart(
            mathematicalFunction = { x -> Math.sin(x) * 2 },
            minX = 0.0,
            maxX = 10.0
        )
    }
}