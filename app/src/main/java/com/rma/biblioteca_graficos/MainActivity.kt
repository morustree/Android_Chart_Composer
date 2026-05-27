package com.rma.biblioteca_graficos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import com.rma.biblioteca_graficos.ui.theme.Biblioteca_GraficosTheme

import com.rma.android_chart_composer.ScatterChart
import com.rma.android_chart_composer.specs.ChartSeries
import com.rma.android_chart_composer.specs.ChartSpecs
import com.rma.android_chart_composer.specs.TitleSpecs
import com.rma.android_chart_composer.specs.PointSpecs
import com.rma.android_chart_composer.specs.LineSpecs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check for benchmark flag in intent
        val isBenchmark = intent?.getBooleanExtra("BENCHMARK", false) ?: false
        
        enableEdgeToEdge()
        installSplashScreen()
        setContent {
            Biblioteca_GraficosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ChartContent(isBenchmark)
                    }
                }
            }
        }
    }
}

@Composable
fun ChartContent(isBenchmark: Boolean) {
    val pointCount = if (isBenchmark) 10000 else 100
    
    val series = remember(isBenchmark) {
        val random = Random(42)
        val data = List(pointCount) {
            Offset(
                x = random.nextFloat() * 100f,
                y = random.nextFloat() * 100f
            )
        }
        listOf(
            ChartSeries(
                data = data,
                label = "Stress Test Series",
                showLines = true,
                pointSpecs = PointSpecs(size = 4.dp),
                lineSpecs = LineSpecs(thickness = 1.dp)
            )
        )
    }

    ScatterChart(
        series = series,
        modifier = Modifier.fillMaxSize(),
        specs = ChartSpecs(
            title = TitleSpecs(text = if (isBenchmark) "Benchmark: 10k Points" else "Demo Chart")
        )
    )
}

@Preview(name = "Claro", showBackground = true)
@Composable
fun ScatterChartPreview() {
    Biblioteca_GraficosTheme {
        ChartContent(isBenchmark = false)
    }
}
