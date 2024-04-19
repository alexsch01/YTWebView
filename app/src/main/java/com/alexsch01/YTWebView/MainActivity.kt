package com.alexsch01.YTWebView

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myWebView: MyWebView = findViewById(R.id.webview)
        val webSettings = myWebView.settings

        @SuppressLint("SetJavaScriptEnabled")
        webSettings.javaScriptEnabled = true

        // Enable private browsing mode
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.domStorageEnabled = false
        webSettings.databaseEnabled = false

        myWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        myWebView.loadUrl("https://www.youtube.com")
    }
}