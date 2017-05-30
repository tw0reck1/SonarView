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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SonarView extends RotaryView {

    private static final int DEFAULT_COLOR = 0xff03CC02,
            INNER_CIRCLE_MASK = 0x3fffffff,
            ARC_MASK = 0xbfffffff,
            POINT_GRADIENT_START_MASK = 0xffffffff,
            POINT_GRADIENT_END_MASK = 0x7fffffff;

    private static final int
            LINE_COUNT = 12,
            LINE_ANGLE = 360 / LINE_COUNT,
            SHORT_LINE_COUNT = 72,
            SHORT_LINE_ANGLE = 360 / SHORT_LINE_COUNT,
            DEFAULT_SCANNER_ROTATION_ANGLE = 4;

    private Paint
            mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mInnerBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mThinStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mSmallFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap mCircleBitmap, mLinesBitmap, mLabelsBitmap;
    protected int mScannerRotation;

    private List<SonarPoint> mPointsList = new LinkedList<>();

    protected int mColor = DEFAULT_COLOR;
    protected int mArcColor = DEFAULT_COLOR & ARC_MASK;
    protected int mPointGradientStartColor = DEFAULT_COLOR & POINT_GRADIENT_START_MASK;
    protected int mPointGradientEndColor = DEFAULT_COLOR & POINT_GRADIENT_END_MASK;

    public SonarView(Context context) {
        super(context);
        init();
    }

    public SonarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0);
        init();
    }

    public SonarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs, defStyleAttr);
        init();
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.SonarView, defStyleAttr, 0);

        mColor = array.getColor(R.styleable.SonarView_sv_color, DEFAULT_COLOR);

        array.recycle();
    }

    protected void init() {
        setRotationType(ROTATION_AZIMUTH);
        setClickable(true);

        mArcColor = mColor & ARC_MASK;
        mPointGradientStartColor = mColor & POINT_GRADIENT_START_MASK;
        mPointGradientEndColor = mColor & POINT_GRADIENT_END_MASK;

        mBackgroundPaint.setColor(Color.BLACK);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mInnerBackgroundPaint.setColor(mColor & INNER_CIRCLE_MASK);
        mInnerBackgroundPaint.setStyle(Paint.Style.FILL);

        mPointPaint.setColor(mColor);
        mPointPaint.setStyle(Paint.Style.FILL);

        mStrokePaint.setColor(mColor);
        mStrokePaint.setStyle(Paint.Style.STROKE);

        mThinStrokePaint.setColor(mColor);
        mThinStrokePaint.setStyle(Paint.Style.STROKE);

        mSmallFontPaint.setColor(mColor);
        mSmallFontPaint.setTextAlign(Paint.Align.CENTER);

        mFontPaint.setColor(mColor);
        mFontPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setPoints(Collection<SonarPoint> points) {
        mPointsList.clear();
        mPointsList.addAll(points);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float centerX = w/2f, centerY = h/2f,
                drawWidth = w - getPaddingLeft() - getPaddingRight(),
                drawHeight = h - getPaddingTop() - getPaddingBottom(),
                radius = Math.min(drawWidth, drawHeight) / 2f;

        mStrokePaint.setStrokeWidth(radius / 150);

        mThinStrokePaint.setStrokeWidth(radius / 300);

        mSmallFontPaint.setTextSize(radius / 16);

        mFontPaint.setTextSize(radius / 12);
        mFontPaint.setFakeBoldText(true);

        mArcPaint.setStyle(Paint.Style.FILL);

        mCircleBitmap = getCircleBitmap(w, h, centerX, centerY, radius);
        mLinesBitmap = getLinesBitmap(w, h, centerX, centerY, radius);
        mLabelsBitmap = getLabelsBitmap(w, h, centerX, centerY, radius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        update();

        float width = canvas.getWidth(),
                height = canvas.getHeight(),
                drawWidth = width - getPaddingLeft() - getPaddingRight(),
                drawHeight = height - getPaddingTop() - getPaddingBottom(),
                centerX = width / 2f,
                centerY = height / 2f,
                radius = Math.min(drawWidth, drawHeight) / 2f;

        drawCircle(canvas, width, height, centerX, centerY, radius);
        drawLines(canvas, width, height, centerX, centerY, radius);
        drawLabels(canvas, width, height, centerX, centerY, radius);
        drawPoints(canvas, width, height, centerX, centerY, radius);
        drawArc(canvas, width, height, centerX, centerY, radius);

        invalidate();
    }

    protected void update() {
        updateAngle();

        mScannerRotation = (mScannerRotation + getScannerRotationAngle()) % 360;

        detectPoints();
    }

    protected int getScannerRotationAngle() {
        return isPressed()
                ? 2 * DEFAULT_SCANNER_ROTATION_ANGLE
                : DEFAULT_SCANNER_ROTATION_ANGLE;
    }

    protected void detectPoints() {
        long currentMs = System.currentTimeMillis();

        for (SonarPoint point : mPointsList) {
            point.detect(currentMs, mCurrentAngle, mScannerRotation);
        }
    }

    protected void drawCircle(Canvas canvas, float width, float height, float centerX, float centerY, float radius){
        canvas.drawBitmap(mCircleBitmap, 0, 0, null);
    }

    protected void drawLines(Canvas canvas, float width, float height, float centerX, float centerY, float radius){
        canvas.drawBitmap(mLinesBitmap, 0, 0, null);
    }

    protected void drawLabels(Canvas canvas, float width, float height, float centerX, float centerY, float radius){
        canvas.drawBitmap(mLabelsBitmap, 0, 0, null);
    }

    protected void drawPoints(Canvas canvas, float width, float height, float centerX, float centerY, float radius){
        if (!hasSensors()) return;

        float circleBaseRadius = Math.max(1, radius / 50);

        for (SonarPoint point : mPointsList) {
            if (!point.isVisible()) continue;

            float sizeRatio = 1f + ((2.5f * (1f - point.getVisibility())));
            float circleRadius = circleBaseRadius * sizeRatio;

            PointF circleCenter = SonarUtils.getPointOnCircle(centerX, centerY,
                    (radius * 0.8f - circleBaseRadius) * point.getDetectedDist(),
                    point.getDetectedDegree());

            mPointPaint.setShader(new RadialGradient(
                    circleCenter.x, circleCenter.y, 3.5f * circleBaseRadius,
                    mPointGradientStartColor, mPointGradientEndColor, Shader.TileMode.CLAMP));
            mPointPaint.setAlpha((int)(point.getVisibility()*255));

            canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, mPointPaint);
        }
    }

    protected void drawArc(Canvas canvas, float width, float height, float centerX, float centerY, float radius){
        if (!hasSensors()) return;

        radius *= 0.8f;

        int degree = -90 + mScannerRotation;
        int offset = 360;
        int transparentArc = 0x00ffffff & mArcColor;

        Shader gradient = new SweepGradient(centerX, centerY,
                new int[]{transparentArc, transparentArc, transparentArc,
                        transparentArc, transparentArc, mArcColor}, null);

        Matrix gradientMatrix = new Matrix();
        gradientMatrix.preRotate(degree-1, centerX, centerY);
        gradient.setLocalMatrix(gradientMatrix);

        mArcPaint.setShader(gradient);

        final RectF rect = new RectF(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);

        canvas.drawArc(rect, degree, offset, true, mArcPaint);
    }

    private Bitmap getCircleBitmap(int width, int height, float centerX, float centerY, float radius){
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas circleCanvas = new Canvas(result);

        circleCanvas.drawCircle(centerX, centerY, radius, mBackgroundPaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.8f, mInnerBackgroundPaint);

        circleCanvas.drawCircle(centerX, centerY, radius, mStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.8f, mThinStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.65f, mThinStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.5f, mThinStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.35f, mThinStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.2f, mThinStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.05f, mThinStrokePaint);

        return result;
    }

    private Bitmap getLinesBitmap(int width, int height, float centerX, float centerY, float radius){
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas linesCanvas = new Canvas(result);

        for(int i=0; i<LINE_COUNT; i++){
            int angle = i * LINE_ANGLE;
            PointF start = SonarUtils.getPointOnCircle(centerX, centerY, radius * 0.8f, angle),
                    end = SonarUtils.getPointOnCircle(centerX, centerY, 0f, angle);
            linesCanvas.drawLine(start.x, start.y, end.x, end.y,
                    i%(LINE_COUNT/4)==0 ? mStrokePaint : mThinStrokePaint);
        }

        for(int i=0; i<SHORT_LINE_COUNT; i++){
            int angle = i * SHORT_LINE_ANGLE;
            boolean longerLine = (i % 3 == 0);
            PointF start = SonarUtils.getPointOnCircle(centerX, centerY,
                    radius * (longerLine ? 0.83f : 0.81f), angle),
                    end = SonarUtils.getPointOnCircle(centerX, centerY,
                            radius * (longerLine ? 0.75f : 0.78f), angle);
            linesCanvas.drawLine(start.x, start.y, end.x, end.y,
                    i%(SHORT_LINE_COUNT/4)==0 ? mStrokePaint : mThinStrokePaint);
        }

        return result;
    }

    private Bitmap getLabelsBitmap(int width, int height, float centerX, float centerY, float radius) {
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas labelsCanvas = new Canvas(result);

        int count = (mSmallFontPaint.getTextSize() > 24f)
                ? 15 : ((mSmallFontPaint.getTextSize() > 12f) ? 30 : 45);

        Paint usedPaint;

        for (int i = 0; i < 360 / count; i++) {
            int degree = i * count;
            usedPaint = (degree % 90 == 0) ? mFontPaint : mSmallFontPaint;
            PointF start = SonarUtils.getPointOnCircle(centerX, centerY, radius * 0.9f, degree);
            labelsCanvas.drawText(Integer.toString(degree), start.x, start.y
                    - ((usedPaint.descent() + usedPaint.ascent()) / 2), usedPaint);
        }

        return result;
    }

}