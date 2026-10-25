package app.vyrlume.mobile.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewSurface(
    url: String?,
    demoHtml: String?,
    reloadKey: Int,
    modifier: Modifier = Modifier,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadedTarget by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

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
                webViewClient = WebViewClient()
                webView = this
            }
        },
        update = { view ->
            val target = "${url ?: "demo"}:reload=$reloadKey"
            if (loadedTarget != target) {
                loadedTarget = target
                if (demoHtml != null) {
                    view.loadDataWithBaseURL("https://demo.vyrlume.local/", demoHtml, "text/html", "utf-8", null)
                } else if (url != null) {
                    view.loadUrl(url)
                }
            }
        },
        modifier = modifier.fillMaxSize().testTag("preview_surface"),
    )
}

val DemoPreviewHtml = """
    <!doctype html>
    <html lang="en">
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <style>
        * { box-sizing: border-box; }
        body { margin: 0; min-height: 100vh; padding: 40px 24px; color: #effbff;
          font-family: "Segoe UI", sans-serif; background: radial-gradient(circle at 85% 5%, #32b9df 0, transparent 35%),
          linear-gradient(155deg, #0a5678, #061b2d 70%); }
        main { max-width: 560px; margin: auto; }
        .pill { display: inline-block; padding: 7px 12px; border: 1px solid #70dbf2;
          border-radius: 999px; color: #a5e65a; font-size: 13px; }
        h1 { margin: 22px 0 12px; font-size: clamp(40px, 10vw, 68px); line-height: .98; letter-spacing: -2px; }
        p { color: #b9d4e2; font-size: 17px; line-height: 1.6; }
        .card { margin-top: 28px; padding: 24px; border: 1px solid #4c7c91; border-radius: 24px;
          background: rgba(13, 52, 74, .78); box-shadow: inset 0 1px rgba(255,255,255,.15), 0 20px 50px #00101a88; }
        button { min-height: 48px; padding: 0 22px; border: 0; border-radius: 14px;
          color: #062033; background: linear-gradient(#9aebff, #45bddf); font-weight: 700; font-size: 16px; }
        output { display: block; margin-top: 18px; color: #a5e65a; font-size: 15px; }
      </style>
    </head>
    <body><main>
      <span class="pill">● LIVE DEMO · PORT 3000</span>
      <h1>Small screen.<br>Big ideas.</h1>
      <p>This interactive preview is bundled with Vyrlume. Your real workspace can preview forwarded development servers here.</p>
      <div class="card">
        <strong>Aero Notes</strong>
        <p>Tap the button to verify that preview interactions work.</p>
        <button id="action">Create a note</button>
        <output id="output">No notes yet — ready when you are.</output>
      </div>
    </main>
    <script>
      let count = 0;
      document.getElementById('action').addEventListener('click', () => {
        count += 1;
        document.getElementById('output').textContent = count + (count === 1 ? ' note created locally.' : ' notes created locally.');
      });
    </script>
    </body></html>
""".trimIndent()
