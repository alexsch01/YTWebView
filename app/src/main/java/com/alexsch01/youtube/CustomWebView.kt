package com.alexsch01.youtube

import android.content.Context
import android.webkit.WebView
import android.util.AttributeSet

class CustomWebView(context: Context, attrs: AttributeSet) : WebView(context, attrs) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(VISIBLE)
    }
}
