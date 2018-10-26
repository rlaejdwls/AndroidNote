package com.example.customviewexample.task.spannable.gridview.extensions

import android.graphics.Rect

/**
 * Created by Hwang on 2018-10-17.
 *
 * Description :
 */
fun Rect.isAdjacentTo(rect: Rect): Boolean {
    return (this.right == rect.left
            || this.top == rect.bottom
            || this.left == rect.right
            || this.bottom == rect.top)
}

fun Rect.intersects(rect: Rect): Boolean {
    return this.intersects(rect.left, rect.top, rect.right, rect.bottom)
}