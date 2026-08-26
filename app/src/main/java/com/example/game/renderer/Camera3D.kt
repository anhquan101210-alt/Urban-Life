package com.example.game.renderer

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

class Camera3D(
    var targetX: Float = 18f,
    var targetY: Float = 18f,
    var zoom: Float = 1.0f,
    var yawDeg: Float = 45f,   // Horizontal rotation (0 to 360)
    var pitchDeg: Float = 55f, // Tilt angle (20 to 85)
    var viewportWidth: Float = 1000f,
    var viewportHeight: Float = 800f
) {
    val minZoom = 0.45f
    val maxZoom = 2.8f

    fun setTopView() {
        pitchDeg = 80f
        yawDeg = 0f
        zoom = 0.9f
    }

    fun setIsometricView() {
        pitchDeg = 55f
        yawDeg = 45f
        zoom = 1.1f
    }

    fun setCloseView() {
        pitchDeg = 32f
        zoom = 2.0f
    }

    fun centerOn(x: Float, y: Float) {
        targetX = x
        targetY = y
    }

    fun pan(deltaScreenX: Float, deltaScreenY: Float) {
        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

        // Convert screen delta to world ground delta
        val baseTileSize = 48f * zoom
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val sinP = sin(pitchRad).coerceAtLeast(0.3f)

        val unscaledDx = deltaScreenX / baseTileSize
        val unscaledDy = (deltaScreenY / baseTileSize) / sinP

        val worldDx = (unscaledDx * cosY + unscaledDy * sinY) * 0.75f
        val worldDy = (-unscaledDx * sinY + unscaledDy * cosY) * 0.75f

        targetX = (targetX - worldDx).coerceIn(0f, 36f)
        targetY = (targetY - worldDy).coerceIn(0f, 36f)
    }

    fun zoomBy(factor: Float) {
        zoom = (zoom * factor).coerceIn(minZoom, maxZoom)
    }

    fun rotateBy(deltaYaw: Float, deltaPitch: Float) {
        yawDeg = (yawDeg + deltaYaw) % 360f
        if (yawDeg < 0) yawDeg += 360f
        pitchDeg = (pitchDeg + deltaPitch).coerceIn(25f, 85f)
    }

    /**
     * Projects a 3D world coordinate (x, y, z) into 2D Screen Space (screenX, screenY).
     */
    fun project(wx: Float, wy: Float, wz: Float): Offset {
        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

        val relX = wx - targetX
        val relY = wy - targetY
        val relZ = wz

        // 1. Rotate around Z axis (Yaw)
        val rotX = relX * cos(yawRad) - relY * sin(yawRad)
        val rotY = relX * sin(yawRad) + relY * cos(yawRad)

        // 2. Rotate around X axis (Pitch)
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)

        val projX = rotX
        val projY = rotY * sinP - relZ * cosP

        val baseTileSize = 48f * zoom

        val screenX = viewportWidth / 2f + projX * baseTileSize
        val screenY = viewportHeight / 2f + projY * baseTileSize

        return Offset(screenX, screenY)
    }

    /**
     * Calculates visual depth (distance from camera) for sorting 3D objects back-to-front.
     */
    fun getDepth(wx: Float, wy: Float): Float {
        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val relX = wx - targetX
        val relY = wy - targetY
        return relX * sin(yawRad) + relY * cos(yawRad)
    }

    /**
     * Inverse Raycasting: Converts screen tap (sx, sy) to ground plane (gx, gy).
     */
    fun screenToGround(sx: Float, sy: Float): Pair<Int, Int>? {
        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
        val baseTileSize = 48f * zoom
        val sinP = sin(pitchRad).coerceAtLeast(0.2f)

        val projX = (sx - viewportWidth / 2f) / baseTileSize
        val projY = (sy - viewportHeight / 2f) / baseTileSize

        val rotY = projY / sinP
        val rotX = projX

        val cosY = cos(yawRad)
        val sinY = sin(yawRad)

        val relX = rotX * cosY + rotY * sinY
        val relY = -rotX * sinY + rotY * cosY

        val gx = (relX + targetX).roundToInt()
        val gy = (relY + targetY).roundToInt()

        if (gx in 0 until 36 && gy in 0 until 36) {
            return Pair(gx, gy)
        }
        return null
    }
}
