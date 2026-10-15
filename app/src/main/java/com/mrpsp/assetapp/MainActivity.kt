
package com.mrpsp.assetapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * MRPSP Asset App.
 * Uses the hosted website for authentication, inventory, face verification,
 * PIN fallback, QR/barcode handling, and transaction records.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView

    private val hostingUrl = "https://kolkatametroproject-web.github.io/firebase-excel/"

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadSite()
            } else {
                showCameraRequired()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(7, 59, 112)
        window.navigationBarColor = Color.rgb(7, 59, 112)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadSite()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadSite() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val header = TextView(this).apply {
            text = "MRPSP  •  ASSET CHECKOUT / RETURN"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 16, 18, 16)
            setBackgroundColor(Color.rgb(7, 59, 112))
        }

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = false
            settings.allowContentAccess = true

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return true

                    val allowedHost =
                        uri.host == "firebase-excel.web.app" ||
                        uri.host == "firebase-excel.firebaseapp.com" ||
                        uri.host == "metro-railway-project.firebaseapp.com" ||
                        uri.host?.endsWith("github.io") == true

                    return !(uri.scheme == "https" && allowedHost)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectAssetOnlyMode()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    runOnUiThread {
                        val wantsVideo = request.resources.any {
                            it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        }

                        if (
                            wantsVideo &&
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            request.grant(
                                arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                            )
                        } else {
                            request.deny()
                        }
                    }
                }
            }
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            web,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        web.loadUrl(hostingUrl)
    }

    private fun injectAssetOnlyMode() {
        val javascript = """
            (function () {
                if (window.__mrpspAssetShellInstalled) return;
                window.__mrpspAssetShellInstalled = true;

                const style = document.createElement('style');
                style.id = 'mrpsp-asset-only-style';
                style.textContent = `
                    #tab-inventory { display: block !important; }
                    #inv-add,
                    #inv-list,
                    #inv-history,
                    #inv-audit,
                    #inv-transfer,
                    #inv-recovery {
                        display: none !important;
                    }
                    button[onclick*="toggleInvMode('add')"],
                    button[onclick*="toggleInvMode('list')"],
                    button[onclick*="toggleInvMode('history')"] {
                        display: none !important;
                    }
                    [id^="tab-"]:not(#tab-inventory) {
                        display: none !important;
                    }
                    .asset-app-hidden {
                        display: none !important;
                    }
                `;

                document.head.appendChild(style);

                function applyAssetOnlyMode() {
                    document.querySelectorAll('[id^="tab-"]').forEach(function (element) {
                        if (element.id !== 'tab-inventory') {
                            element.classList.add('asset-app-hidden');
                        }
                    });

                    document.querySelectorAll('[onclick]').forEach(function (element) {
                        const action = (
                            element.getAttribute('onclick') || ''
                        ).toLowerCase();

                        const isNavigation =
                            action.includes('switchtab') ||
                            action.includes('showtab') ||
                            action.includes('navigateto');

                        if (isNavigation && !action.includes('inventory')) {
                            element.classList.add('asset-app-hidden');
                        }

                        if (
                            action.includes("toggleinvmode('add'") ||
                            action.includes("toggleinvmode('list'") ||
                            action.includes("toggleinvmode('history'")
                        ) {
                            element.classList.add('asset-app-hidden');
                        }
                    });

                    const login = document.getElementById('loginSection');
                    const inventory = document.getElementById('tab-inventory');

                    if (
                        login &&
                        getComputedStyle(login).display === 'none' &&
                        inventory &&
                        typeof window.toggleInvMode === 'function'
                    ) {
                        inventory.classList.remove('hidden');

                        const checkout = document.getElementById('inv-checkout');
                        const returnPanel = document.getElementById('inv-return');

                        if (
                            checkout &&
                            returnPanel &&
                            checkout.classList.contains('hidden') &&
                            returnPanel.classList.contains('hidden')
                        ) {
                            window.toggleInvMode('checkout');
                        }
                    }
                }

                applyAssetOnlyMode();

                new MutationObserver(applyAssetOnlyMode).observe(
                    document.documentElement,
                    {
                        subtree: true,
                        childList: true,
                        attributes: true,
                        attributeFilter: ['class', 'style']
                    }
                );
            })();
        """.trimIndent()

        web.evaluateJavascript(javascript, null)
    }

    private fun showCameraRequired() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Camera required")
            .setMessage(
                "MRPSP Asset App needs camera access for face verification " +
                "and QR/barcode scanning. Grant camera permission to continue."
            )
            .setCancelable(false)
            .setPositiveButton("Try again") { _, _ ->
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .show()
    }

    @Deprecated("Deprecated by Android; retained for compatibility")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
