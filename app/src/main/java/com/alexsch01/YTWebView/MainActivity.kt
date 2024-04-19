package com.alexsch01.YTWebView

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myWebView = findViewById(R.id.webview)

        // clear data
        myWebView.clearFormData()
        myWebView.clearHistory()
        myWebView.clearSslPreferences()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()

        @SuppressLint("SetJavaScriptEnabled")
        myWebView.settings.javaScriptEnabled = true

        myWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        myWebView.loadUrl("https://www.youtube.com")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()) {
            myWebView.goBack()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }
}
