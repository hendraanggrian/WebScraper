package io.github.hendraanggrian.websnatch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.URL;
import java.net.URLConnection;

/**
 * @author Hendra Anggrian (hendraanggrian@gmail.com)
 */
public class WebSnatch extends WebView {

    private final String NAME = "HTMLOUT";
    private final String PROCESS_URL = "javascript:window.HTMLOUT.processHTML(document.getElementsByTagName('html')[0].innerHTML);";

    private AsyncTask<String, Void, String> loadTask;
    private int timeout = -1;

    public WebSnatch(Context context) {
        super(context);
    }

    public WebSnatch setUserAgent(String userAgent) {
        getSettings().setUserAgentString(userAgent);
        return this;
    }

    public WebSnatch setTimeout(final int timeout) {
        this.timeout = timeout;
        return this;
    }

    public void load(String url, final WebSnatchCompletion completion) {
        loadTask = new AsyncTask<String, Void, String>() {
            Exception exception;

            @Override
            protected void onPreExecute() {
                completion.onStarted(WebSnatch.this);
            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    URL url = new URL(params[0]);
                    URLConnection conn = url.openConnection();
                    conn.getHeaderFields();
                    String longUrl = conn.getURL().toString();

                    if (!isCancelled())
                        return longUrl;
                    else
                        return "";
                } catch (Exception exc) {
                    exception = exc;
                    return "";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result.isEmpty()) {
                    if (exception != null)
                        completion.onError(WebSnatch.this, exception);
                    return;
                }

                WebViewClient webViewClient = new WebViewClient() {
                    @Override
                    public void onPageStarted(final WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                        if (timeout != -1)
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    view.stopLoading();
                                    completion.onError(WebSnatch.this, new TimeoutException(timeout));
                                }
                            }, timeout);
                    }

                    @Override
                    public void onLoadResource(WebView view, String url) {
                        super.onLoadResource(view, url);
                        completion.onProgress(WebSnatch.this, view.getProgress());
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        view.loadUrl(PROCESS_URL);
                    }
                };
                SnatchInterface snatchInterface = new SnatchInterface() {
                    @Override
                    @JavascriptInterface
                    public void processHTML(final String html) {
                        ((Activity) getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                completion.onSuccess(WebSnatch.this, html);
                            }
                        });
                    }
                };

                getSettings().setJavaScriptEnabled(true);
                addJavascriptInterface(snatchInterface, NAME);
                setWebViewClient(webViewClient);
                loadUrl(result);
            }
        }.execute(url);
    }

    public void stop() {
        loadTask.cancel(true);
        getSettings().setJavaScriptEnabled(false);
        setWebViewClient(null);
        stopLoading();
    }

    public interface SnatchInterface {
        @JavascriptInterface
        void processHTML(String html);
    }
}