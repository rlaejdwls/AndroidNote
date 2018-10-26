package com.example.customviewexample.task.spannable.gridview.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.customviewexample.R
import com.example.customviewexample.task.spannable.gridview.manager.SpanLayoutParams
import com.example.customviewexample.task.spannable.gridview.manager.SpanSize
import com.example.customviewexample.task.spannable.gridview.view.SpannableSquareView
import kotlinx.android.synthetic.main.item_spannable_grid.view.*

/**
 * Created by Hwang on 2018-10-12.
 *
 * Description :
 */
class SpannableGridAdapter(private val context: Context): RecyclerView.Adapter<SpannableGridAdapter.ViewHolder>() {
    private val items: ArrayList<String> = ArrayList<String>().apply {
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
        add("http://treegames.co.kr/test/data/image/")
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpannableGridAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_spannable_grid, parent, false))
    }
    override fun onBindViewHolder(holder: SpannableGridAdapter.ViewHolder, position: Int) {
        val size = if (((position % 10) == 0) || ((position % 10) == 7)) { 2 } else { 1 }
//        val width = if (((position % 10) == 0) || ((position % 10) == 7)) { 2 } else { 1 }
//        val height = if (((position % 10) == 0) || ((position % 10) == 7)) { 2 } else { 1 }
        holder.itemView.layoutParams = SpanLayoutParams(SpanSize(size, size))

        Glide.with(context)
                .asDrawable()
                .load(items[position] + position.toString())
                .apply(RequestOptions().apply {
                    error(R.drawable.quokka_001_375_375)
                })
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.imgSquare)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgSquare: SpannableSquareView = itemView.img_square
    }
}