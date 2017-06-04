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

    private Paint mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap mSonarBitmap;

    private List<SonarPoint> mPointsList = new LinkedList<>();

    protected int mScannerAngle;

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

        mArcPaint.setStyle(Paint.Style.FILL);

        mPointPaint.setColor(mColor);
        mPointPaint.setStyle(Paint.Style.FILL);
    }

    public void setPoints(Collection<SonarPoint> points) {
        mPointsList.clear();
        mPointsList.addAll(points);
    }

    public void addPoints(SonarPoint...points) {
        for (SonarPoint point : points) {
            mPointsList.add(point);
        }
    }

    public void setColor(int color) {
        mColor = color;
        mArcColor = mColor & ARC_MASK;
        mPointGradientStartColor = mColor & POINT_GRADIENT_START_MASK;
        mPointGradientEndColor = mColor & POINT_GRADIENT_END_MASK;

        mPointPaint.setColor(mColor);
        if (mSonarBitmap != null) {
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        mSonarBitmap = getSonarBitmap(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        update();

        drawSonar(canvas);

        invalidate();
    }

    protected void update() {
        updateAngle();

        mScannerAngle = (mScannerAngle + getScannerRotationAngle()) % 360;

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
            point.detect(currentMs, mCurrentAngle, mScannerAngle);
        }
    }

    protected void drawSonar(Canvas canvas) {
        float width = canvas.getWidth(), height = canvas.getHeight(),
                drawWidth = width - getPaddingLeft() - getPaddingRight(),
                drawHeight = height - getPaddingTop() - getPaddingBottom(),
                centerX = width / 2f, centerY = height / 2f,
                radius = Math.min(drawWidth, drawHeight) / 2f;

        canvas.drawBitmap(mSonarBitmap, 0, 0, null);
        drawPoints(canvas, width, height, centerX, centerY, radius);
        drawArc(canvas, width, height, centerX, centerY, radius);
    }

    private void drawPoints(Canvas canvas, float width, float height, float centerX, float centerY, float radius) {
        if (!hasSensors()) return;

        float circleBaseRadius = Math.max(1, radius / 50);

        for (SonarPoint point : mPointsList) {
            if (!point.isVisible()) continue;

            float sizeRatio = 1f + ((2.5f * (1f - point.getVisibility())));
            float circleRadius = circleBaseRadius * sizeRatio;

            PointF circleCenter = SonarUtils.getPointOnCircle(centerX, centerY,
                    (radius * 0.8f - circleBaseRadius) * point.getDetectedDist(),
                    point.getDetectedAngle());

            mPointPaint.setShader(new RadialGradient(
                    circleCenter.x, circleCenter.y, 3.5f * circleBaseRadius,
                    mPointGradientStartColor, mPointGradientEndColor, Shader.TileMode.CLAMP));
            mPointPaint.setAlpha((int) (point.getVisibility() * 255));

            canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, mPointPaint);
        }
    }

    private void drawArc(Canvas canvas, float width, float height, float centerX, float centerY, float radius){
        if (!hasSensors()) return;

        radius *= 0.8f;

        int degree = -90 + mScannerAngle;
        int offset = 360;
        int transparentArc = 0x00ffffff & mArcColor;

        Shader gradient = new SweepGradient(centerX, centerY,
                new int[] {transparentArc, transparentArc, transparentArc,
                        transparentArc, transparentArc, mArcColor}, null);

        Matrix gradientMatrix = new Matrix();
        gradientMatrix.preRotate(degree - 1, centerX, centerY);
        gradient.setLocalMatrix(gradientMatrix);

        mArcPaint.setShader(gradient);

        final RectF rect = new RectF(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);

        canvas.drawArc(rect, degree, offset, true, mArcPaint);
    }

    private Bitmap getSonarBitmap(int width, int height) {
        float centerX = width / 2f, centerY = height / 2f,
                drawWidth = width - getPaddingLeft() - getPaddingRight(),
                drawHeight = height - getPaddingTop() - getPaddingBottom(),
                radius = Math.min(drawWidth, drawHeight) / 2f;

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas circleCanvas = new Canvas(result);

        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Paint.Style.FILL);

        circleCanvas.drawCircle(centerX, centerY, radius, backgroundPaint);

        Paint innerBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerBackgroundPaint.setColor(mColor & INNER_CIRCLE_MASK);
        innerBackgroundPaint.setStyle(Paint.Style.FILL);

        circleCanvas.drawCircle(centerX, centerY, radius * 0.8f, innerBackgroundPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(mColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(radius / 150);

        Paint thinStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thinStrokePaint.setColor(mColor);
        thinStrokePaint.setStyle(Paint.Style.STROKE);
        thinStrokePaint.setStrokeWidth(radius / 300);

        circleCanvas.drawCircle(centerX, centerY, radius, strokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.8f, thinStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.64f, thinStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.48f, thinStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.32f, thinStrokePaint);
        circleCanvas.drawCircle(centerX, centerY, radius * 0.16f, thinStrokePaint);

        for (int i = 0; i < LINE_COUNT; i++) {
            int angle = i * LINE_ANGLE;
            PointF start = SonarUtils.getPointOnCircle(centerX, centerY, radius * 0.8f, angle),
                    end = SonarUtils.getPointOnCircle(centerX, centerY, 0f, angle);

            circleCanvas.drawLine(start.x, start.y, end.x, end.y,
                    (i % (LINE_COUNT / 4) == 0) ? strokePaint : thinStrokePaint);
        }

        for (int i = 0; i < SHORT_LINE_COUNT; i++) {
            int angle = i * SHORT_LINE_ANGLE;
            boolean longerLine = (i % 3 == 0);
            PointF start = SonarUtils.getPointOnCircle(centerX, centerY,
                    radius * (longerLine ? 0.83f : 0.81f), angle),
                    end = SonarUtils.getPointOnCircle(centerX, centerY,
                            radius * (longerLine ? 0.75f : 0.78f), angle);

            circleCanvas.drawLine(start.x, start.y, end.x, end.y,
                    (i % (SHORT_LINE_COUNT / 4) == 0) ? strokePaint : thinStrokePaint);
        }

        Paint fontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fontPaint.setColor(mColor);
        fontPaint.setTextAlign(Paint.Align.CENTER);
        fontPaint.setFakeBoldText(true);
        fontPaint.setTextSize(radius / 12);

        Paint smallFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smallFontPaint.setColor(mColor);
        smallFontPaint.setTextAlign(Paint.Align.CENTER);
        smallFontPaint.setTextSize(radius / 16);

        int count = (smallFontPaint.getTextSize() > 24f)
                ? 15 : ((smallFontPaint.getTextSize() > 12f) ? 30 : 45);

        Paint usedPaint;

        for (int i = 0; i < 360 / count; i++) {
            int degree = i * count;
            usedPaint = (degree % 90 == 0) ? fontPaint : smallFontPaint;
            PointF start = SonarUtils.getPointOnCircle(centerX, centerY, radius * 0.9f, degree);

            circleCanvas.drawText(Integer.toString(degree), start.x, start.y
                    - ((usedPaint.descent() + usedPaint.ascent()) / 2), usedPaint);
        }

        return result;
    }

}