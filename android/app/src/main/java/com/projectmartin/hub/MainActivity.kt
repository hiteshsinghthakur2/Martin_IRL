package com.projectmartin.hub

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // Production Web App URL (fallback to local asset when offline)
    private val liveProductionUrl = "https://martin2626.vercel.app/"
    private val localAssetUrl = "file:///android_asset/index.html"

    // Runtime permissions launcher for Camera, Mic & Modern Bluetooth
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted && micGranted) {
            Toast.makeText(this, "Hardware permissions granted for Martin Hub", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set modern dark status bar matching the Martin Hub theme
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.black)

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.martinWebView)

        // Request runtime permissions on launch
        checkAndRequestAppPermissions()

        // Configure high-performance full-screen WebView
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.mediaPlaybackRequiresUserGesture = false

        // Mixed Content allowed for local HTTP ESP32 endpoints & Cloud SSL
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Custom WebChromeClient: Automatically grant Camera, Mic & Audio to MediaPipe / Web Speech
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.let {
                    runOnUiThread {
                        // Grant all requested web permissions (CAMERA, AUDIO_CAPTURE, PROTECTED_MEDIA)
                        it.grant(it.resources)
                    }
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    android.util.Log.d("MartinHubWebView", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                }
                return true
            }
        }

        // Custom WebViewClient: Handles loading, offline detection, and internal routing
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // Keep all app URLs inside the WebView
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
                    return false
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // If live URL fails to load due to connection drop, fallback to bundled offline asset
                if (request?.isForMainFrame == true && view?.url != localAssetUrl) {
                    android.util.Log.w("MartinHub", "Network error detected. Falling back to bundled offline asset.")
                    view?.loadUrl(localAssetUrl)
                    Toast.makeText(this@MainActivity, "Offline Mode: Running local Martin Hub with BLE Fallback", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Load Live Production URL if connected, else fallback to local bundled asset
        loadTargetUrl()
    }

    private fun loadTargetUrl() {
        if (isInternetAvailable()) {
            webView.loadUrl(liveProductionUrl)
        } else {
            Toast.makeText(this, "No internet detected. Loading offline Hub...", Toast.LENGTH_SHORT).show()
            webView.loadUrl(localAssetUrl)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun checkAndRequestAppPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        // Android 12+ (API 31+) Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val ungrantedPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(ungrantedPermissions.toTypedArray())
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
