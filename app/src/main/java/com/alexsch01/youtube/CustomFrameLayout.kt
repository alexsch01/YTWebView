package com.alexsch01.youtube

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class CustomFrameLayout(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        super.dispatchWindowVisibilityChanged(VISIBLE)
    }
}
