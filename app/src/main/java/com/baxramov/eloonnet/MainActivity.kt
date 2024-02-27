package com.baxramov.eloonnet

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val filesUploader: FilesUploader by lazy { FilesUploader(this) }

    private lateinit var webView: WebView
    private lateinit var noInternetLayout: LinearLayout
    private var isConnected = false

    private val networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isConnected = isNetworkConnected(context)
            if (isConnected) {
                webView.visibility = View.VISIBLE
                noInternetLayout.visibility = View.GONE
                webView.loadUrl(URL)
            } else {
                webView.visibility = View.GONE
                noInternetLayout.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkChangeReceiver)
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Eloonnet)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        askNotificationPermission()
        val actionBar = supportActionBar
        actionBar?.hide()
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.web_view)
        webView.isClickable = true

        noInternetLayout = findViewById(R.id.no_internet_layout)

        registerReceiver(
            networkChangeReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        webView.visibility = View.GONE  // Hide WebView initially

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                webView: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                Log.d("error", "Oh no! $description")
                showErrorDialog()
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.d("error", "Oh no! $errorResponse")
                showErrorDialog()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d(LINK, "$url")
                view?.loadUrl(url!!)
                return true
            }

            // save cookies
            override fun onPageFinished(webView: WebView, url: String) {
                CookieManager.getInstance().flush()
                CookieSyncManager.getInstance().startSync()
                // Show WebView after page has finished loading
                webView.visibility = View.VISIBLE
                noInternetLayout.visibility = View.GONE
            }
        }
        webView.loadUrl(URL)

        // Additional error handling in case of connection issues
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return filesUploader
                    .onShowFileChooser(
                        webView,
                        filePathCallback,
                        fileChooserParams
                    )
            }
        }

        webSettings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            domStorageEnabled = true
            saveFormData = true
            allowContentAccess = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            builtInZoomControls = true;
            setSupportMultipleWindows(true);
            setSupportZoom(true)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
        webView.restoreState(savedInstanceState!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        filesUploader.onActivityResult(requestCode, resultCode, data)
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
            Toast.makeText(this, "Приложения не будет показать уведомления", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showAlertDialog(
                    "Показывать уведомления",
                    "Разрешить этому приложению показать уведомления",
                    "Разрешить"
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showErrorDialog() {
        Toast.makeText(
            this@MainActivity,
            "Произошла ошибка, пожалуйста перезапустите приложение",
            Toast.LENGTH_SHORT
        ).show()
    }


    private fun showAlertDialog(
        dialogTitle: String, dialogMessage: String, buttonTitle: String, action: () -> Unit
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)
        builder.setMessage(dialogMessage)

        builder.setPositiveButton(buttonTitle) { dialog, which ->
            action()
        }

        builder.setNeutralButton(getString(R.string.no_thanks)) { dialog, which ->
            Toast.makeText(
                this,
                getString(R.string.permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }

        builder.show()
    }

    companion object {
        private const val URL = "https://eloon.net"
        private const val ERROR = "ERROR"
        private const val LINK = "My_link_to_open"
    }
}