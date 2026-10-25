package app.vyrlume.mobile.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.vyrlume.mobile.BuildConfig
import app.vyrlume.mobile.data.IdeSection
import app.vyrlume.mobile.data.ServerConnection

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RemoteWorkbench(
    server: ServerConnection,
    section: IdeSection,
    modifier: Modifier = Modifier,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentSection by rememberUpdatedState(section)

    DisposableEffect(Unit) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        onDispose {
            webView?.let { view ->
                view.tag = null
                view.stopLoading()
                view.destroy()
            }
            webView = null
        }
    }

    LaunchedEffect(section, loading) {
        if (!loading && error == null) {
            webView?.let { RemoteWorkbenchRuntime.syncSection(it, section) }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.loadWithOverviewMode = false
                        settings.useWideViewPort = false
                        settings.mediaPlaybackRequiresUserGesture = true
                        isFocusable = true
                        isFocusableInTouchMode = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest) {
                                request.deny()
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                                error = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                view?.let { RemoteWorkbenchRuntime.applyMobileLayout(it, currentSection) }
                                loading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                failure: WebResourceError?,
                            ) {
                                if (request?.isForMainFrame == true) {
                                    error = failure?.description?.toString() ?: "Unable to load this server."
                                    loading = false
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                sslError: SslError?,
                            ) {
                                handler?.cancel()
                                error = "The server’s TLS certificate could not be verified."
                                loading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val scheme = request?.url?.scheme
                                return scheme != "http" && scheme != "https"
                            }
                        }
                        webView = this
                    }
                },
                update = { view ->
                    if (loadedUrl != server.url) {
                        loadedUrl = server.url
                        view.loadUrl(server.url)
                    }
                },
                modifier = Modifier.fillMaxSize().testTag("remote_workbench"),
            )

            if (loading) {
                Column(
                    Modifier.align(Alignment.Center).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Opening ${server.name}…",
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            error?.let { message ->
                Column(
                    Modifier.align(Alignment.Center).background(MaterialTheme.colorScheme.surface).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Connection unavailable", style = MaterialTheme.typography.titleLarge)
                    Text(
                        message,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Check the address, HTTPS certificate and that code-server is running.",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        CodingKeyboard(
            onText = { text ->
                webView?.let { RemoteWorkbenchRuntime.insertText(it, text) }
            },
            onMove = { delta ->
                val keyCode = if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            },
            onSpecial = {
                webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE))
                webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE))
            },
        )
    }
}
