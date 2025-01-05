package com.alexsch01.youtube

import android.content.Context
import android.webkit.WebView
import android.util.AttributeSet

class CustomWebView(Context context, AttributeSet attrs) : WebView(context, attrs) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(VISIBLE)
    }
}
