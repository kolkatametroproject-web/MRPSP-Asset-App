package com.mrpsp.assetapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Thin Android client for the existing MRPSP website. The website remains the source of truth
 * for Firebase Auth, project-aware inventory paths, face-api.js matching, PIN fallback, QR/barcode
 * handling, and transaction logs. This shell restricts the exposed UI to checkout/return.
 */
class MainActivity : Activity() {
    private lateinit var web: WebView
    private var pendingWebPermission: PermissionRequest? = null
    private val hostingUrl = "https://firebase-excel.web.app/" // Change if your Hosting URL differs.
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadSite() else showCameraRequired()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 59, 112)
        window.navigationBarColor = Color.rgb(7, 59, 112)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return true
                    return !(uri.scheme == "https" && (uri.host == "firebase-excel.web.app" || uri.host == "firebase-excel.firebaseapp.com" || uri.host == "metro-railway-project.firebaseapp.com" || uri.host?.endsWith("github.io") == true))
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectAssetOnlyMode()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    runOnUiThread {
                        val wantsVideo = request.resources.any { it == PermissionRequest.RESOURCE_VIDEO_CAPTURE }
                        if (wantsVideo && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            pendingWebPermission = request
                            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                        } else request.deny()
                    }
                }
            }
        }
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        web.loadUrl(hostingUrl)
    }

    private fun injectAssetOnlyMode() {
        // Keep login, project chooser, and inventory view. Hide all other modules and stock-admin actions.
        val js = """
        (function(){
          if (window.__mrpspAssetShellInstalled) return;
          window.__mrpspAssetShellInstalled = true;
          const style = document.createElement('style');
          style.id='mrpsp-asset-only-style';
          style.textContent = `
            #tab-inventory { display:block !important; }
            #inv-add, #inv-list, #inv-history, #inv-audit, #inv-transfer, #inv-recovery { display:none !important; }
            button[onclick*="toggleInvMode('add')"], button[onclick*="toggleInvMode('list')"], button[onclick*="toggleInvMode('history')"] { display:none !important; }
            [id^="tab-"]:not(#tab-inventory) { display:none !important; }
            .asset-app-hidden { display:none !important; }
          `;
          document.head.appendChild(style);
          function apply(){
            document.querySelectorAll('[id^="tab-"]').forEach(el=>{ if(el.id!=='tab-inventory') el.classList.add('asset-app-hidden'); });
            // Hide navigation entries to other website modules without disturbing login/project selection.
            document.querySelectorAll('[onclick]').forEach(el=>{
              const a=(el.getAttribute('onclick')||'').toLowerCase();
              if((a.includes('switchtab') || a.includes('showtab') || a.includes('navigateto')) && !a.includes('inventory')) el.classList.add('asset-app-hidden');
              if(a.includes("toggleinvmode('add'") || a.includes("toggleinvmode('list'") || a.includes("toggleinvmode('history'")) el.classList.add('asset-app-hidden');
            });
            const login=document.getElementById('loginSection');
            const inventory=document.getElementById('tab-inventory');
            if(login && getComputedStyle(login).display==='none' && inventory && window.toggleInvMode){
              inventory.classList.remove('hidden');
              const checkout=document.getElementById('inv-checkout');
              const ret=document.getElementById('inv-return');
              if(checkout && ret && checkout.classList.contains('hidden') && ret.classList.contains('hidden')) window.toggleInvMode('checkout');
            }
          }
          apply();
          new MutationObserver(apply).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style']});
          setInterval(apply,1500);
        })();
        """.trimIndent()
        web.evaluateJavascript(js, null)
    }

    private fun showCameraRequired() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Camera required")
            .setMessage("MRPSP Asset App needs camera access for face verification and QR/barcode scanning. Grant camera permission to continue.")
            .setCancelable(false)
            .setPositiveButton("Try again") { _, _ -> cameraPermission.launch(Manifest.permission.CAMERA) }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .show()
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
