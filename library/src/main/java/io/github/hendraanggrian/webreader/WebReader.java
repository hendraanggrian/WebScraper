package io.github.hendraanggrian.webreader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hendra Anggrian (hendraanggrian@gmail.com)
 */
public class WebReader extends WebView {

    private static final String NAME = "HTMLOUT";
    private static final String PROCESS_URL = "javascript:window." + NAME + ".processHTML(document.getElementsByTagName('html')[0].innerHTML);";

    private List<Callback> callbacks;
    private boolean fetchLongUrl;
    private ExpandUrlTask task;

    public WebReader(Activity activity) {
        super(activity);
        init();
    }

    public WebReader(Activity activity, AttributeSet attrs) {
        super(activity, attrs);
        init();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        this.getSettings().setJavaScriptEnabled(true);
        this.callbacks = new ArrayList<>();
    }

    public WebReader addCallback(Callback callback) {
        this.callbacks.add(callback);
        return this;
    }

    public WebReader removeCallback(Callback callback) {
        this.callbacks.remove(callback);
        return this;
    }

    public WebReader removeAllCallbacks() {
        this.callbacks.clear();
        return this;
    }

    public WebReader setUserAgent(String userAgent) {
        this.getSettings().setUserAgentString(userAgent);
        return this;
    }

    public WebReader fetchLongUrl(boolean fetchLongUrl) {
        this.fetchLongUrl = fetchLongUrl;
        return this;
    }

    @Override
    public void loadUrl(String url) {
        for (Callback callback : callbacks)
            callback.onStarted(this);

        if (fetchLongUrl) {
            task = new ExpandUrlTask() {
                @Override
                protected void onPostExecute(Object o) {
                    if (o instanceof Exception) {
                        for (Callback callback : callbacks)
                            callback.onError(WebReader.this, (Exception) o);

                    } else if (o instanceof String) {
                        setWebViewClient(client);
                        addJavascriptInterface(jsInterface, NAME);
                        WebReader.super.loadUrl(o.toString());
                    }
                }
            };
            task.execute(url);

        } else {
            setWebViewClient(client);
            addJavascriptInterface(jsInterface, NAME);
            WebReader.super.loadUrl(url);
        }
    }

    public void stop() {
        task.cancel(true);
        setWebViewClient(null);
        removeJavascriptInterface(NAME);
        stopLoading();
    }

    public void retry() {
        stop();
        loadUrl(getUrl());
    }

    private WebViewClient client = new WebViewClient() {
        private boolean finished;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            finished = false;
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            if (!finished)
                for (Callback callback : callbacks)
                    callback.onProgress(WebReader.this, view.getProgress());
        }

        @Override
        public void onPageFinished(WebView view, final String url) {
            super.onPageFinished(view, url);
            finished = true;
            view.loadUrl(PROCESS_URL);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            finished = true;
            for (Callback callback : callbacks)
                callback.onError(WebReader.this, new HostUnresolvedException(getUrl()));
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            finished = true;
            for (Callback callback : callbacks)
                callback.onError(WebReader.this, new HostUnresolvedException(getUrl()));
        }
    };

    private JsInterface jsInterface = new JsInterface() {
        @Override
        @JavascriptInterface
        public void processHTML(final String html) {
            ((Activity) getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (Callback callback : callbacks)
                        callback.onSuccess(WebReader.this, html);
                }
            });
        }
    };

    public interface Callback {
        void onStarted(WebReader reader);

        void onProgress(WebReader reader, int progress);

        void onSuccess(WebReader reader, String html);

        void onError(WebReader reader, Exception exc);
    }

    public static class SimpleCallback implements Callback {
        @Override
        public void onStarted(WebReader reader) {
        }

        @Override
        public void onProgress(WebReader reader, int progress) {
        }

        @Override
        public void onSuccess(WebReader reader, String html) {
        }

        @Override
        public void onError(WebReader reader, Exception exc) {
        }
    }
}