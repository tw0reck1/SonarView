/*
 * Copyright 2020 Adrian Tworkowski
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

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class StrokeCompassSonarView extends RotaryView implements Sonar {

    private static final String[] DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private static final int DEFAULT_COLOR = 0xff03CC02,
            ARC_MASK = 0xffffffff;

    public static final float DEFAULT_STROKE_WIDTH = 2.5f,
            DEFAULT_THIN_STROKE_WIDTH = 1.25f,
            DEFAULT_POINT_SIZE = 8f;

    private static final int
            LINE_COUNT = 24,
            LINE_ANGLE = 360 / LINE_COUNT,
            SHORT_LINE_COUNT = 72,
            SHORT_LINE_ANGLE = 360 / SHORT_LINE_COUNT,
            DIRECTION_ANGLE = 360 / DIRECTIONS.length;

    private static final int MIN_LOOP_DURATION = 250;
    private static final int DEFAULT_LOOP_DURATION = 1250;
    private static final int MAX_LOOP_DURATION = Integer.MAX_VALUE;

    private Paint mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mSmallFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap mSonarBitmap;

    private List<SonarPoint> mPointsList = new LinkedList<>();

    protected int mScannerAngle;

    protected float mStrokeWidth = DEFAULT_STROKE_WIDTH;
    protected float mThinStrokeWidth = DEFAULT_THIN_STROKE_WIDTH;
    protected float mPointSize = DEFAULT_POINT_SIZE;

    protected int mColor = DEFAULT_COLOR;
    protected int mArcColor = DEFAULT_COLOR & ARC_MASK;

    protected int mLoopDuration = DEFAULT_LOOP_DURATION;

    private ValueAnimator mAnimator;

    public StrokeCompassSonarView(Context context) {
        super(context);
        init();
    }

    public StrokeCompassSonarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0);
        init();
    }

    public StrokeCompassSonarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs, defStyleAttr);
        init();
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.SonarView, defStyleAttr, 0);

        mStrokeWidth = array.getDimension(R.styleable.SonarView_sv_strokeWidth,
                SonarUtils.dpToPx(getResources(), DEFAULT_STROKE_WIDTH));
        mThinStrokeWidth = array.getDimension(R.styleable.SonarView_sv_thinStrokeWidth,
                SonarUtils.dpToPx(getResources(), DEFAULT_THIN_STROKE_WIDTH));
        mPointSize = array.getDimension(R.styleable.SonarView_sv_pointSize,
                SonarUtils.dpToPx(getResources(), DEFAULT_POINT_SIZE));
        mColor = array.getColor(R.styleable.SonarView_sv_color, DEFAULT_COLOR);
        mLoopDuration = SonarUtils.clamp(array.getInt(R.styleable.SonarView_sv_loopDuration,
                DEFAULT_LOOP_DURATION), MIN_LOOP_DURATION, MAX_LOOP_DURATION);

        array.recycle();
    }

    protected void init() {
        setRotationType(ROTATION_AZIMUTH);
        setClickable(true);

        mArcColor = mColor & ARC_MASK;
        mArcPaint.setColor(mArcColor);
        mArcPaint.setStyle(Paint.Style.STROKE);

        mPointPaint.setColor(mColor);
        mPointPaint.setStyle(Paint.Style.FILL);

        mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFontPaint.setColor(mColor);
        mFontPaint.setTextAlign(Paint.Align.CENTER);
        mFontPaint.setFakeBoldText(true);

        mSmallFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSmallFontPaint.setColor(mColor);
        mSmallFontPaint.setTextAlign(Paint.Align.CENTER);

        mAnimator = ValueAnimator.ofInt(0, 360);
        mAnimator.setDuration(mLoopDuration);
        mAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(anim -> {
            mScannerAngle = (int) anim.getAnimatedValue();

            updateAngle();
            detectPoints();

            invalidate();
        });
    }

    @Override
    public void setPoints(Collection<SonarPoint> points) {
        mPointsList.clear();
        mPointsList.addAll(points);
    }

    @Override
    public void addPoints(SonarPoint...points) {
        for (SonarPoint point : points) {
            mPointsList.add(point);
        }
    }

    @Override
    public void setColor(int color) {
        mColor = color;
        mArcColor = mColor & ARC_MASK;

        mPointPaint.setColor(mColor);
        mFontPaint.setColor(mColor);
        mSmallFontPaint.setColor(mColor);
        mArcPaint.setColor(mArcColor);
        if (mSonarBitmap != null) {
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        invalidate();
    }

    @Override
    public void setStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
        if (mSonarBitmap != null) {
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        invalidate();
    }

    @Override
    public void setThinStrokeWidth(float thinStrokeWidth) {
        mThinStrokeWidth = thinStrokeWidth;
        if (mSonarBitmap != null) {
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        invalidate();
    }

    @Override
    public void setPointSize(float pointSize) {
        mPointSize = pointSize;
    }

    @Override
    public void setSizes(float strokeWidth, float thinStrokeWidth, float pointSize) {
        mStrokeWidth = strokeWidth;
        mThinStrokeWidth = thinStrokeWidth;
        mPointSize = pointSize;
        if (mSonarBitmap != null) {
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        invalidate();
    }

    @Override
    public void startAnimation() {
        mAnimator.start();
    }

    @Override
    public void stopAnimation() {
        mAnimator.cancel();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        mSonarBitmap = getSonarBitmap(width, height);

        if (mSonarBitmap != null) {
            float radius = mSonarBitmap.getWidth() / 2f;

            mFontPaint.setTextSize(radius / 8);
            mSmallFontPaint.setTextSize(radius / 12);
            mArcPaint.setStrokeWidth(radius / 40);
            mPointPaint.setStrokeWidth(radius / 50);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new CompassOutline(width, height));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawSonar(canvas);
    }

    protected void detectPoints() {
        long currentMs = System.currentTimeMillis();

        for (SonarPoint point : mPointsList) {
            point.detect(currentMs, 0, mScannerAngle);
        }
    }

    protected void drawSonar(Canvas canvas) {
        if (mSonarBitmap == null) return;

        float radius = mSonarBitmap.getWidth() / 2f;

        drawDirections(canvas, radius, radius, radius);

        canvas.save();
        canvas.rotate(mCurrentAngle - getScreenRotation(), getPaddingLeft() + radius, getPaddingTop() + radius);

        canvas.drawBitmap(mSonarBitmap, getPaddingLeft(), getPaddingTop(), null);

        if (mAnimator.isRunning()) {
            drawPoints(canvas, radius, radius, radius);
            drawArc(canvas, radius, radius, radius);
        }

        canvas.restore();
    }

    private void drawDirections(Canvas canvas, float centerX, float centerY, float radius) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        int screenRotation = getScreenRotation();
        Paint usedPaint;
        for (int i = 0; i < DIRECTIONS.length; i++) {
            usedPaint = (i % 2 == 0) ? mFontPaint : mSmallFontPaint;
            PointF start = SonarUtils.getPointOnCircle(paddingLeft + centerX,
                    paddingTop + centerY, (i % 2 == 0) ? radius * 0.89f : radius * 0.875f,
                    mCurrentAngle - screenRotation + i * DIRECTION_ANGLE);

            canvas.drawText(DIRECTIONS[i], start.x, start.y
                    - ((usedPaint.descent() + usedPaint.ascent()) / 2), usedPaint);
        }
    }

    private void drawPoints(Canvas canvas, float centerX, float centerY, float radius) {
        if (!hasSensors()) return;

        float circleBaseRadius = mPointSize / 2f;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        for (SonarPoint point : mPointsList) {
            if (!point.isVisible()) continue;

            float sizeRatio = 1f + ((2.5f * (1f - point.getVisibility())));
            float circleRadius = circleBaseRadius * sizeRatio;

            PointF circleCenter = SonarUtils.getPointOnCircle(centerX, centerY,
                    (radius * 0.75f - circleBaseRadius) * point.getDetectedDist(),
                    point.getAngle());

            mPointPaint.setAlpha((int) (point.getVisibility() * 255));

            canvas.drawCircle(paddingLeft + circleCenter.x, paddingTop + circleCenter.y,
                    circleRadius, mPointPaint);
        }
    }

    private void drawArc(Canvas canvas, float centerX, float centerY, float radius) {
        if (!hasSensors()) return;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        PointF arcPoint = SonarUtils.getPointOnCircle(paddingLeft + centerX, paddingTop + centerY,
                (radius * 0.75f), mScannerAngle);

        canvas.drawLine(paddingLeft + centerX, paddingTop + centerY,
                arcPoint.x, arcPoint.y, mArcPaint);
    }

    private Bitmap getSonarBitmap(int width, int height) {
        float drawWidth = width - getPaddingLeft() - getPaddingRight(),
                drawHeight = height - getPaddingTop() - getPaddingBottom(),
                diameter = Math.min(drawWidth, drawHeight),
                radius = diameter / 2f,
                center = radius;

        int bitmapSize = Math.round(diameter);

        if (bitmapSize <= 0) return null;

        Bitmap result = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
        Canvas circleCanvas = new Canvas(result);

        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Paint.Style.FILL);

        circleCanvas.drawCircle(center, center, radius * 0.75f, backgroundPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(mColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(mStrokeWidth);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        Paint thinStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thinStrokePaint.setColor(mColor);
        thinStrokePaint.setStyle(Paint.Style.STROKE);
        thinStrokePaint.setStrokeWidth(mThinStrokeWidth);
        thinStrokePaint.setStrokeCap(Paint.Cap.ROUND);

        circleCanvas.drawCircle(center, center, radius * 0.75f, strokePaint);
        circleCanvas.drawCircle(center, center, radius * 0.5f, thinStrokePaint);
        circleCanvas.drawCircle(center, center, radius * 0.25f, thinStrokePaint);

        for (int i = 0; i < LINE_COUNT; i++) {
            int angle = i * LINE_ANGLE;
            boolean directionalLine = (i % (LINE_COUNT / 4) == 0);
            float centerOffset = directionalLine ? 0.05f : 0.08f;
            PointF start = SonarUtils.getPointOnCircle(center, center, radius * 0.8f, angle),
                    end = SonarUtils.getPointOnCircle(center, center, radius * centerOffset, angle);

            circleCanvas.drawLine(start.x, start.y, end.x, end.y,
                    directionalLine ? strokePaint : thinStrokePaint);
        }

        for (int i = 0; i < SHORT_LINE_COUNT; i++) {
            int angle = i * SHORT_LINE_ANGLE;
            boolean longerLine = (i % 3 == 0);
            PointF start = SonarUtils.getPointOnCircle(center, center,
                    radius * (longerLine ? 0.81f : 0.78f), angle),
                    end = SonarUtils.getPointOnCircle(center, center,
                            radius * 0.75f, angle);

            circleCanvas.drawLine(start.x, start.y, end.x, end.y,
                    (i % (SHORT_LINE_COUNT / 4) == 0) ? strokePaint : thinStrokePaint);
        }

        return result;
    }

    private int getScreenRotation() {
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        switch (display.getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_0:
            default:
                return 0;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class CompassOutline extends ViewOutlineProvider {

        private final int width;
        private final int height;

        private CompassOutline(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            Rect bounds = new Rect(view.getPaddingLeft(), view.getPaddingTop(),
                    width - getPaddingRight(), height - getPaddingBottom());

            float diameter = Math.min(bounds.width(), bounds.height());
            float radius = diameter / 2f;

            int inset = Math.round(0.25f * radius);
            bounds.inset(inset, inset);

            outline.setOval(bounds);
        }

    }

}