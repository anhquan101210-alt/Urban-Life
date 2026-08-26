package com.example.game.renderer

import androidx.compose.ui.geometry.Offset
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 2D Isometric Pixel-Art Camera.
 * Uses classical 2:1 isometric diamond projection:
 * ScreenX = (gx - gy) * (tileWidth / 2) + panX + (viewportWidth / 2)
 * ScreenY = (gx + gy) * (tileHeight / 2) + panY + (viewportHeight / 2)
 */
class Camera3D(
    var targetX: Float = 18f,
    var targetY: Float = 18f,
    var zoom: Float = 1.0f,
    var panX: Float = 0f,
    var panY: Float = 0f,
    var viewportWidth: Float = 1000f,
    var viewportHeight: Float = 800f
) {
    val minZoom = 0.45f
    val maxZoom = 2.8f

    // Isometric diamond dimensions at 1.0x scale
    val baseTileWidth = 64f
    val baseTileHeight = 32f

    fun setZoomPreset(level: Int) {
        when (level) {
            1 -> { // City Overview
                zoom = 0.50f
                clampPan()
            }
            2 -> { // District
                zoom = 0.85f
                clampPan()
            }
            3 -> { // Neighborhood
                zoom = 1.30f
                clampPan()
            }
            4 -> { // Street
                zoom = 1.95f
                clampPan()
            }
        }
    }

    fun setCityView() {
        setZoomPreset(1)
        centerOn(18f, 18f)
    }

    fun setIsometricView() {
        setZoomPreset(2)
        centerOn(18f, 18f)
    }

    fun setCloseView() {
        setZoomPreset(4)
        centerOn(18f, 18f)
    }

    fun setTopView() {
        setZoomPreset(3)
        centerOn(18f, 18f)
    }

    fun centerOn(gx: Float, gy: Float) {
        val cgx = gx.coerceIn(0f, 35f)
        val cgy = gy.coerceIn(0f, 35f)
        targetX = cgx
        targetY = cgy
        val halfW = (baseTileWidth * zoom) / 2f
        val halfH = (baseTileHeight * zoom) / 2f
        panX = -(cgx - cgy) * halfW
        panY = -(cgx + cgy) * halfH
        clampPan()
    }

    fun pan(deltaScreenX: Float, deltaScreenY: Float) {
        panX += deltaScreenX
        panY += deltaScreenY

        clampPan()

        // Update targetX & targetY
        val halfW = (baseTileWidth * zoom) / 2f
        val halfH = (baseTileHeight * zoom) / 2f
        val rx = -panX
        val ry = -panY
        targetX = (((rx / halfW) + (ry / halfH)) / 2f).coerceIn(0f, 35f)
        targetY = (((ry / halfH) - (rx / halfW)) / 2f).coerceIn(0f, 35f)
    }

    private fun clampPan() {
        val halfW = (baseTileWidth * zoom) / 2f
        val halfH = (baseTileHeight * zoom) / 2f

        // Map extent in isometric screen space is approximately:
        // X from -(36 * halfW) to +(36 * halfW)
        // Y from 0 to +(72 * halfH)
        val maxPanX = 36f * halfW + (viewportWidth / 3f)
        val minPanX = -36f * halfW - (viewportWidth / 3f)
        val minPanY = -72f * halfH - (viewportHeight / 3f)
        val maxPanY = (viewportHeight / 3f)

        panX = panX.coerceIn(minPanX, maxPanX)
        panY = panY.coerceIn(minPanY, maxPanY)
    }

    fun zoomBy(factor: Float) {
        val oldZoom = zoom
        zoom = (zoom * factor).coerceIn(minZoom, maxZoom)
        // Maintain center of screen on zoom
        val ratio = zoom / oldZoom
        panX *= ratio
        panY *= ratio
    }

    fun rotateBy(deltaYaw: Float, deltaPitch: Float) {
        // Pixel-art is fixed isometric diagonal view
    }

    /**
     * Projects world 2D grid coordinates (gx, gy) + optional elevation to screen space (sx, sy).
     */
    fun project(gx: Float, gy: Float, elevation: Float = 0f): Offset {
        val halfW = (baseTileWidth * zoom) / 2f
        val halfH = (baseTileHeight * zoom) / 2f

        val sx = (gx - gy) * halfW + panX + (viewportWidth / 2f)
        val sy = (gx + gy) * halfH + panY + (viewportHeight / 2f) - (elevation * 24f * zoom)

        return Offset(sx, sy)
    }

    fun getDepth(gx: Float, gy: Float): Float {
        // In 2D isometric, painter's algorithm sorts by (gx + gy)
        return (gx + gy)
    }

    /**
     * Converts screen pixel tap (sx, sy) to 2D isometric grid coordinates (gx, gy).
     */
    fun screenToGround(sx: Float, sy: Float): Pair<Int, Int>? {
        val halfW = (baseTileWidth * zoom) / 2f
        val halfH = (baseTileHeight * zoom) / 2f

        val rx = sx - (panX + (viewportWidth / 2f))
        val ry = sy - (panY + (viewportHeight / 2f))

        val gx = ((rx / halfW) + (ry / halfH)) / 2f
        val gy = ((ry / halfH) - (rx / halfW)) / 2f

        val igx = floor(gx).toInt()
        val igy = floor(gy).toInt()

        if (igx in 0 until 36 && igy in 0 until 36) {
            return Pair(igx, igy)
        }
        return null
    }
}
