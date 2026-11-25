package com.example.browser

import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

interface WebChromeCallbacks {
    fun onProgressChanged(tabId: String, progress: Int)
    fun onTitleReceived(tabId: String, title: String)
    fun onFileChooserRequested(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams?
    )
    fun onSitePermissionRequested(tabId: String, request: PermissionRequest)
    fun onGeolocationRequested(tabId: String, origin: String, callback: GeolocationPermissions.Callback)
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideCustomView()
    fun onPopupRequested(sourceTabId: String, resultMsg: Message)
}

class DskWebChromeClient(
    private val tabId: String,
    private val callbacks: WebChromeCallbacks
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        callbacks.onProgressChanged(tabId, newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        if (!title.isNullOrBlank()) {
            callbacks.onTitleReceived(tabId, title)
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback == null) return false
        callbacks.onFileChooserRequested(filePathCallback, fileChooserParams)
        return true
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        callbacks.onSitePermissionRequested(tabId, request)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        if (origin != null && callback != null) {
            callbacks.onGeolocationRequested(tabId, origin, callback)
        }
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null) {
            callbacks.onShowCustomView(view, callback)
        }
    }

    override fun onHideCustomView() {
        callbacks.onHideCustomView()
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (resultMsg != null) {
            callbacks.onPopupRequested(tabId, resultMsg)
            return true
        }
        return false
    }
}
