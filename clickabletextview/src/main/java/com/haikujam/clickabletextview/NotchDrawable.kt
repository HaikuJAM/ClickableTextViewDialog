package com.haikujam.clickabletextview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable

import androidx.annotation.ColorInt

class NotchDrawable internal constructor(@ColorInt foregroundColor: Int, private val mDirection: Int) :
    ColorDrawable() {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBackgroundColor: Int = Color.TRANSPARENT
    private var mPath: Path? = null

    init {
        this.mPaint.color = foregroundColor
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        updatePath(bounds)
    }

    @Synchronized
    private fun updatePath(bounds: Rect) {
        mPath = Path()

        when (mDirection) {
            LEFT -> {
                mPath!!.moveTo(bounds.width().toFloat(), bounds.height().toFloat())
                mPath!!.lineTo(0f, (bounds.height() / 2).toFloat())
                mPath!!.lineTo(bounds.width().toFloat(), 0f)
                mPath!!.lineTo(bounds.width().toFloat(), bounds.height().toFloat())
            }
            TOP -> {
                mPath!!.moveTo(0f, bounds.height().toFloat())
                mPath!!.lineTo((bounds.width() / 2).toFloat(), 0f)
                mPath!!.lineTo(bounds.width().toFloat(), bounds.height().toFloat())
                mPath!!.lineTo(0f, bounds.height().toFloat())
            }
            RIGHT -> {
                mPath!!.moveTo(0f, 0f)
                mPath!!.lineTo(bounds.width().toFloat(), (bounds.height() / 2).toFloat())
                mPath!!.lineTo(0f, bounds.height().toFloat())
                mPath!!.lineTo(0f, 0f)
            }
            BOTTOM -> {
                mPath!!.moveTo(0f, 0f)
                mPath!!.lineTo((bounds.width() / 2).toFloat(), bounds.height().toFloat())
                mPath!!.lineTo(bounds.width().toFloat(), 0f)
                mPath!!.lineTo(0f, 0f)
            }
        }

        mPath!!.close()
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(mBackgroundColor)
        if (mPath == null)
            updatePath(bounds)
        canvas.drawPath(mPath!!, mPaint)
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    override fun setColor(@ColorInt color: Int) {
        mPaint.color = color
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        if (mPaint.colorFilter != null) {
            return PixelFormat.TRANSLUCENT
        }

        when (mPaint.color.ushr(24)) {
            255 -> return PixelFormat.OPAQUE
            0 -> return PixelFormat.TRANSPARENT
        }
        return PixelFormat.TRANSLUCENT
    }

    companion object {

        const val LEFT = 0
        const val TOP = 1
        const val RIGHT = 2
        const val BOTTOM = 3
        const val AUTO = 4
    }
}