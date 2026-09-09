/*
   THE FORGE  |  Copyright (c) 2026 Yannie D. Forest. All rights reserved.
   NO LICENCE IS GRANTED. No copying, adaptation, redistribution, reverse
   engineering or commercial use without the author's written permission.
   See LICENCE.txt. Every distributed copy carries a build marker.
*/
package com.yannie.carpediem;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.graphics.Color;
import java.io.OutputStream;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class MainActivity extends Activity {
    private WebView web;
    // The file chooser bridge. Android WebViews do NOT open <input type=file>
    // on their own: without onShowFileChooser the Import buttons silently do
    // nothing, which is exactly what happened on the tablet.
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 4711;

    // The SAVE bridge. A blob download in a WebView is opened as a page, which
    // showed the raw backup text and lost the app. Export instead calls
    // window.CarpeNative.saveFile, which opens the system Save dialog here and
    // writes the chosen file itself. Nothing leaves the device; there is still no
    // INTERNET permission, and the Save dialog needs no storage permission.
    private static final int SAVE_FILE_REQUEST = 4712;
    private String pendingSaveB64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        // A floating window is an iframe pointing at this same file, so the
        // WebView must let a file:// page load a file:// subframe. Without
        // these the floating windows stay blank on some WebView versions.
        // The app requests no INTERNET permission, so nothing remote can load.
        try {
            s.setAllowFileAccessFromFileURLs(true);
            s.setAllowUniversalAccessFromFileURLs(true);
        } catch (Throwable ignored) {
        }
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setBackgroundColor(Color.parseColor("#1B6B93"));
        // The app loads only its own HTML from assets, with no network permission,
        // so the only JavaScript that can reach this interface is The Forge's own.
        web.addJavascriptInterface(new CarpeBridge(), "CarpeNative");
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override public void run() { request.grant(request.getResources()); }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });
        // The app is served over https from its own assets rather than opened
        // as a file. A file:// page CANNOT put another file:// document in an
        // iframe, which is why the floating windows were empty on the tablet and
        // had to fall back to carrying a studio alone. Served this way the origin
        // is ordinary, the iframe is allowed, and a floating window carries the
        // WHOLE application exactly as the desktop one does.
        // Nothing leaves the device: the handler only ever reads app/src/main/assets,
        // and the app still requests no INTERNET permission.
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        web.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
        });
        web.loadUrl("https://appassets.androidplatform.net/assets/the_forge.html");
        setContentView(web);
    }

    // Exposed to the page as window.CarpeNative. The page calls saveFile when the
    // person exports a backup; it hands over the bytes as base64 and this opens the
    // system Save dialog. The file is written in onActivityResult once a place is
    // chosen. Returns true when the dialog is being opened, so the page knows the
    // native path was taken; false lets the page fall back to Copy.
    public class CarpeBridge {
        @JavascriptInterface
        public boolean saveFile(final String name, final String mime, final String b64) {
            try {
                pendingSaveB64 = b64;
                final String type = (mime == null || mime.length() == 0) ? "application/octet-stream" : mime;
                final String fname = (name == null || name.length() == 0) ? "the-forge-backup.json" : name;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        try {
                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType(type);
                            intent.putExtra(Intent.EXTRA_TITLE, fname);
                            startActivityForResult(intent, SAVE_FILE_REQUEST);
                        } catch (Exception e) {
                            pendingSaveB64 = null;
                        }
                    }
                });
                return true;
            } catch (Exception e) {
                pendingSaveB64 = null;
                return false;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCallback = null;
            }
            return;
        }
        if (requestCode == SAVE_FILE_REQUEST) {
            OutputStream os = null;
            try {
                if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingSaveB64 != null) {
                    byte[] bytes = Base64.decode(pendingSaveB64, Base64.DEFAULT);
                    os = getContentResolver().openOutputStream(data.getData());
                    if (os != null) { os.write(bytes); os.flush(); }
                }
            } catch (Exception e) {
                // best effort: a failed write leaves the app untouched, and the
                // person can still use Copy to move the backup instead.
            } finally {
                try { if (os != null) os.close(); } catch (Exception e) {}
                pendingSaveB64 = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
