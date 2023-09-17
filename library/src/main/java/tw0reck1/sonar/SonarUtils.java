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
package tw0reck1.sonar;

import android.content.res.Resources;
import android.graphics.PointF;
import android.util.TypedValue;

class SonarUtils {

    private SonarUtils() {}

    static float dpToPx(Resources resources, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                resources.getDisplayMetrics());
    }

    static int getAngleFromRadian(float radian) {
        return (int) (-1f * Math.round(radian / Math.PI * 180f));
    }

    // converts from (0,180,359) to (0,180,-179,-1)
    static int getOffsetAngle(int positiveAngle) {
        return (positiveAngle > 180) ? positiveAngle - 360 : positiveAngle;
    }

    static int getAngleDiff(int fromDegrees, int toDegrees) {
        int diff = toDegrees - fromDegrees;

        if (Math.abs(diff) > 180) {
            diff = 180 - Math.abs(toDegrees) + 180 - Math.abs(fromDegrees);
            if (toDegrees > 0) diff *= -1;
        }

        return diff;
    }

    static boolean isClockwiseCloser(int fromDegrees, int toDegrees) {
        return getAngleDiff(fromDegrees, toDegrees) > 0;
    }

    static PointF getPointOnCircle(float x, float y, float radius, int angle) {
        float resultX = (float) (radius * Math.cos((angle - 90f) * Math.PI / 180f)) + x;
        float resultY = (float) (radius * Math.sin((angle - 90f) * Math.PI / 180f)) + y;

        return new PointF(resultX, resultY);
    }

    static int clamp(int value, int min, int max) {
        return Math.min(Math.max(min, value), max);
    }

}