package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.game.model.CityStats
import com.example.game.renderer.Camera3D
import com.example.game.ui.GameTopBar
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun hud_screenshot() {
        val stats = CityStats(
            population = 4850,
            treasury = 125000L,
            happiness = 88,
            dayCount = 5,
            dayTime = 14.5f
        )
        val camera = Camera3D()

        composeTestRule.setContent {
            MyApplicationTheme {
                GameTopBar(
                    stats = stats,
                    camera = camera,
                    fps = 60,
                    showFps = true,
                    onSpeedChanged = {},
                    onOpenDemand = {},
                    onOpenCityInfo = {},
                    onOpenSettings = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/hud.png")
    }
}

