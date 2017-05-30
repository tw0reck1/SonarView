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

    private static final long LIFETIME = 1250L;

    private long mDetectionTime;

    private int mDetectedDegree, mLastDegree, mLastScannerDegree;
    private float mDetectedDist, mVisibility;

    private int mDegree;
    private float mDist;

    public SonarPoint(int degree, float dist) {
        this.mDegree = degree;
        this.mDist = dist;
    }

    protected void setData(int degree, float dist) {
        mDegree = degree;
        mDist = dist;
    }

    void detect(long currentMs, int currentAngle, int newScannerDegree) {
        int newDegree = mDegree + currentAngle,
                lastDiff = SonarUtils.getAngleDiff(
                        SonarUtils.getOffsetAngle(mLastScannerDegree),
                        SonarUtils.getOffsetAngle(mLastDegree)),
                newDiff = SonarUtils.getAngleDiff(
                        SonarUtils.getOffsetAngle(newScannerDegree),
                        SonarUtils.getOffsetAngle(newDegree));

        if (lastDiff > 0 && newDiff <= 0) {
            mDetectionTime = currentMs;
            mDetectedDegree = mDegree + currentAngle;
            mDetectedDist = mDist;
        }

        mVisibility = 1 - Math.min(1, (float) (currentMs - mDetectionTime) / LIFETIME);
        mLastScannerDegree = newScannerDegree;
        mLastDegree = newDegree;
    }

    public float getVisibility() {
        return mVisibility;
    }

    public boolean isVisible() {
        return mVisibility > 0;
    }

    public int getDetectedDegree() {
        return mDetectedDegree;
    }

    public float getDetectedDist() {
        return mDetectedDist;
    }

}