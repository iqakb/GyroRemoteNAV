package com.example.gyroapp

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.setBlendMode

private fun Paint.initpaintcolor(context: Context) {
    val typedValue = TypedValue();
    context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.color = Color.WHITE
    } else {
        this.color = ContextCompat.getColor(context, typedValue.resourceId)
    }
}

class ClutchView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {

    private var paint: Paint
    private var pointerpaint: Paint
    private var pos:Float = 0F
    private var pt:Int = 10 //pt stands for the pointer thickness in pixel
    var hei:Int = 0
    var wid:Int = 0

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
        paint.textSize = 35f
        paint.typeface = Typeface.DEFAULT_BOLD
        pointerpaint = Paint()
        pointerpaint.initpaintcolor(context)
    }

    fun setPointer(position:Float){
        pos = position
        this.invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        hei = h
        wid = w
        pos = (h*0.5).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(pt.toFloat(),0F,(wid-pt).toFloat(),hei.toFloat(),paint)
        paint.color = Color.DKGRAY
        canvas.drawRect(pt.toFloat(),(hei*0.4).toFloat(),(wid-pt).toFloat(),(hei*0.6).toFloat(),paint)
        paint.color = Color.LTGRAY
        canvas.drawText("DEAD ZONE",(0.5*wid).toFloat(),(0.4*hei+40).toFloat(),paint)
        canvas.drawRect(0f,pos*(hei-pt)/hei,wid.toFloat(),(pos*(hei-pt)/hei)+pt,pointerpaint)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}