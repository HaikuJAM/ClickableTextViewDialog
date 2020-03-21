package com.haikujam.clickabletextview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.*

class DictionaryDialog private constructor(builder: Builder) : PopupWindow.OnDismissListener {

    private var mContext: Context?
    private var mOnDismissListener: OnDismissListener? = null
    private var mOnShowListener: OnShowListener? = null
    private var mPopupWindow: PopupWindow? = null
    private val mGravity: Int
    private var mNotchDirection: Int = NotchDrawable.AUTO
    private val mDismissOnInsideTouch: Boolean
    private val mDismissOnOutsideTouch: Boolean
    private var mModal: Boolean? = null
    private var mContentView: View? = null
    private var mContentLayout: View? = null
    @IdRes
    private val mTextViewId: Int
    private val mText: CharSequence
    private var mAnchorView: TextView? = null
    private var mMaxWidth: Float = 0f
    private var mOverlay: View? = null
    private var mRootView: ViewGroup? = null
    private var mShowNotch: Boolean = false
    private var mNotchView: ImageView? = null
    private val mNotchDrawable: Drawable?
    private var mAnimated: Boolean = false
    private var mAnimator: AnimatorSet? = null
    private val mMargin: Float
    private val mPadding: Float
    private val mAnimationPadding: Float
    private val mAnimationDuration: Long
    private val mNotchWidth: Float
    private val mNotchHeight: Float
    private val mFocusable: Boolean
    private var dismissed = false
    private val width: Int
    private val height: Int

    val isShowing: Boolean
        get() = mPopupWindow != null && mPopupWindow!!.isShowing

    private val mLocationLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val popup = mPopupWindow
            if (popup == null || dismissed) return

            if (mMaxWidth > 0 && mContentView!!.width > mMaxWidth) {
                DictionaryDialogUtils.setWidth(mContentView!!, mMaxWidth)
                popup.update(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                return
            }

            DictionaryDialogUtils.removeOnGlobalLayoutListener(popup.contentView, this)
            popup.contentView.viewTreeObserver.addOnGlobalLayoutListener(mNotchLayoutListener)
            val location = calculePopupLocation()
            popup.isClippingEnabled = true
            popup.update(location.x.toInt(), location.y.toInt(), popup.width, popup.height)
            popup.contentView.requestLayout()
            createOverlay()
        }
    }

    private val mNotchLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val popup = mPopupWindow
            if (popup == null || dismissed) return

            DictionaryDialogUtils.removeOnGlobalLayoutListener(popup.contentView, this)

            popup.contentView.viewTreeObserver.addOnGlobalLayoutListener(mAnimationLayoutListener)
            popup.contentView.viewTreeObserver.addOnGlobalLayoutListener(mShowLayoutListener)
            if (mShowNotch) {
                val anchorRect = DictionaryDialogUtils.calculeRectOnScreen(mAnchorView!!)
                val contentViewRect = DictionaryDialogUtils.calculeRectOnScreen(mContentLayout!!)
                var x: Float
                var y: Float
                if (mNotchDirection == NotchDrawable.TOP || mNotchDirection == NotchDrawable.BOTTOM) {
                    x = mContentLayout!!.paddingLeft + DictionaryDialogUtils.pxFromDp(2f)
                    val centerX = contentViewRect.width() / 2f - mNotchView!!.width / 2f
                    val newX = centerX - (contentViewRect.centerX() - anchorRect.centerX())
                    if (newX > x) {
                        x = if (newX + mNotchView!!.width.toFloat() + x > contentViewRect.width()) {
                            contentViewRect.width() - mNotchView!!.width.toFloat() - x
                        } else {
                            newX
                        }
                    }
                    y = mNotchView!!.top.toFloat()
                    y += if (mNotchDirection == NotchDrawable.BOTTOM) -1 else +1
                } else {
                    y = mContentLayout!!.paddingTop + DictionaryDialogUtils.pxFromDp(2f)
                    val centerY = contentViewRect.height() / 2f - mNotchView!!.height / 2f
                    val newY = centerY - (contentViewRect.centerY() - anchorRect.centerY())
                    if (newY > y) {
                        y =
                            if (newY + mNotchView!!.height.toFloat() + y > contentViewRect.height()) {
                                contentViewRect.height() - mNotchView!!.height.toFloat() - y
                            } else {
                                newY
                            }
                    }
                    x = mNotchView!!.left.toFloat()
                    x += if (mNotchDirection == NotchDrawable.RIGHT) -1 else +1
                }
                DictionaryDialogUtils.setX(mNotchView!!, x.toInt())
                DictionaryDialogUtils.setY(mNotchView!!, y.toInt())
            }
            popup.contentView.requestLayout()
        }
    }

    private val mShowLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val popup = mPopupWindow
            if (popup == null || dismissed) return

            DictionaryDialogUtils.removeOnGlobalLayoutListener(popup.contentView, this)

            if (mOnShowListener != null)
                mOnShowListener!!.onShow(this@DictionaryDialog)
            mOnShowListener = null

            mContentLayout!!.visibility = View.VISIBLE
        }
    }

    private val mAnimationLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val popup = mPopupWindow
            if (popup == null || dismissed) return

            DictionaryDialogUtils.removeOnGlobalLayoutListener(popup.contentView, this)

            if (mAnimated) startAnimation()

            popup.contentView.requestLayout()
        }
    }

    private val mAutoDismissLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val popup = mPopupWindow
        if (popup == null || dismissed) return@OnGlobalLayoutListener

        if (!mRootView!!.isShown) dismiss()
    }


    init {
        mContext = builder.context
        mGravity = builder.gravity
        mNotchDirection = builder.notchDirection
        mDismissOnInsideTouch = builder.dismissOnInsideTouch
        mDismissOnOutsideTouch = builder.dismissOnOutsideTouch
        mModal = builder.modal
        mContentView = builder.contentView
        mTextViewId = builder.textViewId
        mText = builder.text
        mAnchorView = builder.anchorView
        mMaxWidth = builder.maxWidth
        mShowNotch = builder.showNotch
        mNotchWidth = builder.notchWidth
        mNotchHeight = builder.notchHeight
        mNotchDrawable = builder.notchDrawable
        mAnimated = builder.animated
        mMargin = builder.margin
        mPadding = builder.padding
        mAnimationPadding = builder.animationPadding
        mAnimationDuration = builder.animationDuration
        mOnDismissListener = builder.onDismissListener
        mOnShowListener = builder.onShowListener
        mFocusable = builder.focusable
        mRootView = DictionaryDialogUtils.findFrameLayout(mAnchorView!!)
        this.width = builder.width
        this.height = builder.height
        init()
    }

    private fun init() {
        configPopupWindow()
        configContentView()
    }

    private fun configPopupWindow() {
        mPopupWindow = PopupWindow(mContext, null, mDefaultPopupWindowStyleRes)
        mPopupWindow!!.setOnDismissListener(this)
        mPopupWindow!!.width = width
        mPopupWindow!!.height = height
        mPopupWindow!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mPopupWindow!!.isOutsideTouchable = true
        mPopupWindow!!.isTouchable = true
        mPopupWindow!!.setTouchInterceptor(View.OnTouchListener { v, event ->
            val x = event.x.toInt()
            val y = event.y.toInt()

            if (!mDismissOnOutsideTouch && event.action == MotionEvent.ACTION_DOWN
                && (x < 0 || x >= mContentLayout!!.measuredWidth || y < 0 || y >= mContentLayout!!.measuredHeight)
            ) {
                return@OnTouchListener true
            } else if (!mDismissOnOutsideTouch && event.action == MotionEvent.ACTION_OUTSIDE) {
                return@OnTouchListener true
            } else if (event.action == MotionEvent.ACTION_DOWN && mDismissOnInsideTouch) {
                dismiss()
                return@OnTouchListener true
            }
            false
        })
        mPopupWindow!!.isClippingEnabled = false
        mPopupWindow!!.isFocusable = mFocusable
    }


    fun show() {
        verifyDismissed()
        val wordX = " $mText "
        val clickedStringWithNoNewLine =
            mAnchorView?.text.toString().replace("\n", " ").replace("\t", " ")
        val pos = (" $clickedStringWithNoNewLine ").indexOf(wordX)
        val endPos = pos + mText.length
        val point = Point()
        mContentLayout!!.viewTreeObserver.addOnGlobalLayoutListener(mLocationLayoutListener)
        mContentLayout!!.viewTreeObserver.addOnGlobalLayoutListener(mAutoDismissLayoutListener)

        mRootView!!.post {
            if (mRootView!!.isShown) {
                getPointXYPositionToDisplayPopup(point, pos, endPos, mAnchorView)
                mPopupWindow!!.showAsDropDown(mAnchorView, point.x, point.y, Gravity.NO_GRAVITY)
            } else
                Log.e(TAG, "Dialog cannot be shown")
        }
    }

    private var globalLayout: Layout? = null
    private fun getPointXYPositionToDisplayPopup(
        point: Point,
        pos: Int,
        endPos: Int,
        jamEditLine: TextView?
    ) {
        val layout = jamEditLine?.layout
        try {
            mContentLayout?.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            point.x =
                ((layout?.getPrimaryHorizontal(pos)?.toInt()!! + layout.getPrimaryHorizontal(endPos).toInt()) - mContentLayout?.measuredWidth!!) / 2

            val line = layout.getLineForOffset(pos)
            point.y = layout.getLineBottom(line) - jamEditLine.height

            globalLayout = layout
        } catch (e: NullPointerException) {
            try {
                point.x =
                    ((globalLayout?.getPrimaryHorizontal(pos)?.toInt()!! + globalLayout?.getPrimaryHorizontal(
                        endPos
                    )?.toInt()!!) - mPopupWindow?.width!!) / 3
                val line = globalLayout?.getLineForOffset(pos)
                point.y = line?.let { globalLayout?.getLineBottom(it) } ?: 0
            } catch (exception: java.lang.NullPointerException) {
                point.x = 0
                point.y = 0
            } catch (ex: java.lang.IndexOutOfBoundsException) {
                point.x = 0
                point.y = 0
            }
        } catch (ex: java.lang.IndexOutOfBoundsException) {
            point.x = 0
        }
    }

    private fun verifyDismissed() {
        require(!dismissed) { "Dialog has been dismissed." }
    }

    private fun createOverlay() {
        mOverlay = View(mContext)
        mOverlay!!.layoutParams = ViewGroup.LayoutParams(mRootView!!.width, mRootView!!.height)
        mRootView!!.addView(mOverlay)
    }

    private fun calculePopupLocation(): PointF {
        val location = PointF()

        val anchorRect = DictionaryDialogUtils.calculeRectInWindow(mAnchorView!!)
        val anchorCenter = PointF(anchorRect.centerX(), anchorRect.centerY())

        when (mGravity) {
            Gravity.START -> {
                location.x = anchorRect.left - mPopupWindow!!.contentView.width.toFloat() - mMargin
                location.y = anchorCenter.y - mPopupWindow!!.contentView.height / 2f
            }
            Gravity.END -> {
                location.x = anchorRect.right + mMargin
                location.y = anchorCenter.y - mPopupWindow!!.contentView.height / 2f
            }
            Gravity.TOP -> {
                location.x = anchorCenter.x - mPopupWindow!!.contentView.width / 2f
                location.y = anchorRect.top - mPopupWindow!!.contentView.height.toFloat() - mMargin
            }
            Gravity.BOTTOM -> {
                location.x = anchorCenter.x - mPopupWindow!!.contentView.width / 2f
                location.y = anchorRect.bottom + mMargin
            }
            Gravity.CENTER -> {
                location.x = anchorCenter.x - mPopupWindow!!.contentView.width / 2f
                location.y = anchorCenter.y - mPopupWindow!!.contentView.height / 2f
            }
            else -> throw IllegalArgumentException("Gravity must have be  START, END, CENTER, TOP, BOTTOM.")
        }

        return location
    }

    private fun configContentView() {
        if (mContentView is TextView) {
            val tv = mContentView as TextView?
            tv!!.text = mText
        } else {
            val tv = mContentView!!.findViewById<View>(mTextViewId) as TextView
            tv.text = mText
        }

        mContentView?.setPadding(
            mPadding.toInt(),
            mPadding.toInt(),
            mPadding.toInt(),
            mPadding.toInt()
        )

        val linearLayout = LinearLayout(mContext)
        linearLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearLayout.orientation =
            if (mNotchDirection == NotchDrawable.LEFT || mNotchDirection == NotchDrawable.RIGHT) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        val layoutPadding = (if (mAnimated) mAnimationPadding.toInt() else 0)
        linearLayout.setPadding(layoutPadding, layoutPadding, layoutPadding, layoutPadding)

        if (mShowNotch) {
            mNotchView = ImageView(mContext)
            mNotchView!!.setImageDrawable(mNotchDrawable)
            val notchLayoutParams: LinearLayout.LayoutParams =
                if (mNotchDirection == NotchDrawable.TOP || mNotchDirection == NotchDrawable.BOTTOM) {
                    LinearLayout.LayoutParams(mNotchWidth.toInt(), mNotchHeight.toInt(), 0f)
                } else {
                    LinearLayout.LayoutParams(mNotchHeight.toInt(), mNotchWidth.toInt(), 0f)
                }

            notchLayoutParams.gravity = Gravity.CENTER
            mNotchView!!.layoutParams = notchLayoutParams

            if (mNotchDirection == NotchDrawable.BOTTOM || mNotchDirection == NotchDrawable.RIGHT) {
                linearLayout.addView(mContentView)
                linearLayout.addView(mNotchView)
            } else {
                linearLayout.addView(mNotchView)
                linearLayout.addView(mContentView)
            }
        } else {
            linearLayout.addView(mContentView)
        }

        val contentViewParams = LinearLayout.LayoutParams(width, height, 0f)
        contentViewParams.gravity = Gravity.CENTER
        mContentView?.layoutParams = contentViewParams

        mContentLayout = linearLayout
        mContentLayout!!.visibility = View.INVISIBLE
        mPopupWindow!!.contentView = mContentLayout
    }

    fun dismiss() {
        if (dismissed)
            return

        dismissed = true
        if (mPopupWindow != null) {
            mPopupWindow!!.dismiss()
        }
    }

    fun <T : View> findViewById(id: Int): T {

        return mContentLayout!!.findViewById<View>(id) as T
    }

    override fun onDismiss() {
        dismissed = true

        if (mAnimator != null) {
            mAnimator!!.removeAllListeners()
            mAnimator!!.end()
            mAnimator!!.cancel()
            mAnimator = null
        }


        if (mRootView != null && mOverlay != null) {
            mRootView!!.removeView(mOverlay)
        }
        mRootView = null
        mOverlay = null

        if (mOnDismissListener != null)
            mOnDismissListener!!.onDismiss(this)
        mOnDismissListener = null

        DictionaryDialogUtils.removeOnGlobalLayoutListener(
            mPopupWindow!!.contentView,
            mLocationLayoutListener
        )
        DictionaryDialogUtils.removeOnGlobalLayoutListener(
            mPopupWindow!!.contentView,
            mNotchLayoutListener
        )
        DictionaryDialogUtils.removeOnGlobalLayoutListener(
            mPopupWindow!!.contentView,
            mShowLayoutListener
        )
        DictionaryDialogUtils.removeOnGlobalLayoutListener(
            mPopupWindow!!.contentView,
            mAnimationLayoutListener
        )
        DictionaryDialogUtils.removeOnGlobalLayoutListener(
            mPopupWindow!!.contentView,
            mAutoDismissLayoutListener
        )

        mPopupWindow = null
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun startAnimation() {
        val property =
            if (mGravity == Gravity.TOP || mGravity == Gravity.BOTTOM) "translationY" else "translationX"

        val anim1 =
            ObjectAnimator.ofFloat(mContentLayout, property, -mAnimationPadding, mAnimationPadding)
        anim1.duration = mAnimationDuration
        anim1.interpolator = AccelerateDecelerateInterpolator()

        val anim2 =
            ObjectAnimator.ofFloat(mContentLayout, property, mAnimationPadding, -mAnimationPadding)
        anim2.duration = mAnimationDuration
        anim2.interpolator = AccelerateDecelerateInterpolator()

        mAnimator = AnimatorSet()
        mAnimator!!.playSequentially(anim1, anim2)
        mAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!dismissed && isShowing) {
                    animation.start()
                }
            }
        })
        mAnimator!!.start()
    }

    interface OnDismissListener {
        fun onDismiss(dialog: DictionaryDialog)
    }

    interface OnShowListener {
        fun onShow(dialog: DictionaryDialog)
    }


    class Builder(val context: Context?) {
        var dismissOnInsideTouch = true
        var dismissOnOutsideTouch = true
        var modal = false
        var contentView: View? = null
        @IdRes
        var textViewId = android.R.id.text1
        var text: CharSequence = ""
        var anchorView: TextView? = null
        var notchDirection = NotchDrawable.AUTO
        var gravity = Gravity.BOTTOM
        var maxWidth: Float = 0.toFloat()
        var showNotch = true
        var notchDrawable: Drawable? = null
        var animated = false
        var margin = -1f
        var padding = -1f
        var animationPadding = -1f
        var onDismissListener: OnDismissListener? = null
        var onShowListener: OnShowListener? = null
        var animationDuration: Long = 0
        private var backgroundColor: Int = 0
        private var textColor: Int = 0
        private var textFont: Typeface?=null
        private var notchColor: Int = 0
        var notchHeight: Float = 0.toFloat()
        var notchWidth: Float = 0.toFloat()
        var focusable: Boolean = false
        var width = ViewGroup.LayoutParams.WRAP_CONTENT
        var height = ViewGroup.LayoutParams.WRAP_CONTENT

        @Throws(IllegalArgumentException::class)
        fun build(): DictionaryDialog {
            validateArguments()
            if (backgroundColor == 0) {
                backgroundColor =
                    DictionaryDialogUtils.getColor(context!!, mDefaultBackgroundColorRes)
            }

            if (textColor == 0) {
                textColor = DictionaryDialogUtils.getColor(context!!, mDefaultTextColorRes)
            }
            if (contentView == null) {
                val tv = TextView(context)
                DictionaryDialogUtils.setTextAppearance(tv, mDefaultTextAppearanceRes)
                tv.setBackgroundColor(backgroundColor)
                tv.setTextColor(textColor)
                if(textFont!=null) {
                    tv.typeface = textFont
                }
                contentView = tv
            }
            if (notchColor == 0) {
                notchColor = DictionaryDialogUtils.getColor(context!!, mDefaultNotchColorRes)
            }
            if (margin < 0) {
                margin = context!!.resources.getDimension(mDefaultMarginRes)
            }
            if (padding < 0) {
                padding = context!!.resources.getDimension(mDefaultPaddingRes)
            }
            if (animationPadding < 0) {
                animationPadding = context!!.resources.getDimension(mDefaultAnimationPaddingRes)
            }
            if (animationDuration == 0L) {
                animationDuration =
                    context!!.resources.getInteger(mDefaultAnimationDurationRes).toLong()
            }
            if (showNotch) {
                if (notchDirection == NotchDrawable.AUTO)
                    notchDirection = DictionaryDialogUtils.dialogGravityToNotchDirection(gravity)
                if (notchDrawable == null)
                    notchDrawable = NotchDrawable(notchColor, notchDirection)
                if (notchWidth == 0f)
                    notchWidth = context!!.resources.getDimension(mDefaultNotchWidthRes)
                if (notchHeight == 0f)
                    notchHeight = context!!.resources.getDimension(mDefaultNotchHeightRes)
            }
            return DictionaryDialog(this)
        }

        @Throws(IllegalArgumentException::class)
        private fun validateArguments() {
            requireNotNull(context) { "Context not defined." }
            requireNotNull(anchorView) { "Anchor not defined." }
        }

        fun setWidth(width: Int): Builder {
            this.width = width
            return this
        }

        fun setHeight(height: Int): Builder {
            this.height = height
            return this
        }

        fun contentView(textView: TextView): Builder {
            this.contentView = textView
            this.textViewId = 0
            return this
        }

        fun contentView(contentView: View, @IdRes textViewId: Int): Builder {
            this.contentView = contentView
            this.textViewId = textViewId
            return this
        }


        fun contentView(@LayoutRes contentViewId: Int, @IdRes textViewId: Int): Builder {
            val inflater =
                context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            this.contentView = inflater.inflate(contentViewId, null, false)
            this.textViewId = textViewId
            return this
        }


        fun contentView(@LayoutRes contentViewId: Int): Builder {
            val inflater =
                context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            this.contentView = inflater.inflate(contentViewId, null, false)
            this.textViewId = 0
            return this
        }


        fun dismissOnInsideTouch(dismissOnInsideTouch: Boolean): Builder {
            this.dismissOnInsideTouch = dismissOnInsideTouch
            return this
        }


        fun dismissOnOutsideTouch(dismissOnOutsideTouch: Boolean): Builder {
            this.dismissOnOutsideTouch = dismissOnOutsideTouch
            return this
        }


        fun modal(modal: Boolean): Builder {
            this.modal = modal
            return this
        }


        fun text(text: CharSequence): Builder {
            this.text = text
            return this
        }

        fun text(@StringRes textRes: Int): Builder {
            this.text = context!!.getString(textRes)
            return this
        }

        fun anchorView(anchorView: TextView): Builder {
            this.anchorView = anchorView
            return this
        }

        fun gravity(gravity: Int): Builder {
            this.gravity = gravity
            return this
        }


        fun notchDirection(notchDirection: Int): Builder {
            this.notchDirection = notchDirection
            return this
        }


        fun maxWidth(@DimenRes maxWidthRes: Int): Builder {
            this.maxWidth = context!!.resources.getDimension(maxWidthRes)
            return this
        }


        fun maxWidth(maxWidth: Float): Builder {
            this.maxWidth = maxWidth
            return this
        }


        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        fun animated(animated: Boolean): Builder {
            this.animated = animated
            return this
        }


        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        fun animationPadding(animationPadding: Float): Builder {
            this.animationPadding = animationPadding
            return this
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        fun animationPadding(@DimenRes animationPaddingRes: Int): Builder {
            this.animationPadding = context!!.resources.getDimension(animationPaddingRes)
            return this
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        fun animationDuration(animationDuration: Long): Builder {
            this.animationDuration = animationDuration
            return this
        }


        fun padding(padding: Float): Builder {
            this.padding = padding
            return this
        }


        fun padding(@DimenRes paddingRes: Int): Builder {
            this.padding = context!!.resources.getDimension(paddingRes)
            return this
        }


        fun margin(margin: Float): Builder {
            this.margin = margin
            return this
        }


        fun margin(@DimenRes marginRes: Int): Builder {
            this.margin = context!!.resources.getDimension(marginRes)
            return this
        }

        fun textColor(textColor: Int): Builder {
            this.textColor = textColor
            return this
        }

        fun textFont(font: Typeface): Builder {
            this.textFont= font
            return this
        }

        fun backgroundColor(@ColorInt backgroundColor: Int): Builder {
            this.backgroundColor = backgroundColor
            return this
        }

        fun showNotch(showNotch: Boolean): Builder {
            this.showNotch = showNotch
            return this
        }

        fun notchDrawable(notchDrawable: Drawable): Builder {
            this.notchDrawable = notchDrawable
            return this
        }

        fun notchDrawable(@DrawableRes drawableRes: Int): Builder {
            this.notchDrawable = DictionaryDialogUtils.getDrawable(context!!, drawableRes)
            return this
        }

        fun notchColor(@ColorInt notchColor: Int): Builder {
            this.notchColor = notchColor
            return this
        }

        fun notchHeight(notchHeight: Float): Builder {
            this.notchHeight = notchHeight
            return this
        }


        fun notchWidth(notchWidth: Float): Builder {
            this.notchWidth = notchWidth
            return this
        }

        fun onDismissListener(onDismissListener: OnDismissListener): Builder {
            this.onDismissListener = onDismissListener
            return this
        }

        fun onShowListener(onShowListener: OnShowListener): Builder {
            this.onShowListener = onShowListener
            return this
        }

        fun focusable(focusable: Boolean): Builder {
            this.focusable = focusable
            return this
        }
    }

    companion object {

        private val TAG = DictionaryDialog::class.java.simpleName

        private val mDefaultPopupWindowStyleRes = android.R.attr.popupWindowStyle
        private val mDefaultTextAppearanceRes = R.style.dictionary_dialog_default
        private val mDefaultBackgroundColorRes = R.color.dictionary_dialog_background
        private val mDefaultTextColorRes = R.color.dictionary_dialog_text
        private val mDefaultNotchColorRes = R.color.dictionary_dialog_notch
        private val mDefaultMarginRes = R.dimen.dictionary_dialog_margin
        private val mDefaultPaddingRes = R.dimen.dictionary_dialog_padding
        private val mDefaultAnimationPaddingRes = R.dimen.dictionary_dialog_animation_padding
        private val mDefaultAnimationDurationRes = R.integer.dictionary_dialog_animation_duration
        private val mDefaultNotchWidthRes = R.dimen.dictionary_dialog_notch_width
        private val mDefaultNotchHeightRes = R.dimen.dictionary_dialog_notch_height
    }
}