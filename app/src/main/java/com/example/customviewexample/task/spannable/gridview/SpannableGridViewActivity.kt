package com.example.customviewexample.task.spannable.gridview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customviewexample.R
import com.example.customviewexample.task.spannable.gridview.adapter.SpannableGridAdapter
import com.example.customviewexample.task.spannable.gridview.manager.SpannedGridLayoutManager
import kotlinx.android.synthetic.main.activity_spannable_grid_view.*

/**
 * Created by Hwang on 2018-10-12.
 *
 * Description :
 */
class SpannableGridViewActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spannable_grid_view)

        grid_spannable.layoutManager = SpannedGridLayoutManager(SpannedGridLayoutManager.Orientation.VERTICAL, 4).apply {
            itemOrderIsStable = true
        }
        grid_spannable.adapter = SpannableGridAdapter(this)
    }
}