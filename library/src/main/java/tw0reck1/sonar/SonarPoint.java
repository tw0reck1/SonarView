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

public class SonarPoint {

    private static final long DEFAULT_LIFETIME = 1250L;

    private long mDetectionTime;

    private int mDetectedAngle, mLastAngle, mLastScannerAngle;
    private float mDetectedDist, mVisibility;

    private int mAngle;
    private float mDist;
    private long mLifeTime = DEFAULT_LIFETIME;

    public SonarPoint(int angle, float dist) {
        mAngle = angle;
        mDist = dist;
    }

    protected void setData(int angle, float dist) {
        mAngle = angle;
        mDist = dist;
    }

    public void detect(long currentMs, int currentAngle, int newScannerAngle) {
        int newAngle = mAngle + currentAngle;
        int lastDiff = SonarUtils.getAngleDiff(
                SonarUtils.getOffsetAngle(mLastScannerAngle),
                SonarUtils.getOffsetAngle(mLastAngle));
        int newDiff = SonarUtils.getAngleDiff(
                SonarUtils.getOffsetAngle(newScannerAngle),
                SonarUtils.getOffsetAngle(newAngle));

        if (lastDiff > 0 && newDiff <= 0) {
            mDetectionTime = currentMs;
            mDetectedAngle = newAngle;
            mDetectedDist = mDist;
        }

        mVisibility = 1 - Math.min(1, (float) (currentMs - mDetectionTime) / mLifeTime);
        mLastScannerAngle = newScannerAngle;
        mLastAngle = newAngle;
    }

    public float getVisibility() {
        return mVisibility;
    }

    public boolean isVisible() {
        return mVisibility > 0;
    }

    public int getDetectedAngle() {
        return mDetectedAngle;
    }

    public float getDetectedDist() {
        return mDetectedDist;
    }

}