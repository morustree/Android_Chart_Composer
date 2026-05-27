package com.rma.benchmark

import android.content.Intent
import android.graphics.Point
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChartStressBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun stressTest10kPointsWithInteractions() {
        // Samsung devices often fail with FrameTimingMetric due to One UI trace issues
        // We use StartupTimingMetric to at least verify load performance with 10k points
        val metrics = listOf(StartupTimingMetric())

        benchmarkRule.measureRepeated(
            packageName = "com.rma.biblioteca_graficos",
            metrics = metrics,
            compilationMode = CompilationMode.None(),
            iterations = 5
        ) {
            // Measure activity launch and initial render of 10k points
            val intent = Intent().apply {
                action = "android.intent.action.MAIN"
                addCategory("android.intent.category.LAUNCHER")
                setClassName("com.rma.biblioteca_graficos", "com.rma.biblioteca_graficos.MainActivity")
                putExtra("BENCHMARK", true)
            }
            startActivityAndWait(intent)

            // Wait for chart to appear
            device.wait(Until.hasObject(By.textContains("Benchmark")), 5000)

            val centerX = device.displayWidth / 2
            val centerY = device.displayHeight / 2

            // Simulate frequent taps to trigger Tooltip
            repeat(10) { i ->
                val xOffset = (i - 5) * 50
                device.click(centerX + xOffset, centerY)
            }

            // Find Legend and drag it
            var closeButton = device.findObject(By.text("✕"))
            if (closeButton != null) {
                closeButton.drag(Point(centerX, centerY))
                device.waitForIdle()

                closeButton = device.findObject(By.text("✕"))
                closeButton?.click()
                device.waitForIdle()
                
                val reopenButton = device.wait(Until.findObject(By.text("L")), 2000)
                reopenButton?.click()
                device.waitForIdle()
            }
            
            device.swipe(centerX, centerY + 200, centerX, centerY - 200, 10)
            device.waitForIdle()
        }
    }
}
