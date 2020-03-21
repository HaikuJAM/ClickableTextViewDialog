package com.haikujam.clickabletextview

import android.content.Context
import android.content.res.Resources
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes

object DictionaryDialogUtils {

    fun calculeRectOnScreen(view: View): RectF {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return RectF(
            location[0].toFloat(),
            location[1].toFloat(),
            (location[0] + view.measuredWidth).toFloat(),
            (location[1] + view.measuredHeight).toFloat()
        )
    }

    fun calculeRectInWindow(view: View): RectF {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return RectF(
            location[0].toFloat(),
            location[1].toFloat(),
            (location[0] + view.measuredWidth).toFloat(),
            (location[1] + view.measuredHeight).toFloat()
        )
    }

    fun dpFromPx(px: Float): Float {
        return px / Resources.getSystem().displayMetrics.density
    }

    fun pxFromDp(dp: Float): Float {
        return dp * Resources.getSystem().displayMetrics.density
    }

    fun setWidth(view: View, width: Float) {
        var params: ViewGroup.LayoutParams? = view.layoutParams
        if (params == null) {
            params = ViewGroup.LayoutParams(width.toInt(), view.height)
        } else {
            params.width = width.toInt()
        }
        view.layoutParams = params
    }

    fun dialogGravityToNotchDirection(dialogGravity: Int): Int {
        when (dialogGravity) {
            Gravity.START -> return NotchDrawable.RIGHT
            Gravity.END -> return NotchDrawable.LEFT
            Gravity.TOP -> return NotchDrawable.BOTTOM
            Gravity.BOTTOM -> return NotchDrawable.TOP
            Gravity.CENTER -> return NotchDrawable.TOP
            else -> throw IllegalArgumentException("Gravity must have be CENTER, START, END, TOP or BOTTOM.")
        }
    }

    fun setX(view: View, x: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.x = x.toFloat()
        } else {
            val marginParams = getOrCreateMarginLayoutParams(view)
            marginParams.leftMargin = x - view.left
            view.layoutParams = marginParams
        }
    }

    fun setY(view: View, y: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.y = y.toFloat()
        } else {
            val marginParams = getOrCreateMarginLayoutParams(view)
            marginParams.topMargin = y - view.top
            view.layoutParams = marginParams
        }
    }

    private fun getOrCreateMarginLayoutParams(view: View): ViewGroup.MarginLayoutParams {
        val lp = view.layoutParams
        return if (lp != null) {
            if (lp is ViewGroup.MarginLayoutParams) {
                lp
            } else {
                ViewGroup.MarginLayoutParams(lp)
            }
        } else {
            ViewGroup.MarginLayoutParams(view.width, view.height)
        }
    }

    fun removeOnGlobalLayoutListener(
        view: View,
        listener: ViewTreeObserver.OnGlobalLayoutListener
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        } else {

            view.viewTreeObserver.removeGlobalOnLayoutListener(listener)
        }
    }

    fun setTextAppearance(tv: TextView, @StyleRes textAppearanceRes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tv.setTextAppearance(textAppearanceRes)
        } else {

            tv.setTextAppearance(tv.context, textAppearanceRes)
        }
    }

    fun getColor(context: Context, @ColorRes colorRes: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(colorRes)
        } else {

            context.resources.getColor(colorRes)
        }
    }

    fun getDrawable(context: Context, @DrawableRes drawableRes: Int): Drawable? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getDrawable(drawableRes)
        } else {

            context.resources.getDrawable(drawableRes)
        }
    }


    fun findFrameLayout(anchorView: View): ViewGroup {
        var rootView = anchorView.rootView as ViewGroup
        if (rootView.childCount == 1 && rootView.getChildAt(0) is FrameLayout) {
            rootView = rootView.getChildAt(0) as ViewGroup
        }
        return rootView
    }
}