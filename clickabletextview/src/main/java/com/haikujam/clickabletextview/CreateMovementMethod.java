package com.haikujam.clickabletextview;

import android.content.Context;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

public class CreateMovementMethod extends LinkMovementMethod {
    static CreateMovementMethod sInstance;
    static Context mContext;
    private Spannable buffer;
    private TextView widget;

    public static CreateMovementMethod getInstance(Context context) {
        mContext = context;
        if (sInstance == null)
            sInstance = new CreateMovementMethod();

        return sInstance;
    }

    private final GestureDetector detector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);
            if (!widget.hasOnClickListeners() && !widget.getLinksClickable()) {
                // Avoid all touches
                return false;
            }

            // Handle avoiding touch events, if not on a clickable span
            if (!widget.hasOnClickListeners()) {
                if (widget.getLayout()!= null) {
                    int lineX = widget.getLayout().getLineForVertical(((int) (event.getY())));
                    int posY = widget.getLayout().getLineBottom(lineX);
                    float posX = widget.getLayout().getLineRight(lineX);
                    boolean isTouchWithinBound = event.getY() < posY && event.getX() < posX;
                    if(isTouchWithinBound){
                        ClickableSpan[] links = buffer.getSpans(off, off, DictionaryClickableSpan.class);

                        if (links.length != 0) {
                            ClickableSpan link = links[0];
                            link.onClick(widget);
                            return true;
                        } else {
                            return false;
                        }
                    }else{
                        return false;
                    }
                }
            }
            return false;

        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

    });

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        this.buffer = buffer;
        this.widget = widget;
        return detector.onTouchEvent(event);
    }

}
