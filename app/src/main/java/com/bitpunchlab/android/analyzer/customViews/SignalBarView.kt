package com.bitpunchlab.android.analyzer.customViews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bitpunchlab.android.analyzer.R


class SignalBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private val pointPosition: PointF = PointF(10.0f, 10.0f)
    private var positionX = 0
    private var positionY = 0
    private var posXIncrement = 0
    private var strength = 0
    private var rect_scale : Int = 0
    @SuppressLint("ResourceType")
    @LayoutRes
    private var signalColor : Int = R.color.signals_wifi

    @SuppressLint("ResourceType")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 18.0f
        typeface = Typeface.create( "", Typeface.BOLD)
        color = ContextCompat.getColor(context, R.color.signals_wifi)
        setBackgroundColor(Color.WHITE)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // change the width and height of the bar accordingly
        //positionX = (w * 0.93).toInt()
        positionX = w
        positionY = (h).toInt()
        posXIncrement = (w * 0.3).toInt()
        rect_scale = h//(h * 0.5).toInt()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        //drawRectangle(10, 10, canvas!!)
        drawBar((pointPosition.x).toInt(), (pointPosition.y).toInt(), strength, canvas!!)
        canvas
    }

    fun setStrength(level: Int) {
        strength = level
        Log.i("set Strength", "got $strength")
    }

    fun setSignalColor(color: Int) {
        signalColor = color
        Log.i("set Strength", "got $signalColor")
        //paint.color = color
        paint.color = ContextCompat.getColor(context, color)
        //paint.background = ContextCompat.getColor(context, R.color.white)
    }

    private fun drawBar(x: Int, y: Int, level: Int, canvas: Canvas) {
        var posX = 0

        for (i in 1..level) {
            drawRectangle(x + posX, y, canvas)
            posX += posXIncrement
        }
    }

    // ??
    private fun drawRectangle(x: Int, y: Int, canvas: Canvas) {

        val rect = Rect((x-0.8*rect_scale).toInt(), (y-0.5*rect_scale).toInt(),
            (x+0.8*rect_scale).toInt(), (y+rect_scale).toInt())

        canvas.drawRect(rect, paint)
    }
}

/*
    //private var strength = MutableLiveData<Int>(0)
    init {
        strength.observe(rootView as LifecycleOwner, Observer { level ->
            // calculate pointPosition
            // create a loop of count to strength
            // go through the loop to draw one rect a time
            pointPosition.x = 10.0f
            pointPosition.y = 10.0f

            for (i in 1..level) {
                //drawRectangle()
            }
            // move the pointPosition

        })
    }
*/