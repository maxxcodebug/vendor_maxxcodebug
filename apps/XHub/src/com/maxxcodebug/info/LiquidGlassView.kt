package com.maxxcodebug.info

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.color.MaterialColors
import kotlin.math.max

/**
 * Liquid glass container. On Android 13+ it copies what is behind it (the view set as
 * [source]), blurs it and bends it near the edges like a lens, then adds a soft rim light.
 * Older versions get a plain frosted look.
 */
class LiquidGlassView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : FrameLayout(ctx, attrs) {

    /** The view whose pixels appear behind the glass (usually the scroll view). */
    var source: View? = null

    private val scale = 0.25f
    private var snap: Bitmap? = null
    private var shader: RuntimeShader? = null
    private val scaleMatrix = Matrix()
    private val rect = RectF()
    private val loc = IntArray(2)
    private val srcLoc = IntArray(2)
    private var animUntil = 0L

    private val glass = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF }
    private val frost = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x77FFFFFF
        strokeWidth = ctx.dp(1.2f)
    }
    private var bgColor = 0xFF101010.toInt()

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bgColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface)
        frost.color = (bgColor and 0x00FFFFFF) or 0xD0000000.toInt()
    }

    /** Keeps redrawing for a while, used while cards are still springing into place. */
    fun animateFor(ms: Long) {
        animUntil = SystemClock.uptimeMillis() + ms
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        if (w == 0 || h == 0) return
        val r = h / 2f
        rect.set(0f, 0f, w.toFloat(), h.toFloat())

        val src = source
        if (Build.VERSION.SDK_INT >= 33 && src != null) {
            drawRefraction(canvas, src, w, h, r)
            canvas.drawRoundRect(rect, r, r, tint)
        } else {
            canvas.drawRoundRect(rect, r, r, frost)
        }

        val half = rim.strokeWidth / 2f
        rect.inset(half, half)
        canvas.drawRoundRect(rect, r - half, r - half, rim)

        if (animUntil > SystemClock.uptimeMillis()) postInvalidateOnAnimation()
    }

    private fun drawRefraction(canvas: Canvas, src: View, w: Int, h: Int, r: Float) {
        val sw = max(1, (w * scale).toInt())
        val sh = max(1, (h * scale).toInt())
        var b = snap
        if (b == null || b.width != sw || b.height != sh) {
            b = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
            snap = b
        }

        getLocationOnScreen(loc)
        src.getLocationOnScreen(srcLoc)

        val c = Canvas(b)
        c.drawColor(bgColor)
        c.scale(scale, scale)
        c.translate((srcLoc[0] - loc[0]).toFloat(), (srcLoc[1] - loc[1]).toFloat())
        src.draw(c)

        val s = shader ?: RuntimeShader(AGSL).also { shader = it }
        val bs = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        scaleMatrix.setScale(1f / scale, 1f / scale)
        bs.setLocalMatrix(scaleMatrix)
        s.setInputShader("bg", bs)
        s.setFloatUniform("size", w.toFloat(), h.toFloat())
        s.setFloatUniform("radius", r)
        s.setFloatUniform("bevel", h * 0.45f)
        s.setFloatUniform("strength", h * 0.35f)
        glass.shader = s
        canvas.drawRoundRect(rect, r, r, glass)
    }

    companion object {
        private const val AGSL = """
uniform shader bg;
uniform float2 size;
uniform float radius;
uniform float bevel;
uniform float strength;

float sdRR(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
}

half4 main(float2 fc) {
    float2 c = fc - size * 0.5;
    float d = sdRR(c, size * 0.5, radius);
    float depth = clamp(-d / bevel, 0.0, 1.0);
    float edge = 1.0 - depth;
    float bend = edge * edge * strength;
    float2 dir = normalize(c + float2(0.001));
    float2 uv = fc - dir * bend;
    half4 col = bg.eval(uv);
    col += bg.eval(uv + float2(4.0, 0.0));
    col += bg.eval(uv - float2(4.0, 0.0));
    col += bg.eval(uv + float2(0.0, 4.0));
    col += bg.eval(uv - float2(0.0, 4.0));
    col = col * 0.2;
    float rimLight = edge * edge * edge * edge;
    col.rgb = mix(col.rgb, half3(1.0), half(0.10 + rimLight * 0.35));
    return half4(col.rgb, 1.0);
}
"""
    }
}
