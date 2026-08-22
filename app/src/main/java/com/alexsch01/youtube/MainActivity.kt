package com.alexsch01.youtube

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView
    private lateinit var intentForegroundService: Intent
    private var customViewActive = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        intentForegroundService = Intent(this, ForegroundService::class.java)
        startService(intentForegroundService)

        myWebView = findViewById(R.id.webView)
        myWebView.overScrollMode = WebView.OVER_SCROLL_NEVER
        myWebView.isVerticalScrollBarEnabled = false
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true

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
                    (function() {
                        const adSlot = document.querySelector('ad-slot-renderer');
                        if (adSlot) {
                            adSlot.hidden = true;
                        }

                        const companionAd = document.querySelector('ytm-companion-ad-renderer');
                        if (companionAd) {
                            companionAd.hidden = true;
                        }

                        const watchCard = document.querySelector('ytm-universal-watch-card-renderer');
                        if (watchCard) {
                            watchCard.hidden = true;
                        }
    
                        const shareUrlInput = document.querySelector('.unified-share-url-input');
                        if (shareUrlInput) {
                            shareUrlInput.value = shareUrlInput.value.split('?si=')[0];
                        }
    
                        const adShowingVideo = document.querySelector('.ad-showing video');
                        if (adShowingVideo && !isNaN(adShowingVideo.duration)) {
                            adShowingVideo.currentTime = adShowingVideo.duration;
                        }

                        const adSkipButton = document.querySelector('.ytp-ad-skip-button-modern');
                        if (adSkipButton && adSkipButton.checkVisibility()) {
                            adSkipButton.click();
                        }

                        document.querySelectorAll(`ytm-video-with-context-renderer:has(
                            :is(
                                badge-shape[aria-label="Purchased"],
                                badge-shape[aria-label="Preview only"],
                                badge-shape[aria-label="Try now"],
                                badge-shape[aria-label="Free with ads"]
                            )
                        )`).forEach(elem => {
                            elem.hidden = true;
                        });
                    })()
                    """.trimIndent(), null)
                }

                val urlString = request?.url.toString()
                val regex = """^https://m\.youtube\.com/s/player/[a-zA-Z0-9]+/.*ad\.js$""".toRegex()

                if (urlString.matches(regex)) {
                    try {
                        val connection = URL(urlString).openConnection() as HttpURLConnection

                        // Read response stream
                        val inputStream = connection.inputStream
                        val originalText = inputStream.bufferedReader().use { it.readText() }

                        // Perform the literal string replacement
                        val modifiedText = originalText.replace(".isTrusted", "||true")
                        val modifiedStream = modifiedText.byteInputStream(Charsets.UTF_8)

                        return WebResourceResponse("application/javascript", "UTF-8", modifiedStream)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                return null
            }
        }

        // Workaround for fullscreen videos
        myWebView.webChromeClient = object : WebChromeClient() {
            private val frameLayout: CustomFrameLayout = findViewById(R.id.customFrameLayout)
            private val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                // get into proper fullscreen mode
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())

                frameLayout.addView(view, 1)
                customViewActive = true
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }

            override fun onHideCustomView() {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                customViewActive = false
                frameLayout.removeViewAt(1)

                // get out of proper fullscreen mode
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        val myIntent = intent.dataString
        if (myIntent == null) {
            myWebView.loadUrl("https://m.youtube.com")
        } else {
            myWebView.loadUrl(myIntent)
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        customViewActive -> {
                            myWebView.evaluateJavascript(
                                "document.querySelector('.fullscreen-icon').click()",
                                null
                            )
                        }
                        myWebView.canGoBack() -> myWebView.goBack()
                        else -> finish()
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(intentForegroundService)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val myIntent = intent.dataString
        if (myIntent != null) {
            myWebView.loadUrl(myIntent)
        }
    }
}
