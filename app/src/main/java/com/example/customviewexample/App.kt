package com.example.customviewexample

import android.app.Application
import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Created by Hwang on 2018-10-08.
 *
 * Description :
 */
class App: Application() {
    companion object {
        private lateinit var app: App

        fun get(): App {
            return app
        }
    }

    val point = Point()
    var statusBarHeight: Int = 0
    var density: Float = 0f

    override fun onCreate() {
        super.onCreate()
        app = this

        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).run {
            defaultDisplay.getSize(point)
            DisplayMetrics().run {
                defaultDisplay.getMetrics(this)
                this@App.density = this.density
            }
        }
        statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
    }
}