package com.example.customviewexample.task.button.progress

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.customviewexample.R
import kotlinx.android.synthetic.main.activity_progress.*

class ProgressActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        btn_progress.setOnClickListener { btn_progress.startAnimation() }
        btn_progress_for_java.setOnClickListener {
            btn_progress_for_java.startAnimation()
            Handler().postDelayed({
                btn_progress_for_java.stopAnimation()
            }, 3000)
        }
    }
}