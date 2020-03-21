package com.haikujam.clickabletextview

import android.content.Context
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.util.*

class ClickableTextView : AppCompatTextView {

    private var mDelayedSetter: Runnable? = null
    private var mConstructorCallDone: Boolean = false
    private var attrs: AttributeSet? = null
    private var defStyle: Int = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mConstructorCallDone = true
        this.attrs =attrs
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ){
        this.attrs =attrs
        this.defStyle =defStyle
    }

    override fun setText(text: CharSequence, type: BufferType) {
        if (!mConstructorCallDone) {
            // The original call needs to be made at this point otherwise an exception will be thrown in BoringLayout if text contains \n or some other characters.
            super.setText(text, type)
            // Postponing setting text via XML until the constructor has finished calling
            mDelayedSetter = Runnable {
                this@ClickableTextView.setText(text, type)
            }
            post(mDelayedSetter)
        } else {
            removeCallbacks(mDelayedSetter)
            val words = text.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val clickableWords = ArrayList<ClickableWord>()
            for (word in words) {
                clickableWords.add(
                    ClickableWord(
                        word,
                        DictionaryClickableSpan(
                            this,
                            this.context,
                            words.indexOf(word),
                            attrs,
                            defStyle
                        )
                    )
                )
            }
            movementMethod =
                CreateMovementMethod.getInstance(this@ClickableTextView.context)//LinkMovementMethod.getInstance()
            super.setText(addClickablePart(text.toString(), clickableWords), BufferType.SPANNABLE)
        }

    }

    private fun addClickablePart(
        str: String,
        clickableWords: List<ClickableWord>
    ): SpannableStringBuilder {
        val ssb = SpannableStringBuilder(str)
        var lastIndex = -1
        for (clickableWord in clickableWords) {
            val stringWithNoNewLine = str.replace("\n", " ").replace("\t", " ")
            val wordX = " " + clickableWord.word + " "
            var startIndex = (" $stringWithNoNewLine ").indexOf(wordX)
            var endIndex = 0
            if (startIndex != -1 && startIndex < lastIndex) {
                startIndex = (" $stringWithNoNewLine ").indexOf(wordX, lastIndex + 1)
            }
            if (startIndex > -1) {
                endIndex =
                    startIndex + clickableWord.word.length  //+1 because we want to remove space which we added purposely
                ssb.setSpan(clickableWord.getClickableSpan(), startIndex, endIndex, 0)
                Selection.setSelection(ssb, (startIndex + 1), endIndex)
            }
            lastIndex = startIndex

        }

        return ssb
    }

    class ClickableWord(
        /**
         * @return the word
         */
        val word: String, private val clickableSpan: DictionaryClickableSpan
    ) {

        /**
         * @return the clickableSpan
         */
        fun getClickableSpan(): ClickableSpan {
            return clickableSpan
        }
    }

}