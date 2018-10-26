package com.example.customviewexample.task.spannable.gridview.exception

/**
 * Created by Hwang on 2018-10-17.
 *
 * Description :
 */
class InvalidMaxSpansException(maxSpanSize: Int) :
        RuntimeException("Invalid layout spans: $maxSpanSize. Span size must be at least 1.")