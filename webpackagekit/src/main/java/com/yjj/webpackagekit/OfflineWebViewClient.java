package com.yjj.webpackagekit;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.yjj.webpackagekit.core.util.Logger;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * created by yangjianjun on 2018/9/26
 * 离线包加载入口
 */
public class OfflineWebViewClient extends WebViewClient {
    private final WebViewClient delegate;

    public OfflineWebViewClient(WebViewClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        Logger.d("shouldInterceptRequest before: " + url);
        WebResourceResponse resourceResponse = getWebResourceResponse(url);
        if (resourceResponse == null) {
            if (delegate != null) {
                return delegate.shouldInterceptRequest(view, url);
            }
            return super.shouldInterceptRequest(view, url);
        }
        Logger.d("shouldInterceptRequest after cache: " + url);
        return resourceResponse;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        final String url = request.getUrl().toString();
        Logger.d("shouldInterceptRequest before: " + url);
        WebResourceResponse resourceResponse = getWebResourceResponse(url);
        if (resourceResponse == null) {
            if (delegate != null) {
                return delegate.shouldInterceptRequest(view, url);
            }
            return super.shouldInterceptRequest(view, url);
        }
        Logger.d("shouldInterceptRequest after cache: " + url);
        return resourceResponse;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (delegate != null) {
            return delegate.shouldOverrideUrlLoading(view, url);
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    /**
     * 获取资源
     *
     * @param url 资源地址
     */
    private WebResourceResponse getWebResourceResponse(String url) {
        WebResourceResponse resourceResponse = PackageManager.getInstance().getResource(url);
        return resourceResponse;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (delegate != null) {
            delegate.onPageStarted(view, url, favicon);
            return;
        }
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (delegate != null) {
            delegate.onPageFinished(view, url);
            return;
        }
        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (delegate != null) {
            delegate.onReceivedError(view, errorCode, description, failingUrl);
            return;
        }
        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        if (delegate != null) {
            delegate.onReceivedSslError(view, handler, error);
            return;
        }
        super.onReceivedSslError(view, handler, error);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (delegate != null) {
            delegate.onReceivedError(view, request, error);
            return;
        }
        super.onReceivedError(view, request, error);
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        if (delegate != null) {
            delegate.onLoadResource(view, url);
            return;
        }
        super.onLoadResource(view, url);
    }

    @Override
    public void onReceivedLoginRequest(WebView view, String realm, @Nullable String account, String args) {
        if (delegate != null) {
            delegate.onReceivedLoginRequest(view, realm, account, args);
            return;
        }
        super.onReceivedLoginRequest(view, realm, account, args);
    }

}

