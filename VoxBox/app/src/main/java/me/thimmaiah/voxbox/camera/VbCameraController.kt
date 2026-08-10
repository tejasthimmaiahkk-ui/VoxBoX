package me.thimmaiah.voxbox.camera

import androidx.camera.view.CameraController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zoom for the board camera, wrapped around CameraX's [CameraController].
 *
 * Uses `setZoomRatio` rather than `setLinearZoom` because the read-out has to show real
 * magnification: linear zoom is perceptually spaced, so 0.5 on that scale is not 0.5×, and a
 * student aiming at a whiteboard from the back of a hall needs the true number.
 *
 * Bounds come from `ZoomState`, never a hardcoded maximum. Phones with an ultra-wide lens report
 * a minimum below 1.0, and clamping to 1.0 would remove the widest setting — the one most useful
 * for fitting a whole board in frame.
 */
class VbCameraController {
    private var controller: CameraController? = null

    private val _zoom = MutableStateFlow(1f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()

    private val _range = MutableStateFlow(1f to 1f)
    val range: StateFlow<Pair<Float, Float>> = _range.asStateFlow()

    /**
     * Bumped on every zoom change so the capture loop can reset the frame-difference baseline.
     *
     * Zooming changes every pixel, which the change detector would otherwise read as a new board
     * and upload. Resetting the reference means one zoom gesture costs at most one extra frame.
     */
    private val _zoomChangedAt = MutableStateFlow(0L)
    val zoomChangedAt: StateFlow<Long> = _zoomChangedAt.asStateFlow()

    val hasRange: Boolean get() = _range.value.second > _range.value.first

    fun bind(controller: CameraController) {
        this.controller = controller
        controller.zoomState.value?.let { state ->
            _range.value = state.minZoomRatio to state.maxZoomRatio
            _zoom.value = state.zoomRatio
        }
    }

    /** Re-reads the real bounds once the camera has actually opened and reported them. */
    fun refreshRange() {
        controller?.zoomState?.value?.let { state ->
            _range.value = state.minZoomRatio to state.maxZoomRatio
            _zoom.value = state.zoomRatio
        }
    }

    fun unbind() {
        controller = null
    }

    /** [fraction] runs 0..1 across the device's real zoom range. */
    fun setFraction(fraction: Float) {
        val (min, max) = _range.value
        applyRatio(min + fraction.coerceIn(0f, 1f) * (max - min))
    }

    /** Multiplicative step, for a pinch gesture's scale delta. */
    fun nudge(scaleDelta: Float) = applyRatio(_zoom.value * scaleDelta)

    /** Keyboard and D-pad stepping, per the accessibility requirement. */
    fun step(delta: Float) = applyRatio(_zoom.value + delta)

    fun fraction(): Float {
        val (min, max) = _range.value
        if (max <= min) return 0f
        return ((_zoom.value - min) / (max - min)).coerceIn(0f, 1f)
    }

    private fun applyRatio(requested: Float) {
        val (min, max) = _range.value
        val ratio = requested.coerceIn(min, max)
        if (ratio == _zoom.value) return
        runCatching { controller?.setZoomRatio(ratio) }
        _zoom.value = ratio
        _zoomChangedAt.value = System.currentTimeMillis()
    }
}
