package com.mysay.blueparrottsdkaudiodemo;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Workaround for Android restriction on using a Button onTouchListener without a performClick for accessibility
 */

public class TalkButton extends androidx.appcompat.widget.AppCompatButton {

    public TalkButton(Context context) {
        super(context);

    }

    public TalkButton(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public TalkButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    /*
    Required to do,  this implement performCLick() to avoid accessibility warning for a button that just uses the onTouchListener
     */
    @Override
    public boolean performClick() {
        // Calls the super implementation, which generates an AccessibilityEvent and calls the onClick() listener on the view, if any
        super.performClick();
        return true;
    }

}
