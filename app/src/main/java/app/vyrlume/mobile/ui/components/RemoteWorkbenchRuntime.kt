package app.vyrlume.mobile.ui.components

import android.os.SystemClock
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import androidx.core.content.ContextCompat
import app.vyrlume.mobile.data.IdeSection
import org.json.JSONObject

internal object RemoteWorkbenchRuntime {
    private const val ACTIVE_INPUT = """
        const active = document.activeElement;
        const isInput = active instanceof HTMLTextAreaElement || active instanceof HTMLInputElement ||
            active?.classList.contains('native-edit-context') || active?.isContentEditable;
        const visible = element => {
            const rect = element.getBoundingClientRect();
            return rect.width > 0 && rect.height > 0 && element.getClientRects().length > 0;
        };
        const candidates = Array.from(document.querySelectorAll(
            '.native-edit-context, .terminal.focus textarea, .terminal.active textarea, .monaco-editor textarea, textarea, input'
        )).filter(visible);
        const target = isInput ? active : candidates[0];
        if (!target) return false;
        target.focus({ preventScroll: true });
    """

    private val mobileLayoutScript = """
        (() => {
            const viewport = document.querySelector('meta[name="viewport"]') || (() => {
                const meta = document.createElement('meta');
                meta.name = 'viewport';
                document.head.appendChild(meta);
                return meta;
            })();
            viewport.content = 'width=device-width, initial-scale=1, maximum-scale=5, user-scalable=yes';
            let style = document.getElementById('vyrlume-mobile-layout');
            if (!style) {
                style = document.createElement('style');
                style.id = 'vyrlume-mobile-layout';
                style.textContent = `
                    @media (max-width: 600px) {
                        .vyrlume-mobile-hidden,
                        html[data-vyrlume-section] #workbench\\.parts\\.titlebar,
                        html[data-vyrlume-section] #workbench\\.parts\\.activitybar,
                        html[data-vyrlume-section] #workbench\\.parts\\.auxiliarybar { display: none !important; }
                        html[data-vyrlume-section] #workbench\\.parts\\.sidebar,
                        html[data-vyrlume-section] #workbench\\.parts\\.editor,
                        html[data-vyrlume-section] #workbench\\.parts\\.panel {
                            position: fixed !important;
                            top: 0 !important;
                            left: 0 !important;
                            right: auto !important;
                            bottom: auto !important;
                            width: 100vw !important;
                            max-width: 100vw !important;
                            height: calc(100vh - 28px) !important;
                            min-height: calc(100vh - 28px) !important;
                            max-height: calc(100vh - 28px) !important;
                            min-width: 0 !important;
                            margin: 0 !important;
                            z-index: 20;
                        }
                        html[data-vyrlume-section] .editor-group-container .editor-actions { display: none !important; }
                        html[data-vyrlume-section] .editor-group-container .tabs-and-actions-container,
                        html[data-vyrlume-section] .editor-group-container .tabs-container { max-width: 100% !important; }
                        html[data-vyrlume-section="TERMINAL"] #workbench\\.parts\\.panel > .composite.title { display: none !important; }
                        html[data-vyrlume-section="TERMINAL"] #workbench\\.parts\\.panel > .content {
                            width: 100vw !important;
                            height: calc(100vh - 28px) !important;
                        }
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar > .content,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .composite.viewlet,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .monaco-pane-view,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .monaco-split-view2,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .monaco-split-view2 > .monaco-scrollable-element,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar > .content,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .composite.viewlet,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .monaco-pane-view,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .monaco-split-view2,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .monaco-split-view2 > .monaco-scrollable-element,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .split-view-container,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .split-view-view,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .pane,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .pane-body,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .pane-body .monaco-scrollable-element,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar .pane-body .monaco-list,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .split-view-container,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .split-view-view,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .pane,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .pane-body,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .pane-body .monaco-scrollable-element,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .pane-body .monaco-list,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .welcome-view,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar .welcome-view-content {
                            width: 100vw !important;
                            max-width: 100vw !important;
                        }
                        html[data-vyrlume-section="EDITOR"] #workbench\\.parts\\.sidebar,
                        html[data-vyrlume-section="EDITOR"] #workbench\\.parts\\.panel,
                        html[data-vyrlume-section="TERMINAL"] #workbench\\.parts\\.sidebar,
                        html[data-vyrlume-section="TERMINAL"] #workbench\\.parts\\.editor,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.editor,
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.panel,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.editor,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.panel { display: none !important; }
                        html[data-vyrlume-section="FILES"] #workbench\\.parts\\.sidebar,
                        html[data-vyrlume-section="SOURCE_CONTROL"] #workbench\\.parts\\.sidebar,
                        html[data-vyrlume-section="TERMINAL"] #workbench\\.parts\\.panel { display: block !important; }
                    }
                `;
                document.head.appendChild(style);
            }
            document.getElementById('workbench.parts.titlebar')?.parentElement?.classList.add('vyrlume-mobile-hidden');
        })();
    """.trimIndent()

    fun applyMobileLayout(view: WebView, section: IdeSection) {
        view.tag = section.name
        focus(view)
        view.evaluateJavascript(mobileLayoutScript, null)
        view.evaluateJavascript(
            "document.documentElement.dataset.vyrlumeSection = '${section.name}';",
            null,
        )
    }

    fun syncSection(view: WebView, section: IdeSection) {
        applyMobileLayout(view, section)
        whenWorkbenchReady(view, section, 0)
    }

    private fun whenWorkbenchReady(view: WebView, section: IdeSection, attempt: Int) {
        if (!isCurrent(view, section)) return
        val icon = if (section == IdeSection.SOURCE_CONTROL) "source-control" else "explorer"
        val extraCheck = if (section == IdeSection.FILES || section == IdeSection.SOURCE_CONTROL) {
            "&& !!document.querySelector('a.action-label.codicon-$icon-view-icon')"
        } else {
            ""
        }
        view.evaluateJavascript(
            "!!document.querySelector('.monaco-workbench') && !!document.getElementById('workbench.parts.panel')$extraCheck;",
        ) { ready ->
            if (!isCurrent(view, section)) return@evaluateJavascript
            if (ready == "true") {
                when (section) {
                    IdeSection.EDITOR -> restoreEditor(view)
                    IdeSection.TERMINAL -> toggleTerminal(view)
                    IdeSection.FILES, IdeSection.SOURCE_CONTROL -> showSidebarView(view, section)
                    else -> Unit
                }
            } else if (attempt < 50) {
                view.postDelayed({ whenWorkbenchReady(view, section, attempt + 1) }, 200)
            }
        }
    }

    fun insertText(view: WebView, text: String) {
        focus(view)
        val inputConnection = view.onCreateInputConnection(EditorInfo())
        if (inputConnection?.commitText(text, 1) == true) return

        val script = """
            (() => {
                $ACTIVE_INPUT
                return document.execCommand('insertText', false, ${JSONObject.quote(text)});
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    private fun restoreEditor(view: WebView) {
        view.evaluateJavascript(
            """
            (() => {
                const panel = document.getElementById('workbench.parts.panel');
                panel?.querySelector('.codicon-panel-maximize.checked')?.click();
                window.dispatchEvent(new Event('resize'));
            })();
            """.trimIndent(),
            null,
        )
        view.postDelayed({
            if (!isCurrent(view, IdeSection.EDITOR)) return@postDelayed
            applyMobileLayout(view, IdeSection.EDITOR)
            view.evaluateJavascript("window.dispatchEvent(new Event('resize'));", null)
            focusEditor(view)
        }, 200)
    }

    private fun toggleTerminal(view: WebView) {
        focus(view)
        view.evaluateJavascript(
            """
            (() => {
                const panel = document.getElementById('workbench.parts.panel');
                return !!document.querySelector('.terminal textarea') && panel?.getBoundingClientRect().width > 0;
            })();
            """.trimIndent(),
        ) { terminalVisible ->
            if (!isCurrent(view, IdeSection.TERMINAL)) return@evaluateJavascript
            if (terminalVisible != "true") {
                val now = SystemClock.uptimeMillis()
                view.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GRAVE, 0, KeyEvent.META_CTRL_ON))
                view.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_GRAVE, 0, KeyEvent.META_CTRL_ON))
            }
            view.postDelayed({
                if (!isCurrent(view, IdeSection.TERMINAL)) return@postDelayed
                applyMobileLayout(view, IdeSection.TERMINAL)
                resizeTerminal(view)
            }, 450)
        }
    }

    private fun resizeTerminal(view: WebView) {
        view.evaluateJavascript(
            """
            (() => {
                const panel = document.getElementById('workbench.parts.panel');
                panel?.querySelector('.codicon-panel-maximize.checked')?.click();
                panel?.querySelector('.codicon-panel-maximize:not(.checked)')?.click();
                window.dispatchEvent(new Event('resize'));
            })();
            """.trimIndent(),
            null,
        )
        view.postDelayed({
            if (!isCurrent(view, IdeSection.TERMINAL)) return@postDelayed
            view.evaluateJavascript("window.dispatchEvent(new Event('resize'));", null)
            focusTerminal(view)
        }, 200)
        view.postDelayed({
            if (!isCurrent(view, IdeSection.TERMINAL)) return@postDelayed
            view.evaluateJavascript("window.dispatchEvent(new Event('resize'));", null)
        }, 900)
    }

    private fun showSidebarView(view: WebView, section: IdeSection) {
        focus(view)
        hideKeyboard(view)
        val icon = if (section == IdeSection.SOURCE_CONTROL) "source-control" else "explorer"
        view.evaluateJavascript(
            """
            (() => {
                const action = document.querySelector('a.action-label.codicon-$icon-view-icon');
                if (!action) return false;
                if (action.parentElement?.getAttribute('aria-selected') !== 'true') action.click();
                window.dispatchEvent(new Event('resize'));
                return true;
            })();
            """.trimIndent(),
            null,
        )
        view.postDelayed({
            if (isCurrent(view, section)) applyMobileLayout(view, section)
        }, 350)
    }

    private fun focusTerminal(view: WebView) {
        focus(view)
        view.evaluateJavascript(
            """
            (() => {
                const terminals = Array.from(document.querySelectorAll('.terminal textarea'));
                const target = terminals.find(element => element.closest('.terminal.focus')) ||
                    terminals.find(element => element.closest('.terminal.active')) || terminals[0];
                if (!target) return false;
                target.focus({ preventScroll: true });
                return document.activeElement === target;
            })();
            """.trimIndent()
        ) { focused ->
            if (focused == "true" && isCurrent(view, IdeSection.TERMINAL)) showKeyboard(view)
        }
    }

    private fun focusEditor(view: WebView) {
        focus(view)
        view.evaluateJavascript(
            """
            (() => {
                const editors = Array.from(document.querySelectorAll('.native-edit-context, .monaco-editor textarea'));
                const target = editors.find(element => {
                    const rect = element.getBoundingClientRect();
                    return rect.width > 0 && rect.height > 0 && element.getClientRects().length > 0;
                });
                if (!target) return false;
                target.focus({ preventScroll: true });
                return document.activeElement === target;
            })();
            """.trimIndent(),
            null,
        )
    }

    private fun isCurrent(view: WebView, section: IdeSection) = view.tag == section.name

    private fun focus(view: WebView) {
        if (!view.hasFocus()) view.requestFocus()
    }

    private fun showKeyboard(view: WebView) {
        val inputMethodManager = ContextCompat.getSystemService(view.context, InputMethodManager::class.java)
        inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: WebView) {
        val inputMethodManager = ContextCompat.getSystemService(view.context, InputMethodManager::class.java)
        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
