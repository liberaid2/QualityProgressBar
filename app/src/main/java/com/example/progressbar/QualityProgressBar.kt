package com.example.progressbar

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.animation.addListener
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import kotlin.math.min

class QualityProgressBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var strokeWidth = 25f
    set(value) {
        if(value > 0f){
            field = value
            invalidate()
            requestLayout()
        }
    }

    var totalAnimationDuration = 10 * 1000L
    set(value) {
        if(value > 0)
            field = value
    }

    var recolorAnimationDuration = 500L
    set(value) {
        if(value > 0)
            field = value
    }

    var bgColor: Int = Color.WHITE
    set(value) {
        field = value
        invalidate()
        requestLayout()
    }

    var text = ""
    set(value) {
        field = value
        invalidate()
        requestLayout()
    }

    var colorGood: Int = Color.GREEN
    set(value) {
        field = value
        invalidate()
        requestLayout()
    }

    var colorMedium: Int = Color.YELLOW
    set(value) {
        field = value
        invalidate()
        requestLayout()
    }

    var colorBad: Int = Color.RED
    set(value){
        field = value
        invalidate()
        requestLayout()
    }

    var colorUnspecified: Int = Color.GRAY
    set(value) {
        field = value
        invalidate()
        requestLayout()
    }

    private val drawRect = RectF()
    private var sweepAngle = 0f

    private val eraser = Paint()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = this@QualityProgressBar.strokeWidth
    }
    private val textPaint = Paint()
    private val textBounds = Rect()

    private val qualityAnimators = mutableListOf<Pair<ColoredArc, ValueAnimator>>()
    private val arcs: List<Arc>

    private var progressAnimationEnded = false

    init {
        eraser.apply {
            color = Color.WHITE
        }

        arcs = (0 until MAX_ARCS).map {
            Arc(it * ARC_STEP.toFloat(), ARC_STEP.toFloat(), paint, colorUnspecified)
        }

        context.theme.obtainStyledAttributes(attrs, R.styleable.QualityProgressBar, 0, 0).apply {
            try{
                strokeWidth = getDimension(R.styleable.QualityProgressBar_strokeWidth, 25f)
                totalAnimationDuration = getInteger(R.styleable.QualityProgressBar_totalAnimationDuration, 10000).toLong()
                recolorAnimationDuration = getInteger(R.styleable.QualityProgressBar_recolorAnimationDuration, 500).toLong()
                bgColor = getColor(R.styleable.QualityProgressBar_bgColor, Color.WHITE)
                textPaint.color = getColor(R.styleable.QualityProgressBar_textColor, Color.DKGRAY)
                textPaint.textSize = getDimension(R.styleable.QualityProgressBar_textSize, 25f)
                text = getString(R.styleable.QualityProgressBar_text) ?: ""
                colorGood = getColor(R.styleable.QualityProgressBar_colorGood, Color.GREEN)
                colorMedium = getColor(R.styleable.QualityProgressBar_colorMedium, Color.YELLOW)
                colorBad = getColor(R.styleable.QualityProgressBar_colorBad, Color.RED)
                colorUnspecified = getColor(R.styleable.QualityProgressBar_colorUnspecified, Color.GRAY)
            } finally {
                recycle()
            }
        }
    }

    fun animateArc() {
        qualityAnimators.forEach { it.second.cancel() }
        qualityAnimators.clear()
        arcs.forEach { it.color = colorUnspecified }
        progressAnimationEnded = false

        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = totalAnimationDuration
            interpolator = LinearInterpolator()
            addUpdateListener {
                sweepAngle = it.animatedValue as Float
                invalidate()
            }
            addListener(onEnd = {
                qualityAnimators.lastOrNull()?.second?.addUpdateListener { invalidate() }
                progressAnimationEnded = true
            })
            start()
        }
    }

    fun setQuality(fromMillis: Long, toMillis: Long, quality: Quality): Boolean {
        val fromDeg = (fromMillis.toFloat() / totalAnimationDuration * 360f).toInt()
        val toDeg = (toMillis.toFloat() / totalAnimationDuration * 360f).toInt()

        return setQualityDeg(fromDeg, toDeg, quality)
    }

    fun setQualityDeg(fromDeg: Int, toDeg: Int, quality: Quality): Boolean {
        if(fromDeg < 0 || toDeg > 360)
            return false

        qualityAnimators.forEach { (coloredArc, _) ->
            val range = coloredArc.fromDeg until coloredArc.toDeg
            if(fromDeg in range || toDeg in range)
                return false
        }

        qualityAnimators.add(ColoredArc(fromDeg, toDeg) to ValueAnimator.ofFloat(0f, 1f).apply {
            duration = recolorAnimationDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                for(i in fromDeg until toDeg)
                    arcs[i].color = argbEvaluator.evaluate(it.animatedFraction, colorUnspecified, getColorByQuality(quality)) as Int

                if(progressAnimationEnded)
                    invalidate()
            }
            start()
        })

        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val xPad = paddingLeft + paddingRight
        val yPad = paddingTop + paddingBottom
        val width = measuredWidth - xPad
        val height = measuredHeight - yPad

        val size = if(width < height) width else height
        setMeasuredDimension(size + xPad, size + yPad)
    }

    override fun onDraw(canvas: Canvas?) {
        val minSide = min(width, height)

        val side = minSide.toFloat()
        val (x, y) = if(minSide == width) {
            0f to (height / 2f - side / 2)
        } else (width / 2f - side / 2) to 0f

        drawRect.set(x + marginStart + paddingStart, y + marginTop + paddingTop, x + side - marginEnd - paddingEnd, y + side - marginBottom - paddingBottom)

        canvas?.drawColor(Color.WHITE)

        paint.color = colorUnspecified

        arcs.forEach { arc ->
            val diff = sweepAngle - arc.startDeg
            if(diff > 0){
                arc.sweepAngle = diff
                arc.draw(drawRect, canvas)
            }
        }

        canvas?.drawCircle(x + side / 2f, y + side / 2f, side / 2f - strokeWidth, eraser)

        textPaint.getTextBounds(text, 0, text.length, textBounds)
        canvas?.drawText(text, x + side / 2f - textBounds.width() / 2, y + side / 2f + textBounds.height() / 2, textPaint)
    }

    private fun getColorByQuality(quality: Quality): Int = when(quality) {
        Quality.GOOD -> colorGood
        Quality.MEDIUM -> colorMedium
        Quality.BAD -> colorBad
        Quality.UNSPECIFIED -> colorUnspecified
    }

    private data class ColoredArc(val fromDeg: Int, val toDeg: Int)

    enum class Quality{
        GOOD,
        MEDIUM,
        BAD,
        UNSPECIFIED
    }

    private class Arc(val startDeg: Float, val maxAngle: Float, val paint: Paint, var color: Int){
        var sweepAngle = 0f
        set(value){
            if(value < 0f)
                field = 0f
            else if(value > maxAngle)
                field = maxAngle
            else field = value
        }

        fun draw(rect: RectF, canvas: Canvas?){
            paint.color = color
            canvas?.drawArc(rect, startDeg, sweepAngle, true, paint)
        }
    }

    companion object {
        private val argbEvaluator = ArgbEvaluator()

        const val MAX_ARCS = 360
        const val ARC_STEP = MAX_ARCS / 360
    }
}