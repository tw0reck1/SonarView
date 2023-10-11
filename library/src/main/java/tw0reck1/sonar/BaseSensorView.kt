/*
 * Copyright 2017 Adrian Tworkowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tw0reck1.sonar

import androidx.core.content.getSystemService
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import kotlin.math.atan2
import kotlin.math.min

abstract class BaseSensorView : View, SensorEventListener {

    private var sensorManager: SensorManager? = null

    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private var accelerometerValues: FloatArray? = null
    private var magnetometerValues: FloatArray? = null

    protected val hasSensors: Boolean
        get() = accelerometer != null && magnetometer != null

    private val rotationMatrix = FloatArray(9)
    private val inclinationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    constructor(
        context: Context
    ) : super(context) {
        initSensors()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        initSensors()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initSensors()
    }

    private fun initSensors() {
        sensorManager = context.getSystemService()
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width: Int
        val height: Int
        val horizontalPadding = paddingLeft - paddingRight
        val verticalPadding = paddingTop - paddingBottom
        val availableWidthSize = widthSize - horizontalPadding
        val availableHeightSize = heightSize - verticalPadding
        val availableSize = min(availableWidthSize, availableHeightSize)

        width = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
            availableSize + horizontalPadding
        }
        height = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
            availableSize + verticalPadding
        }

        setMeasuredDimension(width, height)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerSensors()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterSensors()
    }

    private fun registerSensors() {
        val sensorManager = sensorManager ?: return

        listOfNotNull(accelerometer, magnetometer)
            .forEach { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            }
    }

    private fun unregisterSensors() {
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerValues = event.values
        }
        if (accelerometerValues != null && magnetometerValues != null && getRotationMatrix()) {
            SensorManager.getOrientation(rotationMatrix, orientation)

            val azimuth = -atan2(rotationMatrix[3], rotationMatrix[0])

            onOrientationChanged(azimuth, orientation[1], orientation[2])
        }
    }

    private fun getRotationMatrix() : Boolean =
        SensorManager.getRotationMatrix(
            rotationMatrix,
            inclinationMatrix,
            accelerometerValues,
            magnetometerValues
        )

    protected abstract fun onOrientationChanged(azimuth: Float, pitch: Float, roll: Float)
}