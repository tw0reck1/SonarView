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

public class StrokeSonarView extends RotaryView implements Sonar {

    private static final int DEFAULT_COLOR = 0xff03CC02,
            INNER_CIRCLE_MASK = 0x3fffffff,
            ARC_MASK = 0xffffffff;

    private static final boolean DEFAULT_OUTER_BORDER = true;

    private static final int
            LINE_COUNT = 12,
            LINE_ANGLE = 360 / LINE_COUNT,
            SHORT_LINE_COUNT = 72,
            SHORT_LINE_ANGLE = 360 / SHORT_LINE_COUNT;

    private static final int MIN_LOOP_DURATION = 250;
    private static final int DEFAULT_LOOP_DURATION = 1250;
    private static final int MAX_LOOP_DURATION = Integer.MAX_VALUE;

    private Paint mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap mSonarBitmap;

    private List<SonarPoint> mPointsList = new LinkedList<>();

    protected int mScannerAngle;

    protected int mColor = DEFAULT_COLOR;
    protected int mArcColor = DEFAULT_COLOR & ARC_MASK;

    protected boolean mOuterBorder;

    protected int mLoopDuration = DEFAULT_LOOP_DURATION;

    private ValueAnimator mAnimator;

    public StrokeSonarView(Context context) {
        super(context);
        init();
    }

    public StrokeSonarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0);
        init();
    }

    public StrokeSonarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs, defStyleAttr);
        init();
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.SonarView, defStyleAttr, 0);

        mColor = array.getColor(R.styleable.SonarView_sv_color, DEFAULT_COLOR);
        mOuterBorder = array.getBoolean(R.styleable.SonarView_sv_outerBorder, DEFAULT_OUTER_BORDER);
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
        mPointPaint.setStyle(Paint.Style.STROKE);
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
        mArcPaint.setColor(mArcColor);
        if (mSonarBitmap != null) {
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        mSonarBitmap = getSonarBitmap(width, height);

        if (mSonarBitmap != null) {
            float radius = mSonarBitmap.getWidth() / 2f;

            mArcPaint.setStrokeWidth(radius / 40);
            mPointPaint.setStrokeWidth(radius / 60);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new SonarOutline(width, height));
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        startAnimation();
    }

    private void startAnimation() {
        mAnimator = ValueAnimator.ofInt(0, 360);
        mAnimator.setDuration(mLoopDuration);
        mAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator anim) {
                mScannerAngle = (int) anim.getAnimatedValue();

                updateAngle();
                detectPoints();

                invalidate();
            }
        });
        mAnimator.start();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (mAnimator == null) return;

        if (getVisibility() == VISIBLE) {
            mAnimator.start();
        } else {
            mAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawSonar(canvas);
    }

    protected void detectPoints() {
        long currentMs = System.currentTimeMillis();

        for (SonarPoint point : mPointsList) {
            point.detect(currentMs, mCurrentAngle, mScannerAngle);
        }
    }

    protected void drawSonar(Canvas canvas) {
        if (mSonarBitmap == null) return;

        float radius = mSonarBitmap.getWidth() / 2f;

        canvas.drawBitmap(mSonarBitmap, getPaddingLeft(), getPaddingTop(), null);

        canvas.save();
        canvas.rotate(-getScreenRotation(), getPaddingLeft() + radius, getPaddingTop() + radius);

        drawPoints(canvas, radius, radius, radius);
        drawArc(canvas, radius, radius, radius);

        canvas.restore();
    }

    private void drawPoints(Canvas canvas, float centerX, float centerY, float radius) {
        if (!hasSensors()) return;

        float circleBaseRadius = Math.max(1, radius / 50);

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        for (SonarPoint point : mPointsList) {
            if (!point.isVisible()) continue;

            float sizeRatio = 0.75f + ((2.25f * (1f - point.getVisibility())));
            float circleRadius = circleBaseRadius * sizeRatio;

            PointF circleCenter = SonarUtils.getPointOnCircle(centerX, centerY,
                    (radius * 0.8f - circleBaseRadius) * point.getDetectedDist(),
                    point.getDetectedAngle());

            mPointPaint.setAlpha((int) (point.getVisibility() * 255));

            canvas.drawCircle(paddingLeft + circleCenter.x, paddingTop + circleCenter.y,
                    circleRadius, mPointPaint);
        }
    }

    private void drawArc(Canvas canvas, float centerX, float centerY, float radius) {
        if (!hasSensors()) return;

        PointF arcPoint = SonarUtils.getPointOnCircle(centerX, centerY,
                (radius * 0.8f), mScannerAngle);

        canvas.drawLine(centerX, centerY, arcPoint.x, arcPoint.y, mArcPaint);
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
        backgroundPaint.setColor(mColor & INNER_CIRCLE_MASK);
        backgroundPaint.setStyle(Paint.Style.FILL);

        circleCanvas.drawCircle(center, center, radius, backgroundPaint);

        Paint innerBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerBackgroundPaint.setColor(Color.BLACK);
        innerBackgroundPaint.setStyle(Paint.Style.FILL);

        circleCanvas.drawCircle(center, center, radius * 0.8f, innerBackgroundPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(mColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(radius / 100);

        Paint thinStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thinStrokePaint.setColor(mColor);
        thinStrokePaint.setStyle(Paint.Style.STROKE);
        thinStrokePaint.setStrokeWidth(radius / 200);

        if (mOuterBorder) {
            circleCanvas.drawCircle(center, center, radius - strokePaint.getStrokeWidth() / 2f, strokePaint);
        }
        circleCanvas.drawCircle(center, center, radius * 0.8f, thinStrokePaint);
        circleCanvas.drawCircle(center, center, radius * 0.64f, thinStrokePaint);
        circleCanvas.drawCircle(center, center, radius * 0.48f, thinStrokePaint);
        circleCanvas.drawCircle(center, center, radius * 0.32f, thinStrokePaint);
        circleCanvas.drawCircle(center, center, radius * 0.16f, thinStrokePaint);

        for (int i = 0; i < LINE_COUNT; i++) {
            int angle = i * LINE_ANGLE;
            PointF start = SonarUtils.getPointOnCircle(center, center, radius * 0.8f, angle),
                    end = SonarUtils.getPointOnCircle(center, center, 0f, angle);

            circleCanvas.drawLine(start.x, start.y, end.x, end.y,
                    (i % (LINE_COUNT / 4) == 0) ? strokePaint : thinStrokePaint);
        }

        for (int i = 0; i < SHORT_LINE_COUNT; i++) {
            int angle = i * SHORT_LINE_ANGLE;
            boolean longerLine = (i % 3 == 0);
            PointF start = SonarUtils.getPointOnCircle(center, center,
                    radius * (longerLine ? 0.83f : 0.81f), angle),
                    end = SonarUtils.getPointOnCircle(center, center,
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
            PointF start = SonarUtils.getPointOnCircle(center, center, radius * 0.9f, degree);

            circleCanvas.drawText(Integer.toString(degree), start.x, start.y
                    - ((usedPaint.descent() + usedPaint.ascent()) / 2), usedPaint);
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
    private class SonarOutline extends ViewOutlineProvider {

        private final int width;
        private final int height;

        private SonarOutline(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            Rect bounds = new Rect(view.getPaddingLeft(), view.getPaddingTop(),
                    width - getPaddingRight(), height - getPaddingBottom());

            float diameter = Math.min(bounds.width(), bounds.height());
            float radius = diameter / 2f;

            int inset = Math.round(0.2f * radius);
            bounds.inset(inset, inset);

            outline.setOval(bounds);
        }

    }

}