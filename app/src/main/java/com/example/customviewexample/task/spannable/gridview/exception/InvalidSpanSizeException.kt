package com.example.customviewexample.task.spannable.gridview.exception

/**
 * Created by Hwang on 2018-10-17.
 *
 * Description :
 */
class InvalidSpanSizeException(errorSize: Int, maxSpanSize: Int) :
        RuntimeException("Invalid item span size: $errorSize. Span size must be in the range: (1...$maxSpanSize)")