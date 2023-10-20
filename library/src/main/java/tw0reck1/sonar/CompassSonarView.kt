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
package tw0reck1.sonar

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Build
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.getSystemService
import tw0reck1.sonar.SonarUtils.clamp
import tw0reck1.sonar.SonarUtils.dpToPx
import tw0reck1.sonar.SonarUtils.getPointOnCircle
import tw0reck1.sonar.SonarUtils.spToPx
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CompassSonarView : RotaryView, Sonar {

    companion object {
        private val DIRECTIONS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

        private const val DEFAULT_COLOR = 0xff03CC02.toInt()
        private const val INNER_CIRCLE_MASK = 0x3fffffff.toInt()
        private const val ARC_MASK = 0xbfffffff.toInt()
        private const val TEXT_MASK = 0xbfffffff.toInt()
        private const val STROKE_MASK = 0x9fffffff.toInt()
        private const val POINT_GRADIENT_START_MASK = 0xffffffff.toInt()
        private const val POINT_GRADIENT_END_MASK = 0xbfffffff.toInt()

        private const val DEFAULT_FONT_SIZE = 28f
        private const val DEFAULT_THIN_FONT_SIZE = 21f
        private const val DEFAULT_STROKE_WIDTH = 2.5f
        private const val DEFAULT_THIN_STROKE_WIDTH = 1.25f
        private const val DEFAULT_POINT_SIZE = 16f

        private const val LINE_COUNT = 24
        private const val LINE_ANGLE = 360 / LINE_COUNT
        private const val SHORT_LINE_COUNT = 72
        private const val SHORT_LINE_ANGLE = 360 / SHORT_LINE_COUNT
        private val DIRECTION_ANGLE = 360 / DIRECTIONS.size

        private const val MIN_LOOP_DURATION = 250
        private const val DEFAULT_LOOP_DURATION = 1250
        private const val MAX_LOOP_DURATION = Int.MAX_VALUE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fontPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val smallFontPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val pointsInterpolator: Interpolator = DecelerateInterpolator(0.6f)

    private var sonarBitmap: Bitmap? = null

    private val pointsList: MutableList<SonarPoint> = LinkedList()

    private var scannerAngle = 0

    private var fontSize = DEFAULT_FONT_SIZE
    private var thinFontSize = DEFAULT_THIN_FONT_SIZE
    private var strokeWidth = DEFAULT_STROKE_WIDTH
    private var thinStrokeWidth = DEFAULT_THIN_STROKE_WIDTH
    private var pointSize = DEFAULT_POINT_SIZE

    private var color: Int = DEFAULT_COLOR
    private var arcColor = DEFAULT_COLOR and ARC_MASK

    private var loopDuration = DEFAULT_LOOP_DURATION

    private val animator: ValueAnimator = ValueAnimator.ofInt(0, 360).apply {
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        duration = DEFAULT_LOOP_DURATION.toLong()

        addUpdateListener { anim: ValueAnimator ->
            scannerAngle = anim.animatedValue as Int

            updateAngle()
            val currentMs = System.currentTimeMillis()
            for (point in pointsList) {
                point.detect(currentMs, 0, scannerAngle)
            }
            invalidate()
        }
    }

    constructor(
        context: Context
    ) : super(context) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        initAttributes(context, attrs, 0)
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initAttributes(context, attrs, defStyleAttr)
        init()
    }

    private fun initAttributes(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        var array = context.theme.obtainStyledAttributes(attrs, R.styleable.TextSonarView, defStyleAttr, 0)

        fontSize = array.getDimension(
            R.styleable.TextSonarView_sv_fontSize,
            context.spToPx(DEFAULT_FONT_SIZE)
        )
        thinFontSize = array.getDimension(
            R.styleable.TextSonarView_sv_thinFontSize,
            context.spToPx(DEFAULT_THIN_FONT_SIZE)
        )

        array.recycle()
        array = context.theme.obtainStyledAttributes(attrs, R.styleable.SonarView, defStyleAttr, 0)

        strokeWidth = array.getDimension(
            R.styleable.SonarView_sv_strokeWidth,
            context.dpToPx(DEFAULT_STROKE_WIDTH)
        )
        thinStrokeWidth = array.getDimension(
            R.styleable.SonarView_sv_thinStrokeWidth,
            context.dpToPx(DEFAULT_THIN_STROKE_WIDTH)
        )
        pointSize = array.getDimension(
            R.styleable.SonarView_sv_pointSize,
            context.dpToPx(DEFAULT_POINT_SIZE)
        )
        color = array.getColor(
            R.styleable.SonarView_sv_color,
            DEFAULT_COLOR
        )
        loopDuration = array.getInt(
            R.styleable.SonarView_sv_loopDuration,
            DEFAULT_LOOP_DURATION
        )
            .clamp(MIN_LOOP_DURATION, MAX_LOOP_DURATION)

        array.recycle()
    }

    private fun init() {
        isClickable = true

        arcColor = color and ARC_MASK

        arcPaint.style = Paint.Style.FILL
        pointPaint.color = color
        pointPaint.style = Paint.Style.FILL

        fontPaint.color = color
        fontPaint.textAlign = Paint.Align.CENTER
        fontPaint.isFakeBoldText = true
        fontPaint.textSize = fontSize

        smallFontPaint.color = color and TEXT_MASK
        smallFontPaint.textAlign = Paint.Align.CENTER
        smallFontPaint.textSize = thinFontSize

        animator.duration = loopDuration.toLong()
    }

    override fun setPoints(points: Iterable<SonarPoint>) {
        pointsList.clear()
        pointsList.addAll(points)
    }

    override fun addPoints(vararg points: SonarPoint) {
        for (point in points) {
            pointsList.add(point)
        }
    }

    override fun setColor(color: Int) {
        this.color = color

        arcColor = color and ARC_MASK

        pointPaint.color = color
        fontPaint.color = color
        smallFontPaint.color = color and TEXT_MASK

        sonarBitmap = sonarBitmap?.let { bitmap ->
            bitmap.recycle()

            getSonarBitmap(width, height)
        }

        invalidate()
    }

    override fun setStrokeWidth(strokeWidth: Float) {
        this.strokeWidth = strokeWidth

        sonarBitmap = sonarBitmap?.let { bitmap ->
            bitmap.recycle()

            getSonarBitmap(width, height)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = CompassOutline(width, height)
        }

        invalidate()
    }

    override fun setThinStrokeWidth(thinStrokeWidth: Float) {
        this.thinStrokeWidth = thinStrokeWidth

        sonarBitmap = sonarBitmap?.let { bitmap ->
            bitmap.recycle()

            getSonarBitmap(width, height)
        }

        invalidate()
    }

    override fun setFontSize(fontSize: Float) {
        this.fontSize = fontSize
        this.fontPaint.textSize = fontSize

        sonarBitmap = sonarBitmap?.let { bitmap ->
            bitmap.recycle()

            getSonarBitmap(width, height)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = CompassOutline(width, height)
        }

        invalidate()
    }

    override fun setThinFontSize(thinFontSize: Float) {
        this.thinFontSize = thinFontSize
        this.smallFontPaint.textSize = thinFontSize

        sonarBitmap = sonarBitmap?.let { bitmap ->
            bitmap.recycle()

            getSonarBitmap(width, height)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = CompassOutline(width, height)
        }

        invalidate()
    }

    override fun setPointSize(pointSize: Float) {
        this.pointSize = pointSize
    }

    override fun setSizes(
        strokeWidth: Float,
        thinStrokeWidth: Float,
        fontSize: Float,
        thinFontSize: Float,
        pointSize: Float
    ) {
        this.strokeWidth = strokeWidth
        this.thinStrokeWidth = thinStrokeWidth
        this.fontSize = fontSize
        this.fontPaint.textSize = fontSize
        this.thinFontSize = thinFontSize
        this.smallFontPaint.textSize = thinFontSize
        this.pointSize = pointSize

        sonarBitmap = sonarBitmap?.let { bitmap ->
            bitmap.recycle()

            getSonarBitmap(width, height)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = CompassOutline(width, height)
        }

        invalidate()
    }

    override fun startAnimation() {
        if (!animator.isRunning) {
            animator.start()
        }
    }

    override fun stopAnimation() {
        animator.cancel()
        currentAngle = 0
        invalidate()
    }

    override fun onSizeChanged(width: Int, height: Int, oldwidth: Int, oldheight: Int) {
        sonarBitmap?.recycle()
        sonarBitmap = getSonarBitmap(width, height)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = CompassOutline(width, height)
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawSonar(canvas)
    }

    private fun drawSonar(canvas: Canvas) {
        val bitmap = sonarBitmap ?: return

        val radius = bitmap.width / 2f
        val centerX = paddingLeft + radius
        val centerY = paddingTop + radius

        drawDirections(canvas, centerX, centerY, radius)

        canvas.save()
        canvas.rotate(currentAngle - screenRotation.toFloat(), centerX, centerY)

        canvas.drawBitmap(bitmap, paddingLeft.toFloat(), paddingTop.toFloat(), null)

        if (animator.isRunning) {
            drawPoints(canvas, centerX, centerY, radius)
            drawArc(canvas, centerX, centerY, radius)
        }

        canvas.restore()
    }

    private fun drawDirections(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val screenRotation = screenRotation
        val offset = max(fontSize, thinFontSize) / 2f
        var usedPaint: Paint

        DIRECTIONS.forEachIndexed { i, letter ->
            usedPaint = if (i % 2 == 0) {
                fontPaint
            } else {
                smallFontPaint
            }
            val start = getPointOnCircle(
                centerX,
                centerY,
                radius - offset,
                currentAngle - screenRotation + i * DIRECTION_ANGLE
            )
            canvas.drawText(
                letter,
                start.x,
                start.y - (usedPaint.descent() + usedPaint.ascent()) / 2,
                usedPaint
            )
        }
    }

    private fun drawPoints(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        if (!hasSensors) return

        val circleBaseRadius = pointSize / 2f

        val textSize = max(fontSize, thinFontSize)
        val maxRadius = radius - (textSize + strokeWidth / 2f)

        for (point in pointsList) {
            if (!point.isVisible) continue

            val visibility = pointsInterpolator.getInterpolation(point.visibility)
            val sizeRatio = 0.75f + 0.5f * (1f - visibility)
            val circleRadius = circleBaseRadius * sizeRatio

            val circleCenter = getPointOnCircle(
                centerX,
                centerY,
                (maxRadius * 0.9f - circleBaseRadius) * point.detectionDistance,
                point.angle
            )

            val pointColor = point.color ?: color
            val pointGradientStartColor = pointColor and POINT_GRADIENT_START_MASK
            val pointGradientEndColor = pointColor and POINT_GRADIENT_END_MASK

            pointPaint.shader = RadialGradient(
                circleCenter.x,
                circleCenter.y,
                1.25f * circleBaseRadius,
                pointGradientStartColor,
                pointGradientEndColor,
                Shader.TileMode.CLAMP
            )
            pointPaint.alpha = (visibility * 255f).roundToInt()

            canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, pointPaint)
        }
    }

    private fun drawArc(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        if (!hasSensors) return

        val transparentArc = 0x00ffffff and arcColor
        val gradient: Shader = SweepGradient(
            centerX,
            centerY,
            intArrayOf(
                transparentArc,
                transparentArc,
                transparentArc,
                transparentArc,
                transparentArc,
                arcColor
            ),
            null
        )

        val degree = -90f + scannerAngle
        val gradientMatrix = Matrix()
        gradientMatrix.preRotate(degree - 1f, centerX, centerY)
        gradient.setLocalMatrix(gradientMatrix)

        arcPaint.shader = gradient

        val offset = 360f
        val textSize = max(fontSize, thinFontSize)
        val inset = -0.9f * (radius - textSize - strokeWidth / 2f)
        val rect = RectF(centerX, centerY, centerX, centerY)
        rect.inset(inset, inset)
        canvas.drawArc(rect, degree, offset, true, arcPaint)
    }

    private fun getSonarBitmap(width: Int, height: Int): Bitmap? {
        val drawWidth = width - paddingLeft - paddingRight
        val drawHeight = height - paddingTop - paddingBottom
        val textSize = max(fontSize, thinFontSize)
        val diameter = min(drawWidth, drawHeight)
        val radius = diameter / 2f - textSize - strokeWidth / 2f
        val center = diameter / 2f
        val bitmapSize = diameter

        if (bitmapSize <= 0) return null

        val result = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val circleCanvas = Canvas(result)

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint.color = Color.BLACK
        backgroundPaint.style = Paint.Style.FILL

        circleCanvas.drawCircle(center, center, radius * 0.9f, backgroundPaint)

        val innerBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        innerBackgroundPaint.color = color and INNER_CIRCLE_MASK
        innerBackgroundPaint.style = Paint.Style.FILL

        circleCanvas.drawCircle(center, center, radius * 0.9f, innerBackgroundPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        strokePaint.color = color
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = strokeWidth
        strokePaint.strokeCap = Paint.Cap.ROUND

        val thinStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        thinStrokePaint.color = color and STROKE_MASK
        thinStrokePaint.style = Paint.Style.STROKE
        thinStrokePaint.strokeWidth = thinStrokeWidth
        thinStrokePaint.strokeCap = Paint.Cap.ROUND

        circleCanvas.drawCircle(center, center, radius * 0.9f, strokePaint)
        circleCanvas.drawCircle(center, center, radius * 0.6f, thinStrokePaint)
        circleCanvas.drawCircle(center, center, radius * 0.3f, thinStrokePaint)

        var usedPaint: Paint
        for (i in 0 until LINE_COUNT) {
            val angle = i * LINE_ANGLE
            val directionalLine = i % (LINE_COUNT / 4) == 0
            val centerOffset = if (directionalLine) {
                0.08f
            } else {
                0.12f
            }
            val start = getPointOnCircle(center, center, radius * 0.9f, angle)
            val end = getPointOnCircle(center, center, radius * centerOffset, angle)
            usedPaint = if (directionalLine) {
                strokePaint
            } else {
                thinStrokePaint
            }
            circleCanvas.drawLine(start.x, start.y, end.x, end.y, usedPaint)
        }

        thinStrokePaint.color = color
        val maxLineRadius = radius - strokeWidth / 4f
        for (i in 0 until SHORT_LINE_COUNT) {
            val angle = i * SHORT_LINE_ANGLE
            val startRadius = if (i % 3 == 0) {
                maxLineRadius
            } else {
                maxLineRadius * 0.96f
            }
            val start = getPointOnCircle(center, center, startRadius, angle)
            val end = getPointOnCircle(center, center, radius * 0.9f, angle)
            usedPaint = if (i % (SHORT_LINE_COUNT / 4) == 0) {
                strokePaint
            } else {
                thinStrokePaint
            }
            circleCanvas.drawLine(start.x, start.y, end.x, end.y, usedPaint)
        }

        return result
    }

    private val screenRotation: Int
        get() {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                context.getSystemService<WindowManager>()?.defaultDisplay
            }
            return when (display?.rotation) {
                Surface.ROTATION_270 -> 270
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_0 -> 0
                else -> 0
            }
        }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class CompassOutline(
        private val width: Int,
        private val height: Int
    ) : ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            val paddingLeft = paddingLeft
            val paddingTop = paddingTop
            val drawWidth = width - paddingLeft - paddingRight
            val drawHeight = height - paddingTop - paddingBottom

            val diameter = min(drawWidth, drawHeight)
            val textSize = max(fontSize, thinFontSize)
            val radius = diameter / 2f - textSize - strokeWidth / 2f

            val centerX = (paddingLeft + drawWidth / 2f).roundToInt()
            val centerY = (paddingTop + drawHeight / 2f).roundToInt()

            val bounds = Rect(centerX, centerY, centerX, centerY)

            val inset = (-0.9f * radius).roundToInt()
            bounds.inset(inset, inset)

            outline.setOval(bounds)
        }
    }
}
