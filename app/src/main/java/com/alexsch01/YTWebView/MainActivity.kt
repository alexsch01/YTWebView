package com.alexsch01.YTWebView

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: CustomWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myWebView = findViewById(R.id.webview)

        // Workaround for Google login
        myWebView.webViewClient = object : WebViewClient() {
            private val invalids = arrayOf(
                "intent://",
                "pagead2.googlesyndication.com",
                "ade.googlesyndication.com",
                "pubads.g.doubleclick.net",
                "tpc.googlesyndication.com",
                "googleads.g.doubleclick.net",
                "www.googleadservices.com",
                "ad.doubleclick.net",
                "static.doubleclick.net",

                // Not full domains
                "www.youtube.com/pagead/",
                "www.google.com/pagead/",
            )

            private val emptyResponse = WebResourceResponse("text/html", "utf-8", ByteArrayInputStream("".toByteArray()))

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val website = request?.url.toString().removePrefix("https://")

                for (invalidSite in invalids) {
                    if (website.startsWith(invalidSite)) {
                        return true
                    }
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val website = request?.url.toString().removePrefix("https://")

                for (invalidSite in invalids) {
                    if (website.startsWith(invalidSite)) {
                        return emptyResponse
                    }
                }

                return null
            }
        }

        // Workaround for fullscreen videos
        myWebView.webChromeClient = object : WebChromeClient() {
            private var fullScreenVideoView: View? = null
            private val viewGroup =
                (findViewById<ViewGroup>(android.R.id.content)!!).getChildAt(0) as ViewGroup

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (fullScreenVideoView != null) {
                    onHideCustomView()
                    return
                }
                fullScreenVideoView = view

                // Deprecated but this is the most stable way of making video proper fullscreen
                fullScreenVideoView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

                requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
                viewGroup.addView(fullScreenVideoView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            }

            override fun onHideCustomView() {
                requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED
                viewGroup.removeView(fullScreenVideoView)

                fullScreenVideoView = null
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        myWebView.settings.javaScriptEnabled = true

        myWebView.loadUrl("https://m.youtube.com")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()) {
            myWebView.goBack()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }
}
