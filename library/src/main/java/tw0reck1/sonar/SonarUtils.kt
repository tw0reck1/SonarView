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
import android.graphics.PointF
import android.util.TypedValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal object SonarUtils {

    fun Context.dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    fun Context.spToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dp, resources.displayMetrics)

    // converts radians to degrees
    fun Float.toDegrees(): Int = (-1f * (this / PI * 180f)).roundToInt()

    // converts from (0,180,359) to (0,180,-179,-1)
    fun getOffsetAngle(positiveAngle: Int): Int =
        if (positiveAngle > 180) {
            positiveAngle - 360
        } else {
            positiveAngle
        }

    fun getAngleDiff(fromDegrees: Int, toDegrees: Int): Int {
        var diff = toDegrees - fromDegrees
        if (abs(diff) > 180) {
            diff = 180 - abs(toDegrees) + 180 - abs(fromDegrees)
            if (toDegrees > 0) diff *= -1
        }
        return diff
    }

    fun isClockwiseCloser(fromDegrees: Int, toDegrees: Int): Boolean =
        getAngleDiff(fromDegrees, toDegrees) > 0

    fun getPointOnCircle(x: Float, y: Float, radius: Float, angle: Int): PointF {
        val resultX = (radius * cos((angle - 90f) * PI / 180f)).toFloat() + x
        val resultY = (radius * sin((angle - 90f) * PI / 180f)).toFloat() + y
        return PointF(resultX, resultY)
    }

    fun getDistanceOnArc(center: Float, radius: Float, angle: Int): Float {
        val a = getPointOnCircle(center, center, radius, 0)
        val b = getPointOnCircle(center, center, radius, angle)
        return hypot((a.x - b.x), (a.y - b.y))
    }

    fun Int.clamp(min: Int, max: Int): Int = min(max(min, this), max)
}
