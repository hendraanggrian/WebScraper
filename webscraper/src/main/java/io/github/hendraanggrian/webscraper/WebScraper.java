package io.github.hendraanggrian.webscraper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * @author Hendra Anggrian (hendraanggrian@gmail.com)
 */
public class WebScraper extends WebScraperBase {

    private OnTimeoutListener timeoutListener;

    public WebScraper(Context context) {
        super(context);
    }

    public WebScraper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebScraper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebScraper(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public WebScraper(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
    }

    public WebScraper setUserAgent(String userAgent) {
        this.getSettings().setUserAgentString(userAgent);
        return this;
    }

    public WebScraper setTimeout(int timeout, OnTimeoutListener listener) {
        this.timeoutListener = listener;
        this.timeoutListener.setTimeout(timeout);
        return this;
    }

    public WebScraper clearTimeout() {
        this.timeoutListener = null;
        return this;
    }

    public void loadUrl(String url, final Callback callback) {
        if (!url.equals(PROCESS_URL)) {
            callback.onStarted(this);
            addJavascriptInterface(new JavascriptProcessor() {
                @Override
                @JavascriptInterface
                public void processHTML(final String html) {
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(WebScraper.this, html);
                        }
                    });
                }
            }, NAME);
            setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    callback.onRequest(WebScraper.this, url);
                    return super.shouldInterceptRequest(view, url);
                }

                @Override
                public void onPageFinished(WebView view, final String url) {
                    super.onPageFinished(view, url);
                    view.loadUrl(PROCESS_URL);
                }
            });
            setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    callback.onProgress(WebScraper.this, newProgress);
                }
            });

            if (timeoutListener != null)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stop();
                        timeoutListener.onTimeout(WebScraper.this);
                    }
                }, timeoutListener.getTimeout());
        }
        super.loadUrl(url);
    }

    public void stop() {
        removeJavascriptInterface(NAME);
        setWebViewClient(new WebViewClient());
        setWebChromeClient(new WebChromeClient());

        stopLoading();
        clearHistory();
        clearCache(true);
    }

    public void retry() {
        stop();
        loadUrl(getOriginalUrl());
    }

    public static abstract class OnTimeoutListener {
        public abstract void onTimeout(WebScraper webScraper);

        private int timeout;

        protected void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        protected int getTimeout() {
            return timeout;
        }
    }

    public interface Callback {
        void onStarted(WebScraper scraper);

        void onProgress(WebScraper scraper, int progress);

        void onRequest(WebScraper scraper, String url);

        void onSuccess(WebScraper scraper, String html);
    }

    public static class SimpleCallback implements Callback {

        @Override
        public void onStarted(WebScraper scraper) {

        }

        @Override
        public void onProgress(WebScraper scraper, int progress) {

        }

        @Override
        public void onRequest(WebScraper scraper, String url) {

        }

        @Override
        public void onSuccess(WebScraper scraper, String html) {

        }
    }
}