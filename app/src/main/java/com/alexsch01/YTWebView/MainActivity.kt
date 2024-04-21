package com.alexsch01.YTWebView

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: CustomWebView

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
