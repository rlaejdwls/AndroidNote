package com.example.customviewexample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.customviewexample.task.flexible.imageview.FlexibleImageViewActivity
import com.example.customviewexample.task.number.selector.NumberSelectorActivity
import com.example.customviewexample.task.spannable.gridview.SpannableGridViewActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_number_selector.setOnClickListener { startActivity(Intent(this, NumberSelectorActivity::class.java)) }
        btn_flexible_image_view.setOnClickListener { startActivity(Intent(this, FlexibleImageViewActivity::class.java)) }
        btn_spannable_grid_view.setOnClickListener { startActivity(Intent(this, SpannableGridViewActivity::class.java)) }
    }
}
