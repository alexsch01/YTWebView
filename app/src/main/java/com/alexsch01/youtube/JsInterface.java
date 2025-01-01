package com.alexsch01.youtube;

import android.webkit.JavascriptInterface;
import java.util.concurrent.Semaphore;

public class JsInterface {
    private String value;
    private final Semaphore semaphore;

    public JsInterface(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public String getValue() {
        return value;
    }

    @JavascriptInterface
    public void setValue(String value) {
        this.value = value;

        // important release the semaphore after the execution
        semaphore.release();
    }
}
