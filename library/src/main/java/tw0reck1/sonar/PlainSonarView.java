/*
 * Copyright 2019 Adrian Tworkowski
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
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
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

public class PlainSonarView extends RotaryView implements Sonar {

    private static final int DEFAULT_COLOR = 0xff03CC02,
            INNER_CIRCLE_MASK = 0x3fffffff,
            ARC_MASK = 0xbfffffff,
            POINT_MASK = 0xffffffff;

    public static final float DEFAULT_STROKE_WIDTH = 3.75f,
            DEFAULT_THIN_STROKE_WIDTH = 2f,
            DEFAULT_POINT_SIZE = 20f;

    private static final boolean DEFAULT_OUTER_BORDER = true;

    private static final int
            LINE_COUNT = 8,
            LINE_ANGLE = 360 / LINE_COUNT;

    private static final int MIN_LOOP_DURATION = 250;
    private static final int DEFAULT_LOOP_DURATION = 1250;
    private static final int MAX_LOOP_DURATION = Integer.MAX_VALUE;

    private Paint mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap mSonarBitmap;

    private List<SonarPoint> mPointsList = new LinkedList<>();

    protected int mScannerAngle;

    protected float mStrokeWidth = DEFAULT_STROKE_WIDTH;
    protected float mThinStrokeWidth = DEFAULT_THIN_STROKE_WIDTH;
    protected float mPointSize = DEFAULT_POINT_SIZE;

    protected int mColor = DEFAULT_COLOR;
    protected int mArcColor = DEFAULT_COLOR & ARC_MASK;
    protected int mPointColor = DEFAULT_COLOR & POINT_MASK;

    protected boolean mOuterBorder;

    protected int mLoopDuration = DEFAULT_LOOP_DURATION;

    private ValueAnimator mAnimator;

    public PlainSonarView(Context context) {
        super(context);
        init();
    }

    public PlainSonarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0);
        init();
    }

    public PlainSonarView(Context context, AttributeSet attrs, int defStyleAttr) {
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
        mOuterBorder = array.getBoolean(R.styleable.SonarView_sv_outerBorder, DEFAULT_OUTER_BORDER);
        mLoopDuration = SonarUtils.clamp(array.getInt(R.styleable.SonarView_sv_loopDuration,
                DEFAULT_LOOP_DURATION), MIN_LOOP_DURATION, MAX_LOOP_DURATION);

        array.recycle();
    }

    protected void init() {
        setRotationType(ROTATION_AZIMUTH);
        setClickable(true);

        mArcColor = mColor & ARC_MASK;
        mPointColor = mColor & POINT_MASK;

        mArcPaint.setStyle(Paint.Style.FILL);

        mPointPaint.setColor(mColor);
        mPointPaint.setStyle(Paint.Style.FILL);

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
        mPointColor = mColor & POINT_MASK;

        mPointPaint.setColor(mColor);
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
        }
        invalidate();
    }

    @Override
    public void setStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
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
        // no-op
    }

    @Override
    public void setThinFontSize(float thinFontSize) {
        // no-op
    }

    @Override
    public void setPointSize(float pointSize) {
        mPointSize = pointSize;
    }

    @Override
    public void setSizes(float strokeWidth, float thinStrokeWidth, float fontSize, float thinFontSize, float pointSize) {
        mStrokeWidth = strokeWidth;
        mThinStrokeWidth = thinStrokeWidth;
        mPointSize = pointSize;
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
            mSonarBitmap = getSonarBitmap(getWidth(), getHeight());
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
    protected void onSizeChanged(int width, int height, int oldwidth, int oldheight) {
        if (mSonarBitmap != null) {
            mSonarBitmap.recycle();
        }
        mSonarBitmap = getSonarBitmap(width, height);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new PlainOutline(width, height));
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

        canvas.drawBitmap(mSonarBitmap, getPaddingLeft(), getPaddingTop(), null);

        if (!mAnimator.isRunning()) return;

        float radius = mSonarBitmap.getWidth() / 2f;
        float centerX = getPaddingLeft() + radius;
        float centerY = getPaddingTop() + radius;

        canvas.save();
        canvas.rotate(-getScreenRotation(), centerX, centerY);

        drawPoints(canvas, centerX, centerY, radius);
        drawArc(canvas, centerX, centerY, radius);

        canvas.restore();
    }

    private void drawPoints(Canvas canvas, float centerX, float centerY, float radius) {
        if (!hasSensors()) return;

        float circleRadius = mPointSize / 2f;
        float maxRadius = radius - circleRadius - mStrokeWidth;

        for (SonarPoint point : mPointsList) {
            if (!point.isVisible()) continue;

            PointF circleCenter = SonarUtils.getPointOnCircle(centerX, centerY,
                    maxRadius * point.getDetectedDist(),
                    point.getDetectedAngle());

            mPointPaint.setAlpha((int) (point.getVisibility() * 255));

            canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, mPointPaint);
        }
    }

    private void drawArc(Canvas canvas, float centerX, float centerY, float radius) {
        if (!hasSensors()) return;

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

        final RectF rect = new RectF(centerX - radius, centerY + radius,
                centerX + radius, centerY - radius);

        canvas.drawArc(rect, degree, offset, true, mArcPaint);
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

        circleCanvas.drawCircle(center, center, radius, backgroundPaint);

        Paint innerBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerBackgroundPaint.setColor(mColor & INNER_CIRCLE_MASK);
        innerBackgroundPaint.setStyle(Paint.Style.FILL);

        circleCanvas.drawCircle(center, center, radius, innerBackgroundPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(mColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(mStrokeWidth);

        Paint thinStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thinStrokePaint.setColor(mColor);
        thinStrokePaint.setStyle(Paint.Style.STROKE);
        thinStrokePaint.setStrokeWidth(mThinStrokeWidth);

        float padding = mOuterBorder ? strokePaint.getStrokeWidth() / 2f : 0f;
        if (mOuterBorder) {
            circleCanvas.drawCircle(center, center, radius - padding, strokePaint);
        }
        circleCanvas.drawCircle(center, center, (radius - padding) * 0.75f, thinStrokePaint);
        circleCanvas.drawCircle(center, center, (radius - padding) * 0.5f, thinStrokePaint);
        circleCanvas.drawCircle(center, center, (radius - padding) * 0.25f, thinStrokePaint);

        for (int i = 0; i < LINE_COUNT; i++) {
            int angle = i * LINE_ANGLE;
            PointF start = SonarUtils.getPointOnCircle(center, center, radius, angle),
                    end = SonarUtils.getPointOnCircle(center, center, 0f, angle);

            circleCanvas.drawLine(start.x, start.y, end.x, end.y, thinStrokePaint);
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
    private class PlainOutline extends ViewOutlineProvider {

        private final int width;
        private final int height;

        private PlainOutline(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(view.getPaddingLeft(), view.getPaddingTop(),
                    width - getPaddingRight(), height - getPaddingBottom());
        }

    }

}