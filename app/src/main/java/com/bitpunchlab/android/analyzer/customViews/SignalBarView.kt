package com.bitpunchlab.android.analyzer.customViews

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View


class SignalBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private val pointPosition: PointF = PointF(0.0f, 0.0f)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // change the width and height of the bar accordingly
    }
}