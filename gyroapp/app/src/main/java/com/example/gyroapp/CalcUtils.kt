package com.example.gyroapp

import com.google.android.material.math.MathUtils.floorMod
import kotlin.math.abs


//0.12345612.1234523.1234523.4444
fun clamp(position: Float?, scaleori: Float, scaletrans: Float): Float {
    return if (position != null) {
        when {
            position < 0 -> 0F
            position >= scaleori -> scaletrans
            else -> ((position * scaletrans) / scaleori)
        }
    } else 0F
}

fun mod(pos1:Float, pos2:Float):Float{
    return floorMod(pos2-pos1+180,360)-180
}

fun String.toData():String{
    return if (this.length<8) this+"0".repeat(8-this.length)
            else this.substring(0,8)
}