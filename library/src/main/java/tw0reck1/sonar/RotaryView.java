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

import android.content.Context;
import android.util.AttributeSet;

import java.util.LinkedList;
import java.util.List;

class RotaryView extends BaseSensorView {

    public static final int ROTATION_AZIMUTH = 0;
    public static final int ROTATION_PITCH = 1;
    public static final int ROTATION_ROLL = 2;

    private static final int FAST_ROTATE_JUMP = 4; // rotation jump when angle is > DIFFERENCE
    private static final int ROTATE_JUMP = 1; // angle of rotation tick
    private static final int DIFFERENCE = 15;
    private static final int MINIMAL_DIFF = 1; // minimal difference required to rotate,
    private static final int MEASUREMENTS_COUNT = 3;
    private static final int SENSOR_DIFF_TOLERANCE = 15; // max sensor noise treated as correct value, differences > will be ignored

    protected int mCurrentAngle, mDesiredAngle;

    private List<Integer> mMeasurementsList = new LinkedList<>();

    private int mRotationType = ROTATION_AZIMUTH;

    public RotaryView(Context context) {
        super(context);
    }

    public RotaryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RotaryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDesiredAngle(int desiredAngle) {
        if (mDesiredAngle != desiredAngle) {
            mDesiredAngle = desiredAngle;
        }
    }

    public int getDesiredAngle() {
        return mDesiredAngle;
    }

    public void setCurrentAngle(int currentRotation) {
        if (mCurrentAngle != currentRotation) {
            mCurrentAngle = currentRotation;
        }
    }

    public int getCurrentAngle() {
        return mCurrentAngle;
    }

    public void setRotationType(int rotationType) {
        mRotationType = rotationType;

        mMeasurementsList.clear();
    }

    public int getRotationType() {
        return mRotationType;
    }

    protected void updateAngle() {
        if (mCurrentAngle != mDesiredAngle) {
            mCurrentAngle = getNextAngle();
        }
    }

    protected int getNextAngle() {
        boolean goClockwise = SonarUtils.isClockwiseCloser(mCurrentAngle, mDesiredAngle);

        int rotationAngle = (Math.abs(SonarUtils
                .getAngleDiff(mCurrentAngle, mDesiredAngle)) < DIFFERENCE)
                ? ROTATE_JUMP : FAST_ROTATE_JUMP;
        int nextAngle = getCurrentAngle() + (goClockwise ? rotationAngle : -rotationAngle);

        if (nextAngle <= -180) {
            nextAngle += 360;
        } else if (nextAngle >= 180) {
            nextAngle -= 360;
        }

        return nextAngle;
    }

    @Override
    protected void onOrientationChanged(float azimuth, float pitch, float roll) {
        float value = -1;
        if (mRotationType == ROTATION_AZIMUTH) value = azimuth;
        else if (mRotationType == ROTATION_PITCH) value = pitch;
        else if (mRotationType == ROTATION_ROLL) value = roll;

        int angle = SonarUtils.getAngleFromRadian(value);
        int diff = Math.abs(SonarUtils.getAngleDiff(mCurrentAngle, angle));

        // rotation sometimes randomly equals 0 and is treated as error
        // and gets ignored if it differs too much with the last value
        if (diff < MINIMAL_DIFF || (diff > SENSOR_DIFF_TOLERANCE && angle == 0)) return;

        mMeasurementsList.add(angle);

        if (mMeasurementsList.size() >= MEASUREMENTS_COUNT) {
            angle = getAverageAngle(mCurrentAngle);
            mMeasurementsList.clear();

            setDesiredAngle(angle);
        }
    }

    private int getAverageAngle(int currentAngle) {
        int result = 0;

        for (int rotation : mMeasurementsList) {
            result += SonarUtils.getAngleDiff(currentAngle, rotation);
        }
        result /= mMeasurementsList.size();

        return currentAngle + result;
    }

}