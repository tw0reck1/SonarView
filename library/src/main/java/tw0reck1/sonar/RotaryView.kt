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

import android.content.Context
import android.util.AttributeSet
import tw0reck1.sonar.SonarUtils.getAngleDiff
import tw0reck1.sonar.SonarUtils.isClockwiseCloser
import tw0reck1.sonar.SonarUtils.toDegrees
import java.util.LinkedList
import kotlin.math.abs

open class RotaryView : BaseSensorView {

    private companion object {
        // rotation jump when angle is > DIFFERENCE
        private const val FAST_ROTATE_JUMP = 4
        // angle of rotation tick
        private const val ROTATE_JUMP = 1
        private const val DIFFERENCE = 15
        // minimal difference required to rotate,
        private const val MINIMAL_DIFF = 1
        private const val MEASUREMENTS_COUNT = 3
        // max sensor noise treated as correct value, differences > will be ignored
        private const val SENSOR_DIFF_TOLERANCE = 15
    }

    private val mMeasurementsList: MutableList<Int> = LinkedList()

    var currentAngle: Int = 0
    private var targetAngle: Int = 0
    private val nextAngle: Int
        get() {
            val goClockwise = isClockwiseCloser(currentAngle, targetAngle)
            val rotationAngle = if (abs(getAngleDiff(currentAngle, targetAngle)) < DIFFERENCE) {
                ROTATE_JUMP
            } else {
                FAST_ROTATE_JUMP
            }
            val rotationAngleDiff = if (goClockwise) {
                rotationAngle
            } else {
                -rotationAngle
            }

            var nextAngle = currentAngle + rotationAngleDiff
            if (nextAngle <= -180) {
                nextAngle += 360
            } else if (nextAngle >= 180) {
                nextAngle -= 360
            }
            return nextAngle
        }

    constructor(
        context: Context
    ) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    protected fun updateAngle() {
        if (currentAngle != targetAngle) {
            currentAngle = nextAngle
        }
    }

    override fun onOrientationChanged(azimuth: Float, pitch: Float, roll: Float) {
        val newAngle = azimuth.toDegrees()
        val diff = abs(getAngleDiff(currentAngle, newAngle))

        // values can randomly be equal 0 and will be ignored if the change is too aggressive
        if (diff < MINIMAL_DIFF || diff > SENSOR_DIFF_TOLERANCE && newAngle == 0) return

        mMeasurementsList.add(newAngle)

        if (mMeasurementsList.size >= MEASUREMENTS_COUNT) {
            targetAngle = mMeasurementsList.getAverageAngle(currentAngle)
            mMeasurementsList.clear()
        }
    }

    private fun Collection<Int>.getAverageAngle(currentAngle: Int): Int =
        currentAngle + sumOf { angle -> getAngleDiff(currentAngle, angle) }.div(size)
}
