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

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import kotlin.math.min

/**
 * A point to be shown on sonar when detected.
 * - [angle] direction when facing north in degrees (0-359, 0 - north/top, 90 - east/right, etc.),
 * - [distance] distance from the center of a sonar (0f-1f, 0f - center, 1f - boundary),
 * - [visibilityMillis] amount of time during which the point is still visible after detection.
 */
class SonarPoint(
    @IntRange(from = 0, 359)
    val angle: Int,
    @FloatRange(from = 0.0, 1.0)
    val distance: Float,
    val color: Int? = null,
    val visibilityMillis: Long = DEFAULT_VISIBILITY_MILLIS,
) {
    val isVisible: Boolean
        get() = (visibility > 0 && detectionDistance <= 1f) || visibilityMillis == INFINITE_VISIBILITY

    var visibility: Float = 0f
        private set

    var detectionAngle: Int = 0
        private set

    var detectionDistance: Float = 0f
        private set

    private var detectionMillis: Long = 0L
    private var lastAngleDiff: Int = 0

    fun detect(currentMillis: Long, sonarAngle: Int, scannerAngle: Int) {
        val pointAngle = sonarAngle + angle
        val angleDiff = SonarUtils.getAngleDiff(
            SonarUtils.getOffsetAngle(scannerAngle),
            SonarUtils.getOffsetAngle(pointAngle)
        )
        if (lastAngleDiff > 0 && angleDiff <= 0) {
            detectionMillis = currentMillis
            detectionAngle = pointAngle
            detectionDistance = distance
        }
        visibility = if (visibilityMillis == INFINITE_VISIBILITY) {
            1f
        } else {
            1 - min(1f, (currentMillis - detectionMillis) / visibilityMillis.toFloat())
        }
        lastAngleDiff = angleDiff
    }

    companion object {
        private const val DEFAULT_VISIBILITY_MILLIS = 1250L
        const val INFINITE_VISIBILITY = -1L
    }
}
