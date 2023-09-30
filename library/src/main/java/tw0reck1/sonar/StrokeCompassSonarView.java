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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class StrokeCompassSonarView extends RotaryView implements Sonar {

    private static final String[] DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private static final int DEFAULT_COLOR = 0xff03CC02,
            ARC_MASK = 0xffffffff,
            TEXT_MASK = 0xbfffffff,
            STROKE_MASK = 0x9fffffff;

    public static final float
            DEFAULT_FONT_SIZE = 28f,
            DEFAULT_THIN_FONT_SIZE = 21f,
            DEFAULT_STROKE_WIDTH = 2.5f,
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

    private Interpolator mPointsInterpolator = new DecelerateInterpolator(0.6f);

    private Bitmap mSonarBitmap;

    private List<SonarPoint> mPointsList = new LinkedList<>();

    protected int mScannerAngle;

    protected float mFontSize = DEFAULT_FONT_SIZE;
    protected float mThinFontSize = DEFAULT_THIN_FONT_SIZE;
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
                R.styleable.TextSonarView, defStyleAttr, 0);

        mFontSize = array.getDimension(R.styleable.TextSonarView_sv_fontSize,
                SonarUtils.spToPx(getResources(), DEFAULT_FONT_SIZE));
        mThinFontSize = array.getDimension(R.styleable.TextSonarView_sv_thinFontSize,
                SonarUtils.spToPx(getResources(), DEFAULT_THIN_FONT_SIZE));

        array.recycle();
        array = context.getTheme().obtainStyledAttributes(attrs,
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
        mArcPaint.setStrokeWidth(mStrokeWidth);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);

        mPointPaint.setColor(mColor);
        mPointPaint.setStyle(Paint.Style.FILL);

        mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFontPaint.setColor(mColor);
        mFontPaint.setTextAlign(Paint.Align.CENTER);
        mFontPaint.setFakeBoldText(true);
        mFontPaint.setTextSize(mFontSize);

        mSmallFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSmallFontPaint.setColor(mColor & TEXT_MASK);
        mSmallFontPaint.setTextAlign(Paint.Align.CENTER);
        mSmallFontPaint.setTextSize(mThinFontSize);

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
        mSmallFontPaint.setColor(mColor & TEXT_MASK);
        mArcPaint.setColor(mArcColor);
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        invalidate();
    }

    @Override
    public void setStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
        mArcPaint.setStrokeWidth(strokeWidth);
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new CompassOutline(getWidth(), getHeight()));
        }
        invalidate();
    }

    @Override
    public void setThinStrokeWidth(float thinStrokeWidth) {
        mThinStrokeWidth = thinStrokeWidth;
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        invalidate();
    }

    @Override
    public void setFontSize(float fontSize) {
        mFontSize = fontSize;
        mFontPaint.setTextSize(fontSize);
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new CompassOutline(getWidth(), getHeight()));
        }
        invalidate();
    }

    @Override
    public void setThinFontSize(float thinFontSize) {
        mThinFontSize = thinFontSize;
        mSmallFontPaint.setTextSize(thinFontSize);
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new CompassOutline(getWidth(), getHeight()));
        }
        invalidate();
    }

    @Override
    public void setPointSize(float pointSize) {
        mPointSize = pointSize;
    }

    @Override
    public void setSizes(float strokeWidth, float thinStrokeWidth, float fontSize, float thinFontSize, float pointSize) {
        mStrokeWidth = strokeWidth;
        mArcPaint.setStrokeWidth(strokeWidth);
        mThinStrokeWidth = thinStrokeWidth;
        mFontSize = fontSize;
        mFontPaint.setTextSize(fontSize);
        mThinFontSize = thinFontSize;
        mSmallFontPaint.setTextSize(thinFontSize);
        mPointSize = pointSize;
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new CompassOutline(getWidth(), getHeight()));
        }
        invalidate();
    }

    @Override
    public void startAnimation() {
        if (!mAnimator.isRunning()) {
            mAnimator.start();
        }
    }

    @Override
    public void stopAnimation() {
        mAnimator.cancel();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
        }
        mSonarBitmap = getSonarBitmap(width, height);

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
        float centerX = getPaddingLeft() + radius;
        float centerY = getPaddingTop() + radius;

        drawDirections(canvas, centerX, centerY, radius);

        canvas.save();
        canvas.rotate(mCurrentAngle - getScreenRotation(), centerX, centerY);

        canvas.drawBitmap(mSonarBitmap, getPaddingLeft(), getPaddingTop(), null);

        if (mAnimator.isRunning()) {
            drawPoints(canvas, centerX, centerY, radius);
            drawArc(canvas, centerX, centerY, radius);
        }

        canvas.restore();
    }

    private void drawDirections(Canvas canvas, float centerX, float centerY, float radius) {
        int screenRotation = getScreenRotation();
        float offset = Math.max(mFontSize, mThinFontSize) / 2f;
        Paint usedPaint;
        boolean bigLetter;
        for (int i = 0; i < DIRECTIONS.length; i++) {
            bigLetter = (i % 2 == 0);
            usedPaint = bigLetter ? mFontPaint : mSmallFontPaint;
            PointF start = SonarUtils.getPointOnCircle(centerX, centerY, radius - offset,
                    mCurrentAngle - screenRotation + i * DIRECTION_ANGLE);

            canvas.drawText(DIRECTIONS[i], start.x, start.y
                    - ((usedPaint.descent() + usedPaint.ascent()) / 2), usedPaint);
        }
    }

    private void drawPoints(Canvas canvas, float centerX, float centerY, float radius) {
        if (!hasSensors()) return;

        float circleBaseRadius = mPointSize / 2f;

        float textSize = Math.max(mFontSize, mThinFontSize);
        radius -= textSize + mStrokeWidth / 2f;

        for (SonarPoint point : mPointsList) {
            if (!point.isVisible()) continue;

            float visibility = mPointsInterpolator.getInterpolation(point.getVisibility());
            float sizeRatio = 0.75f + ((0.5f * (1f - visibility)));
            float circleRadius = circleBaseRadius * sizeRatio;

            PointF circleCenter = SonarUtils.getPointOnCircle(centerX, centerY,
                    (radius * 0.9f - circleBaseRadius) * point.getDetectedDist(),
                    point.getAngle());

            mPointPaint.setAlpha((int) (visibility * 255));

            canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, mPointPaint);
        }
    }

    private void drawArc(Canvas canvas, float centerX, float centerY, float radius) {
        if (!hasSensors()) return;

        float textSize = Math.max(mFontSize, mThinFontSize);
        float lineLength = 0.9f * (radius - textSize - mStrokeWidth / 2f);

        PointF arcPoint = SonarUtils.getPointOnCircle(centerX, centerY,
                lineLength, mScannerAngle);

        canvas.drawLine(centerX, centerY, arcPoint.x, arcPoint.y, mArcPaint);
    }

    private Bitmap getSonarBitmap(int width, int height) {
        float drawWidth = width - getPaddingLeft() - getPaddingRight(),
                drawHeight = height - getPaddingTop() - getPaddingBottom(),
                textSize = Math.max(mFontSize, mThinFontSize),
                diameter = Math.min(drawWidth, drawHeight),
                radius = diameter / 2f - textSize - mStrokeWidth / 2f,
                center = diameter / 2f;

        int bitmapSize = Math.round(diameter);

        if (bitmapSize <= 0) return null;

        Bitmap result = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
        Canvas circleCanvas = new Canvas(result);

        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Paint.Style.FILL);

        circleCanvas.drawCircle(center, center, radius * 0.9f, backgroundPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(mColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(mStrokeWidth);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        Paint thinStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thinStrokePaint.setColor(mColor & STROKE_MASK);
        thinStrokePaint.setStyle(Paint.Style.STROKE);
        thinStrokePaint.setStrokeWidth(mThinStrokeWidth);
        thinStrokePaint.setStrokeCap(Paint.Cap.ROUND);

        circleCanvas.drawCircle(center, center, radius * 0.9f, strokePaint);
        circleCanvas.drawCircle(center, center, radius * 0.6f, thinStrokePaint);
        circleCanvas.drawCircle(center, center, radius * 0.3f, thinStrokePaint);

        for (int i = 0; i < LINE_COUNT; i++) {
            int angle = i * LINE_ANGLE;
            boolean directionalLine = (i % (LINE_COUNT / 4) == 0);
            float centerOffset = directionalLine ? 0.08f : 0.12f;
            PointF start = SonarUtils.getPointOnCircle(center, center, radius * 0.9f, angle),
                    end = SonarUtils.getPointOnCircle(center, center, radius * centerOffset, angle);

            circleCanvas.drawLine(start.x, start.y, end.x, end.y,
                    directionalLine ? strokePaint : thinStrokePaint);
        }

        thinStrokePaint.setColor(mColor);
        float maxLineRadius = radius - mStrokeWidth / 4f;
        for (int i = 0; i < SHORT_LINE_COUNT; i++) {
            int angle = i * SHORT_LINE_ANGLE;
            boolean longerLine = (i % 3 == 0);
            PointF start = SonarUtils.getPointOnCircle(center, center,
                    longerLine ? maxLineRadius : maxLineRadius * 0.96f, angle),
                    end = SonarUtils.getPointOnCircle(center, center,
                            radius * 0.9f, angle);

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
            int paddingLeft = getPaddingLeft(),
                    paddingTop = getPaddingTop();
            float drawWidth = width - paddingLeft - getPaddingRight(),
                    drawHeight = height - paddingTop - getPaddingBottom(),
                    diameter = Math.min(drawWidth, drawHeight),
                    textSize = Math.max(mFontSize, mThinFontSize),
                    radius = diameter / 2f - textSize - mStrokeWidth / 2f;

            int centerX = Math.round(paddingLeft + drawWidth / 2f);
            int centerY = Math.round(paddingTop + drawHeight / 2f);

            Rect bounds = new Rect(centerX, centerY, centerX, centerY);

            int inset = Math.round(-0.9f * radius);
            bounds.inset(inset, inset);

            outline.setOval(bounds);
        }

    }

}