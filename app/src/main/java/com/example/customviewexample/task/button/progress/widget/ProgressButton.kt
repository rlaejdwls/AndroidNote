package com.example.customviewexample.task.button.progress.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.customviewexample.R
import com.example.customviewexample.task.button.progress.temp.CircularProgressViewListener
import java.util.*

class ProgressButton: AppCompatButton {
    private val INDETERMINANT_MIN_SWEEP = 15f

    private var paint: Paint? = null
    private var size = 0
    private var bounds: RectF? = null

    private var isIndeterminate: Boolean = false
    private var autostartAnimation:Boolean = false
    private var currentProgress: Float = 0.toFloat()
    private var maxProgress:Float = 0.toFloat()
    private var indeterminateSweep:Float = 0.toFloat()
    private var indeterminateRotateOffset:Float = 0.toFloat()
    private var thickness: Int = 0
    private var color:Int = 0
    private var animDuration:Int = 0
    private var animSwoopDuration:Int = 0
    private var animSyncDuration:Int = 0
    private var animSteps:Int = 0

    private var listeners: MutableList<CircularProgressViewListener>? = null
    // Animation related stuff
    private var startAngle: Float = 0.toFloat()
    private var actualProgress: Float = 0.toFloat()
    private var startAngleRotate: ValueAnimator? = null
    private var progressAnimator: ValueAnimator? = null
    private var indeterminateAnimator: AnimatorSet? = null
    private var initialStartAngle: Float = 0.toFloat()
    private var marginTop: Int = 0
    private var marginBottom: Int = 0
    private var marginLeft: Int = 0
    private var marginRight: Int = 0

    constructor(context: Context): super(context) {
        init(null, 0)
    }
    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        init(attrs, 0)
    }
    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    protected fun init(attrs: AttributeSet?, defStyle: Int) {
        listeners = ArrayList()

        initAttributes(attrs, defStyle)

        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        updatePaint()

        bounds = RectF()
    }

    private fun initAttributes(attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.ProgressButton, defStyle, 0)

        val resources = resources

        currentProgress = a.getFloat(R.styleable.ProgressButton_cpv_progress,
                resources.getInteger(R.integer.cpv_default_progress).toFloat())
        maxProgress = a.getFloat(R.styleable.ProgressButton_cpv_maxProgress,
                resources.getInteger(R.integer.cpv_default_max_progress).toFloat())
        thickness = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_thickness,
                resources.getDimensionPixelSize(R.dimen.cpv_default_thickness))
        isIndeterminate = a.getBoolean(R.styleable.ProgressButton_cpv_indeterminate,
                resources.getBoolean(R.bool.cpv_default_is_indeterminate))
        autostartAnimation = a.getBoolean(R.styleable.ProgressButton_cpv_animAutostart,
                resources.getBoolean(R.bool.cpv_default_anim_autostart))
        initialStartAngle = a.getFloat(R.styleable.ProgressButton_cpv_startAngle,
                resources.getInteger(R.integer.cpv_default_start_angle).toFloat())
        startAngle = initialStartAngle

        val margin = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_margin, 0)
        if (margin == 0) {
            marginTop = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_marginTop, 0)
            marginBottom = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_marginBottom, 0)
            marginLeft = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_marginLeft, 0)
            marginBottom = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_marginRight, 0)
        } else {
            marginTop = margin
            marginBottom = margin
            marginLeft = margin
            marginRight = margin
        }

        val accentColor = context.resources.getIdentifier("colorAccent", "attr", context.packageName)

        // If color explicitly provided
        color = when {
            a.hasValue(R.styleable.ProgressButton_cpv_color) -> a.getColor(R.styleable.ProgressButton_cpv_color, resources.getColor(R.color.cpv_default_color))
            accentColor != 0 -> {
                val t = TypedValue()
                context.theme.resolveAttribute(accentColor, t, true)
                t.data
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                val t = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorAccent))
                t.getColor(0, ContextCompat.getColor(context, R.color.cpv_default_color))
            }
            else -> ContextCompat.getColor(context, R.color.cpv_default_color)
        }
        animDuration = a.getInteger(R.styleable.ProgressButton_cpv_animDuration,
                resources.getInteger(R.integer.cpv_default_anim_duration))
        animSwoopDuration = a.getInteger(R.styleable.ProgressButton_cpv_animSwoopDuration,
                resources.getInteger(R.integer.cpv_default_anim_swoop_duration))
        animSyncDuration = a.getInteger(R.styleable.ProgressButton_cpv_animSyncDuration,
                resources.getInteger(R.integer.cpv_default_anim_sync_duration))
        animSteps = a.getInteger(R.styleable.ProgressButton_cpv_animSteps,
                resources.getInteger(R.integer.cpv_default_anim_steps))
        a.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        size = if (w < h) w else h
        updateBounds()
    }

    private fun updateBounds() {
        val start = (width / 2) - (size / 2)
        bounds!!.set((start + marginLeft + thickness).toFloat(), (marginTop + thickness).toFloat(), (start + size - marginRight - thickness).toFloat(), (size - marginBottom - thickness).toFloat())
    }

    private fun updatePaint() {
        paint!!.color = color
        paint!!.style = Paint.Style.STROKE
        paint!!.strokeWidth = thickness.toFloat()
        paint!!.strokeCap = Paint.Cap.BUTT
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the arc
        val sweepAngle = if (isInEditMode) currentProgress / maxProgress * 360 else actualProgress / maxProgress * 360
        if (!isIndeterminate)
            canvas.drawArc(bounds!!, startAngle, sweepAngle, false, paint!!)
        else
            canvas.drawArc(bounds!!, startAngle + indeterminateRotateOffset, indeterminateSweep, false, paint!!)
    }

    /**
     * Returns the mode of this view (determinate or indeterminate).
     * @return true if this view is in indeterminate mode.
     */
    fun isIndeterminate(): Boolean {
        return isIndeterminate
    }

    /**
     * Sets whether this CircularProgressView is indeterminate or not.
     * It will reset the animation if the mode has changed.
     * @param isIndeterminate True if indeterminate.
     */
    fun setIndeterminate(isIndeterminate: Boolean) {
        val old = this.isIndeterminate
        val reset = this.isIndeterminate != isIndeterminate
        this.isIndeterminate = isIndeterminate
        if (reset)
            resetAnimation()
        if (old != isIndeterminate) {
            for (listener in listeners!!) {
                listener.onModeChanged(isIndeterminate)
            }
        }
    }

    /**
     * Get the thickness of the progress bar arc.
     * @return the thickness of the progress bar arc
     */
    fun getThickness(): Int {
        return thickness
    }

    /**
     * Sets the thickness of the progress bar arc.
     * @param thickness the thickness of the progress bar arc
     */
    fun setThickness(thickness: Int) {
        this.thickness = thickness
        updatePaint()
        updateBounds()
        invalidate()
    }

    /**
     *
     * @return the color of the progress bar
     */
    fun getColor(): Int {
        return color
    }

    /**
     * Sets the color of the progress bar.
     * @param color the color of the progress bar
     */
    fun setColor(color: Int) {
        this.color = color
        updatePaint()
        invalidate()
    }

    /**
     * Gets the progress value considered to be 100% of the progress bar.
     * @return the maximum progress
     */
    fun getMaxProgress(): Float {
        return maxProgress
    }

    /**
     * Sets the progress value considered to be 100% of the progress bar.
     * @param maxProgress the maximum progress
     */
    fun setMaxProgress(maxProgress: Float) {
        this.maxProgress = maxProgress
        invalidate()
    }

    /**
     * @return current progress
     */
    fun getProgress(): Float {
        return currentProgress
    }

    /**
     * Sets the progress of the progress bar.
     *
     * @param currentProgress the new progress.
     */
    fun setProgress(currentProgress: Float) {
        this.currentProgress = currentProgress
        // Reset the determinate animation to approach the new currentProgress
        if (!isIndeterminate) {
            if (progressAnimator != null && progressAnimator!!.isRunning)
                progressAnimator!!.cancel()
            progressAnimator = ValueAnimator.ofFloat(actualProgress, currentProgress)
            progressAnimator!!.duration = animSyncDuration.toLong()
            progressAnimator!!.interpolator = LinearInterpolator()
            progressAnimator!!.addUpdateListener { animation ->
                actualProgress = animation.animatedValue as Float
                invalidate()
            }
            progressAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    for (listener in listeners!!) {
                        listener.onProgressUpdateEnd(currentProgress)
                    }
                }
            })

            progressAnimator!!.start()
        }
        invalidate()
        for (listener in listeners!!) {
            listener.onProgressUpdate(currentProgress)
        }
    }

    /**
     * Register a CircularProgressViewListener with this View
     * @param listener The listener to register
     */
    fun addListener(listener: CircularProgressViewListener?) {
        if (listener != null)
            listeners!!.add(listener)
    }

    /**
     * Unregister a CircularProgressViewListener with this View
     * @param listener The listener to unregister
     */
    fun removeListener(listener: CircularProgressViewListener) {
        listeners!!.remove(listener)
    }

    /**
     * Starts the progress bar animation.
     * (This is an alias of resetAnimation() so it does the same thing.)
     */
    fun startAnimation() {
        resetAnimation()
    }

    /**
     * Resets the animation.
     */
    fun resetAnimation() {
        // Cancel all the old animators
        if (startAngleRotate != null && startAngleRotate!!.isRunning)
            startAngleRotate!!.cancel()
        if (progressAnimator != null && progressAnimator!!.isRunning)
            progressAnimator!!.cancel()
        if (indeterminateAnimator != null && indeterminateAnimator!!.isRunning)
            indeterminateAnimator!!.cancel()

        // Determinate animation
        if (!isIndeterminate) {
            // The cool 360 swoop animation at the start of the animation
            startAngle = initialStartAngle
            startAngleRotate = ValueAnimator.ofFloat(startAngle, startAngle + 360)
            startAngleRotate!!.duration = animSwoopDuration.toLong()
            startAngleRotate!!.interpolator = DecelerateInterpolator(2f)
            startAngleRotate!!.addUpdateListener { animation ->
                startAngle = animation.animatedValue as Float
                invalidate()
            }
            startAngleRotate!!.start()

            // The linear animation shown when progress is updated
            actualProgress = 0f
            progressAnimator = ValueAnimator.ofFloat(actualProgress, currentProgress)
            progressAnimator!!.duration = animSyncDuration.toLong()
            progressAnimator!!.interpolator = LinearInterpolator()
            progressAnimator!!.addUpdateListener { animation ->
                actualProgress = animation.animatedValue as Float
                invalidate()
            }
            progressAnimator!!.start()
        } else {
            indeterminateSweep = INDETERMINANT_MIN_SWEEP
            // Build the whole AnimatorSet
            indeterminateAnimator = AnimatorSet()
            var prevSet: AnimatorSet? = null
            var nextSet: AnimatorSet
            for (k in 0 until animSteps) {
                nextSet = createIndeterminateAnimator(k.toFloat())
                val builder = indeterminateAnimator!!.play(nextSet)
                if (prevSet != null)
                    builder.after(prevSet)
                prevSet = nextSet
            }

            // Listen to end of animation so we can infinitely loop
            indeterminateAnimator!!.addListener(object : AnimatorListenerAdapter() {
                internal var wasCancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    wasCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!wasCancelled)
                        resetAnimation()
                }
            })
            indeterminateAnimator!!.start()
            for (listener in listeners!!) {
                listener.onAnimationReset()
            }
        }// Indeterminate animation


    }

    /**
     * Stops the animation
     */

    fun stopAnimation() {
        if (startAngleRotate != null) {
            startAngleRotate!!.cancel()
            startAngleRotate = null
        }
        if (progressAnimator != null) {
            progressAnimator!!.cancel()
            progressAnimator = null
        }
        if (indeterminateAnimator != null) {
            indeterminateAnimator!!.cancel()
            indeterminateAnimator = null
        }
    }

    // Creates the animators for one step of the animation
    private fun createIndeterminateAnimator(step: Float): AnimatorSet {
        val maxSweep = 360f * (animSteps - 1) / animSteps + INDETERMINANT_MIN_SWEEP
        val start = -90f + step * (maxSweep - INDETERMINANT_MIN_SWEEP)

        // Extending the front of the arc
        val frontEndExtend = ValueAnimator.ofFloat(INDETERMINANT_MIN_SWEEP, maxSweep)
        frontEndExtend.duration = (animDuration / animSteps / 2).toLong()
        frontEndExtend.interpolator = DecelerateInterpolator(1f)
        frontEndExtend.addUpdateListener { animation ->
            indeterminateSweep = animation.animatedValue as Float
            invalidate()
        }

        // Overall rotation
        val rotateAnimator1 = ValueAnimator.ofFloat(step * 720f / animSteps, (step + .5f) * 720f / animSteps)
        rotateAnimator1.duration = (animDuration / animSteps / 2).toLong()
        rotateAnimator1.interpolator = LinearInterpolator()
        rotateAnimator1.addUpdateListener { animation -> indeterminateRotateOffset = animation.animatedValue as Float }

        // Followed by...

        // Retracting the back end of the arc
        val backEndRetract = ValueAnimator.ofFloat(start, start + maxSweep - INDETERMINANT_MIN_SWEEP)
        backEndRetract.duration = (animDuration / animSteps / 2).toLong()
        backEndRetract.interpolator = DecelerateInterpolator(1f)
        backEndRetract.addUpdateListener { animation ->
            startAngle = animation.animatedValue as Float
            indeterminateSweep = maxSweep - startAngle + start
            invalidate()
        }

        // More overall rotation
        val rotateAnimator2 = ValueAnimator.ofFloat((step + .5f) * 720f / animSteps, (step + 1) * 720f / animSteps)
        rotateAnimator2.duration = (animDuration / animSteps / 2).toLong()
        rotateAnimator2.interpolator = LinearInterpolator()
        rotateAnimator2.addUpdateListener { animation -> indeterminateRotateOffset = animation.animatedValue as Float }

        val set = AnimatorSet()
        set.play(frontEndExtend).with(rotateAnimator1)
        set.play(backEndRetract).with(rotateAnimator2).after(rotateAnimator1)
        return set
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autostartAnimation)
            startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun setVisibility(visibility: Int) {
        val currentVisibility = getVisibility()
        super.setVisibility(visibility)
        if (visibility != currentVisibility) {
            if (visibility == View.VISIBLE) {
                resetAnimation()
            } else if (visibility == View.GONE || visibility == View.INVISIBLE) {
                stopAnimation()
            }
        }
    }
}