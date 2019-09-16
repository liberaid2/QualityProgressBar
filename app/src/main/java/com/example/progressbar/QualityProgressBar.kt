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

    private val drawRect = RectF()
    private var sweepAngle = 0f

    private val eraser = Paint()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = this@QualityProgressBar.strokeWidth
    }
    private val textPaint = Paint()
    private val textBounds = Rect()

    private val qualityInterpolators = mutableListOf<QualityInterpolator>()
    private val arcs: List<Arc>

    private var progressAnimationEnded = false

    init {
        eraser.apply {
            color = Color.WHITE
        }

        arcs = (0 until MAX_ARCS).map {
            Arc(it * ARC_STEP.toFloat(), ARC_STEP.toFloat(), paint)
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
            } finally {
                recycle()
            }
        }
    }

    fun animateArc() {
        qualityInterpolators.forEach { it.animation.cancel() }
        qualityInterpolators.clear()
        arcs.forEach { it.color = QualityColors.UNSPECIFIED.rbg }
        progressAnimationEnded = false

        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = totalAnimationDuration
            interpolator = LinearInterpolator()
            addUpdateListener {
                sweepAngle = it.animatedValue as Float
                invalidate()
            }
            addListener(onEnd = {
                qualityInterpolators.lastOrNull()?.animation?.addUpdateListener { invalidate() }
                progressAnimationEnded = true
            })
            start()
        }
    }

    fun setQuality(fromMillis: Long, toMillis: Long, quality: QualityColors): Boolean {
        val fromDeg = (fromMillis.toFloat() / totalAnimationDuration * 360f).toInt()
        val toDeg = (toMillis.toFloat() / totalAnimationDuration * 360f).toInt()

        return setQualityDeg(fromDeg, toDeg, quality)
    }

    fun setQualityDeg(fromDeg: Int, toDeg: Int, quality: QualityColors): Boolean {
        if(fromDeg < 0 || toDeg > 360)
            return false

        qualityInterpolators.forEach {
            val range = it.fromDeg until it.toDeg
            if(fromDeg in range || toDeg in range)
                return false
        }

        qualityInterpolators.add(QualityInterpolator(fromDeg, toDeg, quality, recolorAnimationDuration, arcs).apply {
            if(progressAnimationEnded)
                animation.addUpdateListener { invalidate() }
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

        paint.color = QualityColors.UNSPECIFIED.rbg

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

    private data class QualityInterpolator(val fromDeg: Int, val toDeg: Int, val quality: QualityColors,
                                           val animationDuration: Long, val arcs: List<Arc>){
        val animation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                for(i in fromDeg until toDeg)
                    arcs[i].color = argbEvaluator.evaluate(it.animatedFraction, QualityColors.UNSPECIFIED.rbg, quality.rbg) as Int
            }
            start()
        }
    }

    private class Arc(val startDeg: Float, val maxAngle: Float, val paint: Paint){
        var color = QualityColors.UNSPECIFIED.rbg
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

    enum class QualityColors(val rbg: Int) {
        GOOD(Color.GREEN),
        MEDIUM(Color.YELLOW),
        BAD(Color.RED),
        UNSPECIFIED(Color.GRAY)
    }
}