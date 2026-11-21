package com.rasel.volumebuttons

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import kotlin.math.hypot
import kotlin.math.roundToInt
import android.view.WindowManager.LayoutParams as WindowParams

/**
 * The floating bubble: a small see-through circle resting on one side of the
 * screen, a little tucked past the edge, so it is easy to ignore.
 *
 * Tap it and the phone's own volume controls appear, as if a volume key was
 * pressed. Drag it to move it: it settles on the nearer side and stays where
 * it was left.
 */
@SuppressLint("ClickableViewAccessibility") // TalkBack still gets a click: see setOnClickListener.
class Bubble(private val service: AccessibilityService) {

    private val windows = service.getSystemService(WindowManager::class.java)
    private val audio = service.getSystemService(AudioManager::class.java)
    private val place = service.getSharedPreferences("place", 0)
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
    private val size = dp(SIZE_DP)

    private val params = WindowParams(
        size,
        size,
        // The accessibility service's own overlay: over every app, with no
        // "Display over other apps" permission (Android Go phones lack it).
        WindowParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowParams.FLAG_NOT_FOCUSABLE or
            WindowParams.FLAG_LAYOUT_IN_SCREEN or
            WindowParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= 28) {
            layoutInDisplayCutoutMode = WindowParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private val dot = ImageView(service)

    private var shown = false
    private var onRight = true
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false
    private var slide: ValueAnimator? = null

    private val fadeTask = Runnable { dot.animate().alpha(IDLE_ALPHA).setDuration(400).start() }
    private val touchableTask = Runnable { setTouchable(true) }

    init {
        val pad = size / 4
        dot.apply {
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE6202124.toInt())
                setStroke(dp(1), 0x59FFFFFF)
            }
            setImageResource(R.drawable.ic_speaker)
            contentDescription = service.getString(R.string.app_name)
            alpha = IDLE_ALPHA
            setOnTouchListener { _, e -> onTouch(e) }
            setOnClickListener { showVolume() } // TalkBack
            if (Build.VERSION.SDK_INT >= 29) {
                // Dragging the bubble near the edge shouldn't count as a "back" swipe.
                addOnLayoutChangeListener { v, l, t, r, b, _, _, _, _ ->
                    v.systemGestureExclusionRects = listOf(Rect(0, 0, r - l, b - t))
                }
            }
        }
    }

    fun show() {
        if (shown) return
        moveToSaved()
        windows.addView(dot, params)
        shown = true
    }

    fun hide() {
        if (!shown) return
        slide?.cancel()
        handler.removeCallbacksAndMessages(null)
        runCatching { windows.removeView(dot) }
        shown = false
    }

    /** The screen turned sideways or back. */
    fun onScreenChanged() = moveToSaved()

    /** The phone's own volume controls, as a volume key shows them (call
     *  volume during a call, media while something plays), unchanged. */
    private fun showVolume() {
        audio.adjustSuggestedStreamVolume(
            AudioManager.ADJUST_SAME,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            AudioManager.FLAG_SHOW_UI,
        )
        dot.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        // The controls open over the bubble's side of the screen: let touches
        // pass through the bubble to them for a moment.
        setTouchable(false)
        handler.removeCallbacks(touchableTask)
        handler.postDelayed(touchableTask, CONTROLS_MS)
    }

    private fun setTouchable(on: Boolean) {
        params.flags = if (on) {
            params.flags and WindowParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags or WindowParams.FLAG_NOT_TOUCHABLE
        }
        update()
    }

    private fun onTouch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(fadeTask)
                dot.animate().cancel()
                dot.alpha = 1f // fully visible while touched
                slide?.cancel()
                downX = e.rawX
                downY = e.rawY
                startX = params.x
                startY = params.y
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - downX
                val dy = e.rawY - downY
                if (!dragging && hypot(dx, dy) > touchSlop) dragging = true
                if (dragging) {
                    params.x = (startX + dx).roundToInt().coerceIn(-size / 2, screen().width() - size / 2)
                    params.y = (startY + dy).roundToInt().coerceIn(0, freeHeight())
                    update()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) settle() else if (e.actionMasked == MotionEvent.ACTION_UP) showVolume()
                handler.postDelayed(fadeTask, FADE_DELAY_MS)
            }
        }
        return true
    }

    /** After a drag: slide to the nearer side and remember the place. */
    private fun settle() {
        onRight = params.x + size / 2 > screen().width() / 2
        place.edit()
            .putBoolean(KEY_RIGHT, onRight)
            .putFloat(KEY_Y, params.y / freeHeight().coerceAtLeast(1).toFloat())
            .apply()
        slide = ValueAnimator.ofInt(params.x, sideX()).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                params.x = it.animatedValue as Int
                update()
            }
            start()
        }
    }

    private fun moveToSaved() {
        onRight = place.getBoolean(KEY_RIGHT, true) // the real keys are on the right
        params.x = sideX()
        params.y = (place.getFloat(KEY_Y, 0.3f) * freeHeight()).roundToInt()
        update()
    }

    /** Left edge when resting on its side, partly past the screen edge. */
    private fun sideX(): Int {
        val tucked = (size * TUCK).roundToInt()
        return if (onRight) screen().width() - size + tucked else -tucked
    }

    private fun update() {
        if (shown) windows.updateViewLayout(dot, params)
    }

    private fun screen(): Rect =
        if (Build.VERSION.SDK_INT >= 30) {
            windows.maximumWindowMetrics.bounds
        } else {
            val m = DisplayMetrics()
            @Suppress("DEPRECATION")
            windows.defaultDisplay.getRealMetrics(m)
            Rect(0, 0, m.widthPixels, m.heightPixels)
        }

    private fun freeHeight() = (screen().height() - size).coerceAtLeast(0)

    private fun dp(v: Int) = (v * service.resources.displayMetrics.density).roundToInt()

    private companion object {
        const val SIZE_DP = 42
        /** How much of the bubble hides past the screen edge. */
        const val TUCK = 0.25f
        /** How visible the bubble is while left alone. */
        const val IDLE_ALPHA = 0.3f
        const val FADE_DELAY_MS = 800L
        /** About how long the phone's volume controls stay up. */
        const val CONTROLS_MS = 3000L
        const val KEY_RIGHT = "right"
        const val KEY_Y = "y"
    }
}
