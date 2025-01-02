package com.alexsch01.youtube

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.os.Bundle
import android.os.PowerManager
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
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: CustomWebView

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = SCREEN_ORIENTATION_PORTRAIT

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Unrestricted battery usage needs to be enabled")
            builder.setPositiveButton("OK") { _, _ ->
                finishAndRemoveTask()
            }
            val dialog = builder.create()
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }

        myWebView = findViewById(R.id.webview)

        myWebView.webViewClient = object : WebViewClient() {
            private val invalids = arrayOf(
                "support.google.com",
                "wa.me",
                "api.whatsapp.com",
                "www.facebook.com",
                "m.facebook.com",
                "twitter.com",
                "reddit.com"
            )

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                var website = request?.url.toString()
                if (!website.startsWith("https://")) {
                    return true
                }
                website = website.removePrefix("https://")

                if (website.startsWith("www.youtube.com/redirect?")) {
                    val redirectUrl = "https://" + website.split("%3A%2F%2F")[1].split("&v=")[0]
                    view?.context?.startActivity(Intent(
                        Intent.ACTION_VIEW,
                        URLDecoder.decode(redirectUrl, "UTF8").toUri()
                    ))
                    return true
                }

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
                runJavascript("""
                    document.querySelector('ad-slot-renderer')?.remove();
                    document.querySelector('ytm-companion-ad-renderer')?.remove();
                    document.querySelector('ytm-watch-metadata-app-promo-renderer')?.remove();

                    if (document.querySelector('.bottom-sheet-share-item input')) {
                        document.querySelector('.bottom-sheet-share-item input').value =
                            document.querySelector('.bottom-sheet-share-item input')?.value.split('?si=')[0];
                    }

                    if (document.querySelector('.ad-showing video')) {
                        document.querySelector('.ad-showing video').currentTime =
                            document.querySelector('.ad-showing video').duration;
                    }
                """)

                return null
            }
        }

        // Workaround for fullscreen videos
        myWebView.webChromeClient = object : WebChromeClient() {
            private var fullScreenVideoView: View? = null
            private val viewGroup =
                (findViewById<ViewGroup>(android.R.id.content)!!).getChildAt(0) as ViewGroup
            private val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (fullScreenVideoView != null) {
                    onHideCustomView()
                    return
                }
                fullScreenVideoView = view

                // get into proper fullscreen mode
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())

                requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
                viewGroup.addView(fullScreenVideoView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            }

            override fun onHideCustomView() {
                requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
                viewGroup.removeView(fullScreenVideoView)

                // get out of proper fullscreen mode
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.show(WindowInsetsCompat.Type.navigationBars())

                fullScreenVideoView = null
            }
        }

        myWebView.isVerticalScrollBarEnabled = false
        @SuppressLint("SetJavaScriptEnabled")
        myWebView.settings.javaScriptEnabled = true

        if (intent.dataString == null) {
            myWebView.loadUrl("https://m.youtube.com")
        } else {
            myWebView.loadUrl(intent.dataString!!)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.dataString != null) {
            myWebView.loadUrl(intent.dataString!!)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()) {
            myWebView.goBack()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    fun runJavascript(script: String) {
        myWebView.post {
            myWebView.evaluateJavascript(script, null)
        }
    }
}
