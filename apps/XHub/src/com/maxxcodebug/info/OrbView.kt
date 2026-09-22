package com.maxxcodebug.info

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A living liquid orb with an X in the middle. Its colour follows the build type. */
class OrbView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val blob = Paint(Paint.ANTI_ALIAS_FLAG)
    private val core = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val path = Path()
    private val oval = RectF()
    private var t = 0f
    private var tint = Color.parseColor("#B39DDB")

    private val loop = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
        duration = 7000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            t = it.animatedValue as Float
            invalidate()
        }
    }

    fun setStatusColor(c: Int) {
        tint = c
        rebuildCore()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loop.start()
    }

    override fun onDetachedFromWindow() {
        loop.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        rebuildCore()
    }

    private fun rebuildCore() {
        val r = min(width, height) * 0.3f
        if (r <= 0f) return
        core.shader = RadialGradient(
            width / 2f, height / 2f - r * 0.3f, r * 1.4f,
            lighten(tint), tint, Shader.TileMode.CLAMP
        )
    }

    private fun lighten(c: Int): Int {
        val a = 0.45f
        return Color.rgb(
            (Color.red(c) + (255 - Color.red(c)) * a).toInt(),
            (Color.green(c) + (255 - Color.green(c)) * a).toInt(),
            (Color.blue(c) + (255 - Color.blue(c)) * a).toInt()
        )
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val base = min(width, height) * 0.3f * (1f + 0.03f * sin(t))
        val tau = 2f * PI.toFloat()
        val n = 72

        for (layer in 2 downTo 0) {
            path.reset()
            val fac = 1f + 0.3f * layer
            for (i in 0..n) {
                val a = i * tau / n
                val wobble = 1f +
                    0.07f * sin(3f * a + t + layer) +
                    0.05f * sin(5f * a - 2f * t) +
                    0.03f * sin(2f * a + 3f * t + layer)
                val r = base * fac * wobble
                val x = cx + r * cos(a)
                val y = cy + r * sin(a)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            if (layer == 0) {
                canvas.drawPath(path, core)
            } else {
                blob.color = tint
                blob.alpha = if (layer == 2) 45 else 95
                canvas.drawPath(path, blob)
            }
        }

        oval.set(cx - base * 0.6f, cy - base * 0.95f, cx + base * 0.15f, cy - base * 0.3f)
        canvas.drawOval(oval, shine)

        label.textSize = base * 0.8f
        canvas.drawText("X", cx, cy - (label.ascent() + label.descent()) / 2f, label)
    }
}
