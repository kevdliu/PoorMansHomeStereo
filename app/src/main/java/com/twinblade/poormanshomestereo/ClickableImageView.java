package com.twinblade.poormanshomestereo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ClickableImageView extends ImageView {

    private ColorStateList mTint;

    public ClickableImageView(Context context) {
        super(context);
    }

    public ClickableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ClickableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray typedArr = context.obtainStyledAttributes(attrs, R.styleable.ClickableImageView, defStyle, 0);
        mTint = typedArr.getColorStateList(R.styleable.ClickableImageView_click_tint);
        typedArr.recycle();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mTint != null && mTint.isStateful()) {
            setColorFilter(mTint.getColorForState(getDrawableState(), 0));
        }
    }
}
