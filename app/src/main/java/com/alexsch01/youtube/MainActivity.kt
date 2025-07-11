package com.alexsch01.youtube

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView
    private lateinit var intentForegroundService: Intent
    private var customViewActive = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intentForegroundService = Intent(this, ForegroundService::class.java)
        startService(intentForegroundService)

        myWebView = findViewById(R.id.webView)
        myWebView.overScrollMode = WebView.OVER_SCROLL_NEVER
        myWebView.isVerticalScrollBarEnabled = false
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
        myWebView.settings.mediaPlaybackRequiresUserGesture = false

        myWebView.webViewClient = object : WebViewClient() {
            private val validSites = arrayOf(
                "accounts.google.com",
                "accounts.youtube.com",
                "myaccount.google.com/accounts/SetOSID",
                "gds.google.com/web/landing",

                // Open by default -- supported links
                "youtu.be",
                "m.youtube.com",
                "youtube.com",
                "www.youtube.com"
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

                for (validSite in validSites) {
                    if (website.startsWith(validSite)) {
                        return false
                    }
                }

                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                myWebView.post {
                    /*
                        USE
                            if (Element) { Element.hidden = true; }
                        OVER
                            Element?.remove();
                    */
                    myWebView.evaluateJavascript("""
                        if (document.querySelector('ad-slot-renderer')) {
                            document.querySelector('ad-slot-renderer').hidden = true;
                        }

                        if (document.querySelector('ytm-companion-ad-renderer')) {
                            document.querySelector('ytm-companion-ad-renderer').hidden = true;
                        }
    
                        if (document.querySelector('.bottom-sheet-share-item input')) {
                            document.querySelector('.bottom-sheet-share-item input').value =
                                document.querySelector('.bottom-sheet-share-item input').value.split('?si=')[0];
                        }
    
                        if (document.querySelector('.ad-showing video') && !isNaN(document.querySelector('.ad-showing video').duration)) {
                            document.querySelector('.ad-showing video').currentTime =
                                document.querySelector('.ad-showing video').duration;
                        }

                        document.querySelectorAll(`ytm-video-with-context-renderer:has(
                            img[src="https://www.gstatic.com/youtube/img/tvfilm/clapperboard_profile_v2.png"],
                            img[src="https://yt3.ggpht.com/FnWK1Inih45sn80XTnZdTOQ2X0QyzsVXMxHQJ6fn3ST6b83-YR1bRa6duTFdL7nXAktvYEcmimQ=s68-c-k-c0x00ffffff-no-rj-mo"]
                        )`).forEach(elem => {
                            elem.hidden = true;
                        });
                    """, null)
                }

                return null
            }
        }

        // Workaround for fullscreen videos
        myWebView.webChromeClient = object : WebChromeClient() {
            private val frameLayout = findViewById<CustomFrameLayout>(R.id.customFrameLayout)
            private val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                // get into proper fullscreen mode
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())

                frameLayout.addView(view, 1)
                customViewActive = true
                view?.postDelayed({
                    // need a delay when going to landscape mode to prevent video glitch
                    requestedOrientation = SCREEN_ORIENTATION_USER_LANDSCAPE
                }, 100)
            }

            override fun onHideCustomView() {
                requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED
                customViewActive = false
                frameLayout.removeViewAt(1)

                // get out of proper fullscreen mode
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        if (intent.dataString == null) {
            myWebView.loadUrl("https://m.youtube.com")
        } else {
            myWebView.loadUrl(intent.dataString!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(intentForegroundService)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.dataString != null) {
            myWebView.loadUrl(intent.dataString!!)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (customViewActive) {
                myWebView.evaluateJavascript("document.querySelector('.fullscreen-icon').click()", null)
                return true
            } else if (myWebView.canGoBack()) {
                myWebView.goBack()
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }
}
