package com.maxxcodebug.info

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import org.json.JSONObject

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun View.dp(v: Float): Float = context.dp(v)

/** Safe string read: missing keys and JSON null both give an empty string. */
fun JSONObject?.s(key: String): String =
    if (this == null || isNull(key)) "" else optString(key)

/** Starts a spring animation on one view property. */
fun View.spring(
    prop: DynamicAnimation.ViewProperty,
    to: Float,
    stiffness: Float = SpringForce.STIFFNESS_LOW,
    damping: Float = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
) {
    SpringAnimation(this, prop, to).apply {
        spring.stiffness = stiffness
        spring.dampingRatio = damping
        start()
    }
}

/** Cards fly up with a bounce, one after another. */
fun View.springIn(index: Int) {
    alpha = 0f
    translationY = dp(56f)
    scaleX = 0.94f
    scaleY = 0.94f
    postDelayed({
        animate().alpha(1f).setDuration(240).start()
        spring(DynamicAnimation.TRANSLATION_Y, 0f)
        spring(DynamicAnimation.SCALE_X, 1f)
        spring(DynamicAnimation.SCALE_Y, 1f)
    }, 70L * index)
}

/** Jelly press: squish on touch, bounce back on release. Clicks still work. */
fun View.pressSpring() {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.spring(DynamicAnimation.SCALE_X, 0.92f, SpringForce.STIFFNESS_HIGH, SpringForce.DAMPING_RATIO_NO_BOUNCY)
                v.spring(DynamicAnimation.SCALE_Y, 0.92f, SpringForce.STIFFNESS_HIGH, SpringForce.DAMPING_RATIO_NO_BOUNCY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.spring(DynamicAnimation.SCALE_X, 1f, SpringForce.STIFFNESS_MEDIUM, SpringForce.DAMPING_RATIO_HIGH_BOUNCY)
                v.spring(DynamicAnimation.SCALE_Y, 1f, SpringForce.STIFFNESS_MEDIUM, SpringForce.DAMPING_RATIO_HIGH_BOUNCY)
            }
        }
        false
    }
}
