package com.alexsch01.youtube

import android.content.Context
import android.webkit.WebView
import android.util.AttributeSet

public class CustomWebView(Context context, AttributeSet attrs) : WebView(context, attrs) {
    override protected fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(VISIBLE)
    }
}
