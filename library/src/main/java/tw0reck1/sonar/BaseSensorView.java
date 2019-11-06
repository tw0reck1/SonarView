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

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

abstract class BaseSensorView extends View implements
        SensorEventListener {

    protected SensorManager mSensorManager;
    protected Sensor mAccelerometer, mMagnetometer;

    private float[] mAccelerometerValues, mMagnetometerValues;

    public BaseSensorView(Context context) {
        super(context);
        initSensors();
    }

    public BaseSensorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSensors();
    }

    public BaseSensorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSensors();
    }

    protected void initSensors() {
        mSensorManager = (SensorManager) getContext().getSystemService(Activity.SENSOR_SERVICE);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        registerSensors();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        int horizontalPadding = getPaddingLeft() - getPaddingRight();
        int verticalPadding = getPaddingTop() - getPaddingBottom();

        int availableWidthSize = widthSize - horizontalPadding;
        int availableHeightSize = heightSize - verticalPadding;

        int availableSize = Math.min(availableWidthSize, availableHeightSize);

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else width = availableSize + horizontalPadding;

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else height = availableSize + verticalPadding;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        unregisterSensors();
    }

    protected boolean hasSensors() {
        return mAccelerometer != null && mMagnetometer != null;
    }

    protected void registerSensors() {
        registerSensors(mAccelerometer, mMagnetometer);
    }

    protected void registerSensors(Sensor... sensors) {
        for (Sensor sensor : sensors) {
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    protected void unregisterSensors() {
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometerValues = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mMagnetometerValues = event.values;
        }
        if (mAccelerometerValues != null && mMagnetometerValues != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mAccelerometerValues, mMagnetometerValues)) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                onOrientationChanged(orientation[0], orientation[1], orientation[2]);
            }
        }
    }

    protected abstract void onOrientationChanged(float azimuth, float pitch, float roll);

}