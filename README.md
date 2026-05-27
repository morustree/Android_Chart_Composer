[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=morustree_Android_Chart_Composer&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=morustree_Android_Chart_Composer) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=morustree_Android_Chart_Composer&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=morustree_Android_Chart_Composer) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=morustree_Android_Chart_Composer&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=morustree_Android_Chart_Composer) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=morustree_Android_Chart_Composer&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=morustree_Android_Chart_Composer) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=morustree_Android_Chart_Composer&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=morustree_Android_Chart_Composer) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) ![GitHub repo size](https://img.shields.io/github/repo-size/morustree/Android_Chart_Composer)


# 📊 Android Chart Composer

A plotting library built natively in and for the Jetpack Compose environment. Engineered from the ground up for modern Android applications, it aims to render dense datasets effortlessly.

<br>

<img width="2408" height="1080" alt="screenshot3" src="https://github.com/user-attachments/assets/1df41b67-bc7c-4008-b12b-5fbc670e8c94" />

<br><br>
📌 **Current Capability:** The library currently supports Scatter Chart plotting on a linear scale with multi-series capabilities.

---

## 📦 Installation

Open your settings.gradle.kts file and add the JitPack URL at the end of the repositories block:
```kotlin
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
		}
}
```

Add the dependency (build.gradle.kts):
```kotlin
dependencies {
	        implementation("com.github.morustree:Android_Chart_Composer:1.0.2")
}
 ```

---

## ✨ Features

*   **Native Compose Integration:** Designed with Composable functions, eliminating XML overhead and supporting smart recomposition out-of-the-box.
*   **Multi-Series Support:** Plot multiple independent datasets concurrently, each with its own color mapping, marker styles, and connectivity specs.
*   **Rich Layout Freedom:** Complete control over titles, labels, grids, and boundaries through customizable structural classes.
*   **Interactive Floating Legend:** Includes a legend card that users can freely drag or tap away in real-time.
*   **Active Development:** focused on usability refinements and performance auditing.

 ---

## 🔌 Data Inputs

To serve both lightweight visual trackers and scientific platforms, the library supports multiple data paradigms via method overloading:
1.  **`List<Offset>`** & **`List<Pair<Float, Float>>`**.
2.  **`UnivariateFunction` (Apache Commons Math)**: Plot complex polynomials, splines, and regressions without manual data conversion. If you want to use the **`UnivariateFunction`** data input, you must add the Apache dependency to your own application.
3.  **`(Double) -> Double` (Kotlin/KMath)**: Directly plots functional lambdas, offering extensive compatibility with the modern Kotlin Mathematics ecosystem.

---

## 🚀 Usage Examples

### 1. Real-Time Moving Waves
This example showcases how the library behaves under high-frequency layout changes, drawing two interwoven mathematical waves moving horizontally across the screen.

https://github.com/user-attachments/assets/ab9da4ba-10b2-428d-9aa8-657e84958b05

```kotlin
// Inside your Composable structure
var animationStep by remember { mutableStateOf(0f) }

LaunchedEffect(Unit) {
    while (true) {
        animationStep += 0.05f // Horizontal shift speed
        delay(16)              // Matches ~60Hz refresh cycles
    }
}

val movingSeries = remember(animationStep) {
    listOf(
        ChartSeries(
            label = "Alpha Phase",
            showLines = true,
            pointSpecs = PointSpecs(color = Color.Cyan, size = 5.dp),
            lineSpecs = LineSpecs(color = Color.Cyan.copy(alpha = 0.8f), thickness = 3.dp),
            data = List(150) { i -> Offset(i.toFloat(), (Math.sin((i * 0.15) - animationStep) * 80).toFloat()) }
        ),
        ChartSeries(
            label = "Beta Phase",
            showLines = true,
            pointSpecs = PointSpecs(color = Color.Magenta, size = 5.dp),
            lineSpecs = LineSpecs(color = Color.Magenta.copy(alpha = 0.8f), thickness = 3.dp),
            data = List(150) { i -> Offset(i.toFloat(), (-Math.cos((i * 0.15) - animationStep) * 80).toFloat()) }
        )
    )
}

ScatterChart(
    series = movingSeries,
    modifier = Modifier.fillMaxWidth().height(350.dp)
)
```


### 2. Static Pure Mathematical Functions
Perfect for static data, report documents, or engineering dashboards.

<img width="2408" height="1080" alt="screenshot2" src="https://github.com/user-attachments/assets/3c99fb3c-71fe-4cdc-b54f-56bd935bfed4" />

```kotlin
// Plots a static Sine function using Kotlin's native math lambdas
ScatterChart(
    mathematicalFunction = { x -> Math.sin(x) },
    minX = 0.0,
    maxX = 2.0 * Math.PI, // Plots a complete sine period (0 to 2π)
    steps = 100,          // Automatically computes 100 precise plotting points
    label = "Sine Wave f(x)",
    showLines = true,
    modifier = Modifier.fillMaxWidth().height(300.dp)
)
```


### 3. Multi-series static plotting

<img width="2408" height="1080" alt="screenshot3" src="https://github.com/user-attachments/assets/c3267905-5788-4093-a61f-7644cc3882be" />


```kotlin
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
```

---

## ⚡ Performance & Benchmarks

An extreme real-time stress workload was simulated with 10,000 active data points updating 60 times per second under automated user drag interaction. 

### 📈 Official Audit Results
*   **Test Device:** Samsung Galaxy M14 5G (SM-M146B)
*   **Android Environment:** Android 14 / One UI 6 (Mid-range processor)
*   **Audit Engine:** Jetpack Macrobenchmark Wireless Runner

```text
timeToInitialDisplayMs   min 111.2,   median 175.8,   max 918.8
```

### 🏆 Benchmark Takeaways
*   **Instant GPU Pipe:** Once the view initializes, rendering the dense array of 10k points takes a low 111.2ms at its peak performance.
*   **Zero Memory Thrashing:** The steady median time of 175.8ms guarantees that the Kotlin Garbage Collector never triggers hard UI pauses. Memory allocation remains flat, and frame times stay clean.
*   **Continuous UI Fluidity:** Interactive user elements (like dragging the floating legend card) run on an independent layer, completely isolated from data manipulation.

👉 *Want to explore the raw logs? Check the full [Detailed Stress Test Report & Architecture Audit](BENCHMARK.md).*

---

## 🗺️ Roadmap & Continuous Improvement

The upcoming releases are focused on:
*   [ ] **LineChart Engine Expansion:** Reusing the robust axis, label, and grid sub-systems to introduce a fully featured native `LineChart` module.
*   [ ] **Baseline Profiles:** Shaving down the initial cold start initialization peak.

---

## 📄 License

This library is available as open source under the terms of the [MIT License](LICENSE).
