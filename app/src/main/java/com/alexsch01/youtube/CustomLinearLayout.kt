package com.alexsch01.youtube

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class CustomLinearLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        super.dispatchWindowVisibilityChanged(VISIBLE)
    }
}
