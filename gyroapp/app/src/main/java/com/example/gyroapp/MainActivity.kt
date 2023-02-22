package com.example.gyroapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.usb.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.conjugate
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    private lateinit var currentOrient : FloatArray
    private var thrust = 0f
    private var firstTouch = true

    private lateinit var quarternion: Quaternion
    private var oldQuaternion = Quaternion(0f,0f,0f,0f)
    
    private lateinit var dQ: Quaternion
    private var diffEuler = FloatArray(3)
    private var Q = FloatArray(4)

    private lateinit var rawOrientationInfo: TextView
    private lateinit var relativeInfo: TextView
    private lateinit var thrustInfo: TextView
    private lateinit var connectInfo: TextView
    private lateinit var visualInfo:TextView

    private lateinit var yawGauge: GaugeView
    private lateinit var pitchGauge: GaugeView
    private lateinit var rollGauge: GaugeView

    private lateinit var button: Button
    private lateinit var portField: TextView
    private lateinit var addressField: TextView

    private lateinit var client:Socket
    private lateinit var printWriter: PrintWriter
    private var data = "0.0000010.0000010.0000010.000001"
    private var isConnected=false
    private var isWriting = false

    private var port = 0
    private var address = ""


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        rawOrientationInfo = findViewById(R.id.textView2)
        thrustInfo = findViewById(R.id.textView10)
        relativeInfo = findViewById(R.id.relativeInfo)
        connectInfo = findViewById(R.id.connectInfo)
        button = findViewById(R.id.button)
        addressField = findViewById(R.id.addressFIeld)
        portField = findViewById(R.id.portField)
        visualInfo = findViewById(R.id.visual)
        yawGauge = findViewById(R.id.yawGauge)
        pitchGauge = findViewById(R.id.pitchGauge)
        rollGauge = findViewById(R.id.rollGauge)
        pitchGauge.setDanger(true)
        rollGauge.setDanger(true)


        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_UI)


        val clutch:ClutchView = findViewById(R.id.clutchView2)
        var pos = 0f;var size = 0f
        clutch.setOnTouchListener { view, motionEvent ->
            if (firstTouch) { firstTouch = false
                size = clutch.hei.toFloat()
                pos = size*0.5f
                return@setOnTouchListener true
            }
            if (abs(clamp(motionEvent.y,size,size)-pos)>size*0.05f) return@setOnTouchListener true
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                isWriting = false;pos =size*0.5f;clutch.setPointer(pos)
                visualInfo.rotation =0f
                visualInfo.rotationX =0f
                visualInfo.rotationY =0f
            }
            else if (motionEvent.action == MotionEvent.ACTION_DOWN){ isWriting =true;
                oldQuaternion = quarternion
            }
            else {
                pos = clamp(motionEvent.y, size, size) ;clutch.setPointer(pos)
            }

            thrust = (size*0.5f-pos).let{ if (abs(it)<size*0.1f) 0f else (it-sign(it)*size*0.1f)/(size*0.4f)}
            thrustInfo.text = "thrust: ${thrust}"
            return@setOnTouchListener true
        }

        button.setOnClickListener {
            try {
                port = Integer.parseInt(portField.text.toString())
                address = addressField.text.toString()
            } catch (_:java.lang.NumberFormatException){
                Toast.makeText(this,"Invalid input",Toast.LENGTH_SHORT).show()
            }
            if (!isConnected) Thread(ClientThread()).start()
        }
    }
    inner class ClientThread() : Runnable {
        override fun run() {
            try {
                client = Socket(address, port) // connect to server
                isConnected = true
                runOnUiThread { connectInfo.text = if (isConnected) "Connected" else "Error" }
                while (Thread.currentThread().isAlive) {
                    if (isWriting) {
                        client = Socket(address, port)
                        data = thrust.toString().toData() +
                                (diffEuler[0]*180/ PI.toFloat()).toString().toData() +
                                (diffEuler[1]*180/ PI.toFloat()).toString().toData() +
                                (diffEuler[2]*180/ PI.toFloat()).toString().toData()
                        Log.i("bruh", data)
                        printWriter = PrintWriter(client.getOutputStream(), true)
                        printWriter.write(data) // write th0e message to output stream
                        printWriter.flush()
                        Thread.sleep(100)
                        printWriter.close()
                        client.close()
                    }
                }
            } catch (e: IOException) {
                Log.i("bruh", "exception")
                e.printStackTrace()
                isConnected = false
            }
            runOnUiThread { connectInfo.text = if (isConnected) "Connected" else "Error" }
        }
    }
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_UI)
    }


    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent) {
        currentOrient = event.values
        SensorManager.getQuaternionFromVector(Q,currentOrient)
        quarternion = Quaternion(Q[1],Q[2],Q[3],Q[0])


        if (this::rawOrientationInfo.isInitialized){
            rawOrientationInfo.text = "w\t: ${quarternion[0]}\n" +
                    "x\t: ${quarternion[1]}\n" +
                    "y\t: ${quarternion[2]}\n+" +
                    "z: ${quarternion[3]}"
        }
        if (this::relativeInfo.isInitialized){
            dQ = conjugate(oldQuaternion)*quarternion
            diffEuler[0] = atan2(2*(dQ[3]*dQ[2]+dQ[0]*dQ[1]),1-2*(dQ[1].pow(2)+dQ[2].pow(2)))
            diffEuler[1] = atan2(2*(dQ[3]*dQ[0]+dQ[1]*dQ[2]),1-2*(dQ[0].pow(2)+dQ[1].pow(2)))
            diffEuler[2] = asin(2*(dQ[3]*dQ[1]-dQ[2]*dQ[0]))

            yawGauge.setPointer(diffEuler[0]*180/ PI.toFloat())
            pitchGauge.setPointer(diffEuler[1]*180/ PI.toFloat())
            rollGauge.setPointer(diffEuler[2]*180/ PI.toFloat())

            relativeInfo.text = "yaw\t: ${diffEuler[0]*180/ PI.toFloat()}\n" +
                    "pitch\t: ${diffEuler[1]*180/ PI.toFloat()}\n" +
                    "roll\t: ${diffEuler[2]*180/ PI.toFloat()}"
        }
        if (isWriting) {


            visualInfo.rotation = diffEuler[0]*180/ PI.toFloat() //yaw
            visualInfo.rotationX = diffEuler[1]*180/ PI.toFloat() //yaw
            visualInfo.rotationY = diffEuler[2]*180/ PI.toFloat() //roll
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }



}