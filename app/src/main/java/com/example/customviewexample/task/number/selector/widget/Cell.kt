package com.example.customviewexample.task.number.selector.widget

import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.example.customviewexample.App
import com.example.customviewexample.R

/**
 * Created by Hwang on 2018-10-08.
 *
 * Description :
 */
class Cell(context: Context) : AppCompatTextView(context) {
    private val start: PointF = PointF()
    private var isActionDown = false
    private var number: Int = -1

    private var popupView: View
    private var popupWindow: PopupWindow

    private var popupViewSize: Int = (App.get().density * 50 + 0.5f).toInt()

    init {
        popupView = LayoutInflater.from(context).inflate(R.layout.popup_select_number, null)
        popupWindow = PopupWindow(popupView, popupViewSize, popupViewSize)
        popupWindow.isFocusable = false

//        btn_popup_test.setOnClickListener {
//            if (popupWindow.isShowing) {
//                popupWindow.dismiss()
//            }
//            popupWindow.isFocusable = false
//            popupWindow.showAtLocation(popupView, Gravity.NO_GRAVITY, testX, 0)
//            testX += 50
//        }
    }

    private val paint = Paint().apply {
        color = Color.argb(160, 255, 0, 0)
    }

    private fun getAngle(dx: Double, dy: Double): Double {
        return Math.atan2(dy, dx) * (180.0/Math.PI)
    }
    private fun getNumber(angle:Double): Int {
        if (angle >= -157.5 && angle < -112.5) {
            return 0
        } else if (angle >= -112.5 && angle < -67.5) {
            return 1
        } else if (angle >= -67.5 && angle < -22.5) {
            return 2
        } else if ((angle >= 157.5 && angle <= 180) ||
                (angle >= -180 && angle < -157.5)) {
            return 3
        } else if ((angle >= -22.5 && angle < 0) ||
                (angle >= 0 && angle < 22.5)) {
            return 5
        } else if (angle >= 112.5 && angle < 157.5) {
            return 6
        } else if (angle >= 67.5 && angle < 112.5) {
            return 7
        } else if (angle >= 22.5 && angle < 67.5) {
            return 8
        }
        return -1
    }
    private fun getNumber(event: MotionEvent): Int {
        return if (event.x > 0f && event.y > 0f && event.x < width && event.y < height) {
            4
        } else {
            getNumber(getAngle((event.x - start.x).toDouble(), (event.y - start.y).toDouble()))
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    start.x = event.x
                    start.y = event.y
                    number = 4
                    isActionDown = true
                    if (popupWindow.isShowing) {
                        popupWindow.dismiss()
                    }
                    popupWindow.showAtLocation(popupView, Gravity.NO_GRAVITY, left + (width / 2) - (popupViewSize / 2),
                            ((parent as LinearLayout).top + App.get().statusBarHeight) - popupViewSize - 10)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    number = getNumber(event)
                    popupView.findViewById<TextView>(R.id.txt_select_number).text = (number + 1).toString()
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    number = getNumber(event)
                    text = (number + 1).toString()
                    performClick()
                    isActionDown = false
                    if (popupWindow.isShowing) {
                        popupWindow.dismiss()
                    }
                }
                else -> {
                }
            }
        }
        return super.onTouchEvent(event)
    }
    override fun performClick(): Boolean {
        return super.performClick()
    }
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            if (isActionDown) {
                it.drawRect((width / 3.0f) * (number % 3), (height / 3.0f) * (number / 3),
                        (width / 3.0f) * (number % 3 + 1), (height / 3.0f) * (number / 3 + 1), paint)
            }
        }
    }
}