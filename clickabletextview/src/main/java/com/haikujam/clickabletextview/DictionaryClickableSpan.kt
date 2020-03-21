package com.haikujam.clickabletextview

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.content.res.getFontOrThrow


class DictionaryClickableSpan(
    private val jamEditLineEt: TextView,
    private val mContext: Context?,
    private val position: Int,
    attrs: AttributeSet?,
    defStyle: Int
) : ClickableSpan() {

    private var dictionaryDialog: DictionaryDialog? = null

    private val attribute =
        mContext?.obtainStyledAttributes(attrs, R.styleable.ClickableTextViewDialog, defStyle, 0)
    private val mDialogDismissOnInsideTouch = attribute?.getBoolean(
        R.styleable.ClickableTextViewDialog_dialog_dismiss_on_inside_touch,
        true
    )
    private val mDialogDismissOnOutsideTouch = attribute?.getBoolean(
        R.styleable.ClickableTextViewDialog_dialog_dismiss_on_inside_touch,
        true
    )
    private val mDialogModal =
        attribute?.getBoolean(R.styleable.ClickableTextViewDialog_dialog_modal, true)
    private val mDialogContentView =
        attribute?.getResourceId(R.styleable.ClickableTextViewDialog_dialog_content_view, 0)
    private val mDialogTextViewID =
        attribute?.getInteger(R.styleable.ClickableTextViewDialog_dialog_text_view_id, 0)
    private val mDialogNotchDrawable =
        attribute?.getDrawable(R.styleable.ClickableTextViewDialog_dialog_notch_drawable)
    private val mDialogNotchDirection =
        attribute?.getInteger(R.styleable.ClickableTextViewDialog_dialog_notch_direction, 0)
    private val mDialogGravity =
        attribute?.getInteger(R.styleable.ClickableTextViewDialog_dialog_gravity, 0)
    private val mDialogMaxWidth =
        attribute?.getFloat(R.styleable.ClickableTextViewDialog_dialog_max_width, 0f)
    private val mDialogShowNotch =
        attribute?.getBoolean(R.styleable.ClickableTextViewDialog_dialog_show_notch, true)
    private val mDialogAnimated =
        attribute?.getBoolean(R.styleable.ClickableTextViewDialog_dialog_animated, true)
    private val mDialogMargin = attribute?.getFloat(R.styleable.ClickableTextViewDialog_dialog_margin, 0f)
    private val mDialogPadding = attribute?.getFloat(R.styleable.ClickableTextViewDialog_dialog_padding, 0f)
    private val mDialogAnimationPadding =
        attribute?.getFloat(R.styleable.ClickableTextViewDialog_dialog_animation_padding, 0f)
    private val mDialogAnimationDuration =
        attribute?.getFloat(R.styleable.ClickableTextViewDialog_dialog_animation_duration, 0f)
    private val mDialogBackgroundColor =
        attribute?.getColor(R.styleable.ClickableTextViewDialog_dialog_background_color, 0)
    private val mDialogTextColor =
        attribute?.getColor(R.styleable.ClickableTextViewDialog_dialog_text_color, 0)
    private val mDialogNotchColor =
        attribute?.getColor(R.styleable.ClickableTextViewDialog_dialog_notch_color, 0)
//    private val mDialogTextFont =
//        attribute?.getFontOrThrow(R.styleable.ClickableTextViewDialog_dialog_text_font)
    private val mDialogNotchWidth =
        attribute?.getFloat(R.styleable.ClickableTextViewDialog_dialog_notch_width, 30f)
    private val mDialogNotchHeight =
        attribute?.getFloat(R.styleable.ClickableTextViewDialog_dialog_notch_height, 0f)
    private val mDialogWidth = attribute?.getFloat(R.styleable.ClickableTextViewDialog_dialog_width, 0f)
    private val mDialogHeight = attribute?.getFloat(R.styleable.ClickableTextViewDialog_dialog_height, 0f)
    private val mDialogFocusable =
        attribute?.getBoolean(R.styleable.ClickableTextViewDialog_dialog_focusable, false)

    override fun onClick(view: View) {
        if (dictionaryDialog != null && dictionaryDialog?.isShowing!!) {
            dictionaryDialog?.dismiss()
            return
        }
        if (view is ClickableTextView) {

            var cursorStartPosition = jamEditLineEt.selectionStart
            var cursorEndPosition = jamEditLineEt.selectionEnd

            val word: String

            if (cursorStartPosition < 0 || cursorEndPosition < 0) {
                val clickedStringWithNoNewLine =
                    jamEditLineEt.text.toString().replace("\n", " ").replace("\t", " ")
                val splitWords = clickedStringWithNoNewLine.split("\\s+".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()
                word = splitWords[position]
                val wordX = " $word "
                cursorStartPosition = (" $clickedStringWithNoNewLine ").indexOf(wordX)
                cursorEndPosition = (cursorStartPosition) + word.length
            } else {
                word = jamEditLineEt.text.substring(cursorStartPosition, cursorEndPosition)
            }

            if (word.matches("[\\p{InBASIC_LATIN}]*".toRegex())) {
                val color = jamEditLineEt.currentTextColor
                val underlineSpan = ColoredUnderlineSpan(color)
                val ssb = SpannableStringBuilder(jamEditLineEt.text)
                ssb.setSpan(
                    underlineSpan, cursorStartPosition,
                    cursorEndPosition, 0
                )
                jamEditLineEt.text = ssb

                showDictionaryPopUp(word)
            }
        }
    }

    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = true
    }

    private fun showDictionaryPopUp(word: String) {
        val dictionaryDialogBuilder = DictionaryDialog.Builder(mContext)
        dictionaryDialogBuilder.anchorView(jamEditLineEt)
        dictionaryDialogBuilder.text(word)
        if (mDialogShowNotch != null) {
            dictionaryDialogBuilder.showNotch(mDialogShowNotch)
        }
        if (mDialogNotchWidth != null && mDialogNotchWidth>0) {
            dictionaryDialogBuilder.notchWidth(mDialogNotchWidth)
        }
        if (mDialogNotchDirection != null&& mDialogNotchDirection>0) {
            dictionaryDialogBuilder.notchDirection(mDialogNotchDirection)
        }
        if (mDialogAnimated != null) {
            dictionaryDialogBuilder.animated(mDialogAnimated)
        }
        if (mDialogModal != null) {
            dictionaryDialogBuilder.modal(mDialogModal)
        }
        if (mDialogContentView != null&& mDialogContentView>0) {
            dictionaryDialogBuilder.contentView(mDialogContentView)
        }
        if (mDialogTextViewID != null&& mDialogTextViewID>0) {
            dictionaryDialogBuilder.textViewId = mDialogTextViewID
        }
        if (mDialogNotchDrawable != null) {
            dictionaryDialogBuilder.notchDrawable(mDialogNotchDrawable)
        }
        if (mDialogGravity != null&& mDialogGravity>0) {
            dictionaryDialogBuilder.gravity(mDialogGravity)
        }
        if (mDialogMaxWidth != null&& mDialogMaxWidth>0) {
            dictionaryDialogBuilder.maxWidth(mDialogMaxWidth)
        }
        if (mDialogMargin != null&& mDialogMargin>0) {
            dictionaryDialogBuilder.margin(mDialogMargin)
        }
        if (mDialogPadding != null&& mDialogPadding>0) {
            dictionaryDialogBuilder.padding(mDialogPadding)
        }
        if (mDialogAnimationPadding!=null&& mDialogAnimationPadding>0) {
            dictionaryDialogBuilder.animationPadding(mDialogAnimationPadding)
        }
        if (mDialogAnimationDuration!=null&& mDialogAnimationDuration>0) {
            dictionaryDialogBuilder.animationDuration(mDialogAnimationDuration.toLong())
        }
        if (mDialogBackgroundColor!=null&& mDialogBackgroundColor>0) {
            dictionaryDialogBuilder.backgroundColor(mDialogBackgroundColor)
        }
        if (mDialogTextColor!=null&& mDialogTextColor>0) {
            dictionaryDialogBuilder.textColor(mDialogTextColor)
        }
        if (mDialogNotchColor!=null&& mDialogNotchColor>0) {
            dictionaryDialogBuilder.notchColor(mDialogNotchColor)
        }
       /* if (mDialogTextFont!=null) {
            dictionaryDialogBuilder.textFont(mDialogTextFont)
        }*/
        if (mDialogNotchHeight!=null&& mDialogNotchHeight>0) {
            dictionaryDialogBuilder.notchHeight(mDialogNotchHeight)
        }
        if (mDialogWidth!=null&& mDialogWidth>0) {
            dictionaryDialogBuilder.width = mDialogWidth.toInt()
        }
        if (mDialogHeight!=null&& mDialogHeight>0) {
            dictionaryDialogBuilder.height = mDialogHeight.toInt()
        }
        if (mDialogFocusable!=null) {
            dictionaryDialogBuilder.focusable(mDialogFocusable)
        }
        if(mDialogDismissOnInsideTouch!=null){
            dictionaryDialogBuilder.dismissOnInsideTouch(mDialogDismissOnInsideTouch)
        }
        if(mDialogDismissOnOutsideTouch!=null){
            dictionaryDialogBuilder.dismissOnOutsideTouch(mDialogDismissOnOutsideTouch)
        }

        dictionaryDialog = dictionaryDialogBuilder.build()

        dictionaryDialog?.show()
    }

}