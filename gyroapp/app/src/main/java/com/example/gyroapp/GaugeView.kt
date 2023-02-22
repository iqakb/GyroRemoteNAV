package com.example.gyroapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import dev.romainguy.kotlin.math.radians
import kotlin.math.*

private fun Paint.initpaintcolor(context: Context) {
    val typedValue = TypedValue();
    context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.color = Color.WHITE
    } else {
        this.color = ContextCompat.getColor(context, typedValue.resourceId)
    }
}

class GaugeView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private var paint: Paint
    private var pointerpaint: Paint
    private var pos:Float = 0F
    private var danger =false
    private var pt:Int = 10 //pt stands for the pointer thickness in pixel
    var hei:Int = 0
    var wid:Int = 0
    var rad:Int = 0

    init {
        paint = Paint()
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.color = Color.LTGRAY
        } else {
            paint.color = ContextCompat.getColor(context, typedValue.resourceId)
        }
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 25f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.style= Paint.Style.FILL
        pointerpaint = Paint()
        pointerpaint.initpaintcolor(context)
        pointerpaint.strokeWidth =10f
    }

    fun setPointer(position:Float){
        pos = if (abs(position)>90) sign(position)*90f else position
        this.invalidate()
    }

    fun setDanger(bool:Boolean){
        danger=bool
        this.invalidate()
    }

    private fun rotatefromCenter(angle:Float,radius:Float,axis:String):Float{
        return if (axis=="x")  wid*0.5f+radius*cos(PI*0.5f+angle*PI/180).toFloat()
        else hei*0.5f-radius*sin(PI*0.5f+angle*PI/180).toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        hei = h
        wid = w
        rad = min(h,w)
        pos = 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(wid*0.5f,hei*0.5f,rad*0.5f,paint)
        paint.color =Color.RED
        if (danger){
            canvas.drawArc(0f,0f,wid.toFloat(),hei.toFloat(),0f,-10f,true,paint)
            canvas.drawArc(0f,0f,wid.toFloat(),hei.toFloat(),180f,10f,true,paint)
            canvas.drawText("Avoid pointer in red",wid*0.5f,hei*0.75f,paint)
        }
        paint.color = Color.BLACK
        for (i in -90..90 step 10){
            canvas.drawText(i.toString(),rotatefromCenter(i.toFloat(),rad*0.45f,"x"),rotatefromCenter(i.toFloat(),rad*0.45f,"y"),paint)
        }
        paint.color = Color.LTGRAY
        canvas.drawLine(wid*0.5f,hei*0.5f, rotatefromCenter(pos,rad*0.5f,"x"),rotatefromCenter(pos,rad*0.5f,"y"),pointerpaint)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}