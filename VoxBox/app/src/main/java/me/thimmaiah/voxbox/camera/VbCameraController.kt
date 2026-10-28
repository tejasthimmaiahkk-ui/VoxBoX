package me.thimmaiah.voxbox.camera

import androidx.camera.core.ZoomState
import androidx.camera.view.CameraController
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
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
 *
 * The user's chosen ratio is the source of truth, not the camera. CameraX reopens at 1× whenever
 * the controller is rebound (pause/resume, a sampling-interval change), which used to drop the
 * student's zoom and lose the board. [ZoomState] is observed so a reopened camera is put straight
 * back to [desired], and the controller's own pinch-to-zoom is disabled so only this class moves it.
 */
class VbCameraController(initialRatio: Float? = null) {
    private var controller: CameraController? = null
    private var observer: Observer<ZoomState>? = null

    /** What the user last asked for; null until they zoom, so the camera's default stands. */
    var desired: Float? = initialRatio
        private set

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

    fun bind(controller: CameraController, owner: LifecycleOwner) {
        unbind()
        this.controller = controller
        controller.isPinchToZoomEnabled = false
        val zoomObserver = Observer<ZoomState> { state -> onCameraZoom(state) }
        observer = zoomObserver
        controller.zoomState.observe(owner, zoomObserver)
    }

    private fun onCameraZoom(state: ZoomState) {
        _range.value = state.minZoomRatio to state.maxZoomRatio
        val want = desired?.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        if (want != null && kotlin.math.abs(state.zoomRatio - want) > 0.01f) {
            // A reopened camera reports its default; restore the user's framing instead.
            runCatching { controller?.setZoomRatio(want) }
            _zoom.value = want
        } else {
            _zoom.value = state.zoomRatio
        }
    }

    fun unbind() {
        observer?.let { controller?.zoomState?.removeObserver(it) }
        observer = null
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
        desired = ratio
        if (ratio == _zoom.value) return
        runCatching { controller?.setZoomRatio(ratio) }
        _zoom.value = ratio
        _zoomChangedAt.value = System.currentTimeMillis()
    }
}
