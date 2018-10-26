package com.example.customviewexample.task.number.selector

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.customviewexample.App
import com.example.customviewexample.R
import com.example.customviewexample.task.number.selector.widget.Cell
import kotlinx.android.synthetic.main.activity_number_selector.*

/**
 * Created by Hwang on 2018-10-12.
 *
 * Description :
 */
class NumberSelectorActivity: AppCompatActivity() {
    private val percentage: Float = 0.60f
    private var count = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_selector)

        count = 3

        val with: Float = App.get().point.x.toFloat()
//        val height: Float = App.get().point.y.toFloat()

//        val startLeft: Float = (with / 2) - (with * percentage / 2f)
//        val startTop: Float = (height / 2) - (with * percentage / 2f)
        val cellWith: Float = (with * percentage / count) - 2

        for (col in 0..(count - 1)) {
            val layout_column = LinearLayout(this)
            layout_column.orientation = LinearLayout.HORIZONTAL
            layout_column.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            layout_column.gravity = Gravity.CENTER
            for (row in 0..(count - 1)) {
//                val left = startLeft + (cellWith * row)
//                val top = startTop + (cellWith * col)

                val cell = Cell(this)
//                cell.text = (col * 9 + row).toString()
                cell.layoutParams = LinearLayout.LayoutParams(cellWith.toInt() - 1, cellWith.toInt() - 1).apply {
                    this.leftMargin = 1
                    this.rightMargin = 1
                    this.topMargin = 1
                    this.bottomMargin = 1
                }
                cell.setBackgroundColor(Color.argb(255, 100, 100, 100))
                cell.gravity = Gravity.CENTER
                layout_column.addView(cell)
            }
            layout_board.addView(layout_column)
        }
    }
}