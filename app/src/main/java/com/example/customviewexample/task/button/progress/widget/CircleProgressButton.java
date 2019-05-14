package com.example.customviewexample.task.button.progress.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.example.customviewexample.R;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.AppCompatButton;

public class CircleProgressButton extends AppCompatButton {
    private static final float INDETERMINANT_MIN_SWEEP = 15f;

    private Paint paint;
    private int size = 0;
    private RectF bounds;

    private boolean isIndeterminate, autoStartAnimation;
    private float currentProgress, maxProgress, indeterminateSweep, indeterminateRotateOffset;
    private int thickness, color, animDuration, animSwoopDuration, animSyncDuration, animSteps;

    private List<CircularProgressButtonListener> listeners;
    // Animation related stuff
    private float startAngle;
    private float actualProgress;
    private ValueAnimator startAngleRotate;
    private ValueAnimator progressAnimator;
    private ValueAnimator textAnimator;
    private AnimatorSet indeterminateAnimator;
    private float initialStartAngle;

    private int marginTop = 0;
    private int marginBottom = 0;
    private int marginLeft = 0;
    private int marginRight = 0;

    private int animTextColorAlpha = 0;
    private int textColorAlpha = 0;
    private int textColorRed = 0;
    private int textColorGreen = 0;
    private int textColorBlue = 0;

    public CircleProgressButton(Context context) {
        super(context);
        init(null, 0);
    }
    public CircleProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }
    public CircleProgressButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    protected void init(AttributeSet attrs, int defStyle) {
        listeners = new ArrayList<>();

        initAttributes(attrs, defStyle);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        updatePaint();

        bounds = new RectF();
    }

    private void initAttributes(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CircleProgressButton, defStyle, 0);

        currentProgress = a.getFloat(R.styleable.CircleProgressButton_cpb_progress, 0);
        maxProgress = a.getFloat(R.styleable.CircleProgressButton_cpb_maxProgress, 100);
        thickness = a.getDimensionPixelSize(R.styleable.CircleProgressButton_cpb_thickness, 2);
        isIndeterminate = a.getBoolean(R.styleable.CircleProgressButton_cpb_indeterminate, false);
        autoStartAnimation = a.getBoolean(R.styleable.CircleProgressButton_cpb_animAutoStart, false);
        initialStartAngle = a.getFloat(R.styleable.CircleProgressButton_cpb_startAngle, -90);
        startAngle = initialStartAngle;

        int margin = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_margin, 0);
        if (margin == 0) {
            marginTop = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_marginTop, 0);
            marginBottom = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_marginBottom, 0);
            marginLeft = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_marginLeft, 0);
            marginBottom = a.getDimensionPixelSize(R.styleable.ProgressButton_cpv_marginRight, 0);
        } else {
            marginTop = margin;
            marginBottom = margin;
            marginLeft = margin;
            marginRight = margin;
        }

        int accentColor = getContext().getResources().getIdentifier("colorAccent", "attr", getContext().getPackageName());

        if (a.hasValue(R.styleable.CircleProgressButton_cpb_color)) {
            color = a.getColor(R.styleable.CircleProgressButton_cpb_color, Color.parseColor("#2196F3"));
        } else if(accentColor != 0) {
            TypedValue t = new TypedValue();
            getContext().getTheme().resolveAttribute(accentColor, t, true);
            color = t.data;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TypedArray t = getContext().obtainStyledAttributes(new int[] { android.R.attr.colorAccent });
            color = t.getColor(0, Color.parseColor("#2196F3"));
        } else {
            //Use default color
            color = Color.parseColor("#2196F3");
        }
        textColorAlpha = Color.alpha(color);
        textColorRed = Color.red(color);
        textColorGreen = Color.green(color);
        textColorBlue = Color.blue(color);

        animDuration = a.getInteger(R.styleable.CircleProgressButton_cpb_animDuration, 4000);
        animSwoopDuration = a.getInteger(R.styleable.CircleProgressButton_cpb_animSwoopDuration, 5000);
        animSyncDuration = a.getInteger(R.styleable.CircleProgressButton_cpb_animSyncDuration, 500);
        animSteps = a.getInteger(R.styleable.CircleProgressButton_cpb_animSteps, 3);
        a.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        size = (w < h) ? w : h;
        updateBounds();
    }

    private void updateBounds() {
        float start = (getWidth() / 2) - (size / 2);
        bounds.set(start + marginLeft + thickness, marginTop + thickness, start + size - marginRight - thickness, size - marginBottom - thickness);
    }

    private void updatePaint() {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(thickness);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float sweepAngle = (isInEditMode()) ? currentProgress/maxProgress*360 : actualProgress/maxProgress*360;
        if(!isIndeterminate) {
            canvas.drawArc(bounds, startAngle, sweepAngle, false, paint);
        } else {
            canvas.drawArc(bounds, startAngle + indeterminateRotateOffset, indeterminateSweep, false, paint);
        }
    }

    /**
     * Returns the mode of this view (determinate or indeterminate).
     * @return true if this view is in indeterminate mode.
     */
    public boolean isIndeterminate() {
        return isIndeterminate;
    }

    /**
     * Sets whether this CircularProgressView is indeterminate or not.
     * It will reset the animation if the mode has changed.
     * @param isIndeterminate True if indeterminate.
     */
    public void setIndeterminate(boolean isIndeterminate) {
        boolean old = this.isIndeterminate;
        boolean reset = this.isIndeterminate != isIndeterminate;
        this.isIndeterminate = isIndeterminate;
        if (reset) { resetAnimation(); }
        if(old != isIndeterminate) {
            for(CircularProgressButtonListener listener : listeners) {
                listener.onModeChanged(isIndeterminate);
            }
        }
    }

    /**
     * Get the thickness of the progress bar arc.
     * @return the thickness of the progress bar arc
     */
    public int getThickness() {
        return thickness;
    }

    /**
     * Sets the thickness of the progress bar arc.
     * @param thickness the thickness of the progress bar arc
     */
    public void setThickness(int thickness) {
        this.thickness = thickness;
        updatePaint();
        updateBounds();
        invalidate();
    }

    /**
     *
     * @return the color of the progress bar
     */
    public int getColor() {
        return color;
    }

    /**
     * Sets the color of the progress bar.
     * @param color the color of the progress bar
     */
    public void setColor(int color) {
        this.color = color;
        updatePaint();
        invalidate();
    }

    /**
     * Gets the progress value considered to be 100% of the progress bar.
     * @return the maximum progress
     */
    public float getMaxProgress() {
        return maxProgress;
    }

    /**
     * Sets the progress value considered to be 100% of the progress bar.
     * @param maxProgress the maximum progress
     */
    public void setMaxProgress(float maxProgress) {
        this.maxProgress = maxProgress;
        invalidate();
    }

    /**
     * @return current progress
     */
    public float getProgress() {
        return currentProgress;
    }

    /**
     * Sets the progress of the progress bar.
     *
     * @param currentProgress the new progress.
     */
    public void setProgress(final float currentProgress) {
        this.currentProgress = currentProgress;
        // Reset the determinate animation to approach the new currentProgress
        if (!isIndeterminate) {
            if (progressAnimator != null && progressAnimator.isRunning())
                progressAnimator.cancel();
            progressAnimator = ValueAnimator.ofFloat(actualProgress, currentProgress);
            progressAnimator.setDuration(animSyncDuration);
            progressAnimator.setInterpolator(new LinearInterpolator());
            progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    actualProgress = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            progressAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    for(CircularProgressButtonListener listener : listeners) {
                        listener.onProgressUpdateEnd(currentProgress);
                    }
                }
            });

            progressAnimator.start();
        }
        invalidate();
        for(CircularProgressButtonListener listener : listeners) {
            listener.onProgressUpdate(currentProgress);
        }
    }

    /**
     * Register a CircularProgressButtonListener with this View
     * @param listener The listener to register
     */
    public void addListener(CircularProgressButtonListener listener) {
        if(listener != null) { listeners.add(listener); }
    }

    /**
     * Unregister a CircularProgressButtonListener with this View
     * @param listener The listener to unregister
     */
    public void removeListener(CircularProgressButtonListener listener) {
        listeners.remove(listener);
    }

    public void startAnimation() {
        if(textAnimator != null && textAnimator.isRunning())
            textAnimator.cancel();
        textAnimator = ValueAnimator.ofInt(textColorAlpha, 0);
        textAnimator.setDuration(300);
        textAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animTextColorAlpha = (int) animation.getAnimatedValue();
                setTextColor(Color.argb(animTextColorAlpha, textColorRed, textColorGreen, textColorBlue));
            }
        });
        textAnimator.start();
        resetAnimation();
    }

    public void resetAnimation() {
        // Cancel all the old animators
        if(startAngleRotate != null && startAngleRotate.isRunning())
            startAngleRotate.cancel();
        if(progressAnimator != null && progressAnimator.isRunning())
            progressAnimator.cancel();
        if(indeterminateAnimator != null && indeterminateAnimator.isRunning())
            indeterminateAnimator.cancel();

        // Determinate animation
        if(!isIndeterminate) {
            // The cool 360 swoop animation at the start of the animation
            startAngle = initialStartAngle;
            startAngleRotate = ValueAnimator.ofFloat(startAngle, startAngle + 360);
            startAngleRotate.setDuration(animSwoopDuration);
            startAngleRotate.setInterpolator(new DecelerateInterpolator(2));
            startAngleRotate.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    startAngle = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            startAngleRotate.start();

            // The linear animation shown when progress is updated
            actualProgress = 0f;
            progressAnimator = ValueAnimator.ofFloat(actualProgress, currentProgress);
            progressAnimator.setDuration(animSyncDuration);
            progressAnimator.setInterpolator(new LinearInterpolator());
            progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    actualProgress = (Float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            progressAnimator.start();
        } else {
            indeterminateSweep = INDETERMINANT_MIN_SWEEP;
            // Build the whole AnimatorSet
            indeterminateAnimator = new AnimatorSet();
            AnimatorSet prevSet = null, nextSet;
            for(int k=0;k<animSteps;k++) {
                nextSet = createIndeterminateAnimator(k);
                AnimatorSet.Builder builder = indeterminateAnimator.play(nextSet);
                if(prevSet != null)
                    builder.after(prevSet);
                prevSet = nextSet;
            }

            // Listen to end of animation so we can infinitely loop
            indeterminateAnimator.addListener(new AnimatorListenerAdapter() {
                boolean wasCancelled = false;
                @Override
                public void onAnimationCancel(Animator animation) {
                    wasCancelled = true;
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    if(!wasCancelled) { resetAnimation(); }
                    else {
                        startAngle = -90;
                        indeterminateRotateOffset = 0f;
                        indeterminateSweep = 0f;
                        invalidate();
                    }
                }
            });
            indeterminateAnimator.start();
            for(CircularProgressButtonListener listener : listeners) {
                listener.onAnimationReset();
            }
        }
    }

    public void stopAnimation() {
        if(startAngleRotate != null) {
            startAngleRotate.cancel();
            startAngleRotate = null;
        }
        if(progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
        if(indeterminateAnimator != null) {
            indeterminateAnimator.cancel();
            indeterminateAnimator = null;
        }
        if(textAnimator != null) {
            textAnimator.cancel();
            textAnimator = ValueAnimator.ofInt(animTextColorAlpha, textColorAlpha);
            textAnimator.setDuration(300);
            textAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setTextColor(Color.argb((int) animation.getAnimatedValue(), textColorRed, textColorGreen, textColorBlue));
                }
            });
            textAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    textAnimator = null;
                }
            });
            textAnimator.start();
        }
    }

    // Creates the animators for one step of the animation
    private AnimatorSet createIndeterminateAnimator(float step) {
        final float maxSweep = 360f*(animSteps-1)/animSteps + INDETERMINANT_MIN_SWEEP;
        final float start = -90f + step*(maxSweep-INDETERMINANT_MIN_SWEEP);

        // Extending the front of the arc
        ValueAnimator frontEndExtend = ValueAnimator.ofFloat(INDETERMINANT_MIN_SWEEP, maxSweep);
        frontEndExtend.setDuration(animDuration/animSteps/2);
        frontEndExtend.setInterpolator(new DecelerateInterpolator(1));
        frontEndExtend.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                indeterminateSweep = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });

        // Overall rotation
        ValueAnimator rotateAnimator1 = ValueAnimator.ofFloat(step*720f/animSteps, (step+.5f)*720f/animSteps);
        rotateAnimator1.setDuration(animDuration/animSteps/2);
        rotateAnimator1.setInterpolator(new LinearInterpolator());
        rotateAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                indeterminateRotateOffset = (Float) animation.getAnimatedValue();
            }
        });

        // Retracting the back end of the arc
        ValueAnimator backEndRetract = ValueAnimator.ofFloat(start, start+maxSweep-INDETERMINANT_MIN_SWEEP);
        backEndRetract.setDuration(animDuration/animSteps/2);
        backEndRetract.setInterpolator(new DecelerateInterpolator(1));
        backEndRetract.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                startAngle = (Float) animation.getAnimatedValue();
                indeterminateSweep = maxSweep - startAngle + start;
                invalidate();
            }
        });

        // More overall rotation
        ValueAnimator rotateAnimator2 = ValueAnimator.ofFloat((step + .5f) * 720f / animSteps, (step + 1) * 720f / animSteps);
        rotateAnimator2.setDuration(animDuration / animSteps / 2);
        rotateAnimator2.setInterpolator(new LinearInterpolator());
        rotateAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                indeterminateRotateOffset = (Float) animation.getAnimatedValue();
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.play(frontEndExtend).with(rotateAnimator1);
        set.play(backEndRetract).with(rotateAnimator2).after(rotateAnimator1);
        return set;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(autoStartAnimation) { startAnimation(); }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    @Override
    public void setVisibility(int visibility) {
        int currentVisibility = getVisibility();
        super.setVisibility(visibility);
        if (visibility != currentVisibility) {
            if (visibility == View.VISIBLE) {
                resetAnimation();
            } else if (visibility == View.GONE || visibility == View.INVISIBLE) {
                stopAnimation();
            }
        }
    }

    /**
     * Listener interface to provide different callbacks for Circular Progress Views.
     */
    public interface CircularProgressButtonListener {
        /**
         * Called when setProgress(float currentProgress) is called (determinate only)
         *
         * @param currentProgress The progress that was set.
         */
        void onProgressUpdate(float currentProgress);

        /**
         * Called when this view finishes animating to the updated progress. (Determinate only)
         *
         * @param currentProgress The progress that was set and this view has reached in its animation.
         */
        void onProgressUpdateEnd(float currentProgress);

        /**
         * Called when resetAnimation() is called.
         */
        void onAnimationReset();

        /**
         * Called when you switch between indeterminate and determinate modes.
         *
         * @param isIndeterminate true if mode was set to indeterminate, false otherwise.
         */
        void onModeChanged(boolean isIndeterminate);
    }
}
