package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.isOutdated
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

class CloudflareInterceptor(
    private val context: Context,
    private val cookieManager: AndroidCookieJar,
    private val defaultUserAgentProvider: () -> String,
) : WebViewInterceptor(context, defaultUserAgentProvider) {

    private val executor = ContextCompat.getMainExecutor(context)
    private val activeChallenges = ConcurrentHashMap<String, CountDownLatch>()

    override fun shouldIntercept(response: Response): Boolean {
        val server = response.header("Server")?.lowercase().orEmpty()
        val isCloudflareServer = server in SERVER_CHECK || server.contains("cloudflare")
        val isChallenge = response.header("cf-mitigated") == "challenge" ||
            response.header("cf-chl-bypass") != null ||
            response.headers("Set-Cookie").any { it.contains("cf_clearance") || it.contains("__cf_bm") }
        val isErrorCode = response.code in ERROR_CODES
        val should = isChallenge || (isErrorCode && (isCloudflareServer || response.header("Server").isNullOrBlank()))
        if (should) {
            logcat(LogPriority.INFO) {
                "[CLOUDFLARE] Intercept trigger: code=${response.code}, cf-mitigated=${response.header("cf-mitigated")}, server=${response.header("Server")}, url=${response.request.url}"
            }
        }
        return should
    }

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response {
        val host = request.url.host

        // Grace period check: if recently solved and clearance cookie is present, retry immediately
        val solvedAt = NetworkHelper.lastSolveAtMs
        val elapsed = System.currentTimeMillis() - solvedAt
        val justSolved = solvedAt > 0 && elapsed < SOLVE_GRACE_MS
        if (justSolved && cookieManager.get(request.url).any { it.name == "cf_clearance" }) {
            logcat(LogPriority.INFO) {
                "[CLOUDFLARE] Grace period active (solved ${elapsed}ms ago) for $host, retrying request directly"
            }
            response.close()
            val retried = chain.proceed(request)
            if (!shouldIntercept(retried)) {
                return retried
            }
            retried.close()
        }

        // Deduplicate concurrent challenges for the same host
        val existingLatch = activeChallenges[host]
        if (existingLatch != null) {
            logcat(LogPriority.INFO) {
                "[CLOUDFLARE] Challenge already in flight for $host, awaiting existing solver latch..."
            }
            response.close()
            try {
                existingLatch.await()
            } catch (_: InterruptedException) {
            }
            return chain.proceed(request)
        }

        val newLatch = CountDownLatch(1)
        val competingLatch = activeChallenges.putIfAbsent(host, newLatch)
        if (competingLatch != null) {
            logcat(LogPriority.INFO) {
                "[CLOUDFLARE] Competing challenge detected for $host, awaiting competing solver latch..."
            }
            response.close()
            try {
                competingLatch.await()
            } catch (_: InterruptedException) {
            }
            return chain.proceed(request)
        }

        try {
            response.close()
            val oldCookie = cookieManager.get(request.url)
                .firstOrNull { it.name == "cf_clearance" }
            cookieManager.remove(request.url, COOKIE_NAMES, 0)
            android.webkit.CookieManager.getInstance().flush()

            val originalUserAgent = request.header("User-Agent") ?: run {
                try {
                    android.webkit.WebSettings.getDefaultUserAgent(context)
                } catch (_: Exception) {
                    defaultUserAgentProvider()
                }
            }
            val cleanUserAgent = cleanUserAgent(originalUserAgent)
            logcat(LogPriority.INFO) {
                "[CLOUDFLARE] Initiating WebView solve for $host with clean User-Agent: '$cleanUserAgent'"
            }

            val newRequest = request.newBuilder()
                .header("User-Agent", cleanUserAgent)
                .apply {
                    addClientHints(this, cleanUserAgent)
                }
                .build()

            resolveWithWebView(newRequest, oldCookie, cleanUserAgent)

            logcat(LogPriority.INFO) {
                "[CLOUDFLARE] Proceeding with solved request for $host"
            }
            return chain.proceed(newRequest)
        } catch (e: CloudflareBypassException) {
            logcat(LogPriority.ERROR, e) {
                "[CLOUDFLARE] Bypass failed for $host: CloudflareBypassException"
            }
            throw IOException(context.stringResource(MR.strings.information_cloudflare_bypass_failure), e)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) {
                "[CLOUDFLARE] Exception during bypass for $host: ${e.message}"
            }
            throw IOException(e)
        } finally {
            activeChallenges.remove(host)
            newLatch.countDown()
        }
    }

    private fun cleanUserAgent(userAgent: String): String {
        return userAgent
            .replace(ANIYOMI_USER_AGENT_REGEX, "")
            .replace(ANIZEN_USER_AGENT_REGEX, "")
            .replace(TACHIYOMI_USER_AGENT_REGEX, "")
            .replace(CHIMANEW_USER_AGENT_REGEX, "")
            .replace(CHIMAHON_USER_AGENT_REGEX, "")
            .replace(MIHON_USER_AGENT_REGEX, "")
            .replace("; wv", "")
            .trim()
    }

    private fun addClientHints(builder: Request.Builder, userAgent: String) {
        val chromeVersionMatch = CHROME_VERSION_REGEX.find(userAgent)
        val chromeVersion = chromeVersionMatch?.groupValues?.get(1) ?: "131"
        val androidVersion = android.os.Build.VERSION.RELEASE.takeWhile { it.isDigit() }.ifEmpty { "13" }

        builder.header("Sec-CH-UA", "\"Chromium\";v=\"$chromeVersion\", \"Google Chrome\";v=\"$chromeVersion\", \"Not=A?Brand\";v=\"99\"")
        builder.header("Sec-CH-UA-Mobile", "?1")
        builder.header("Sec-CH-UA-Platform", "\"Android\"")
        builder.header("Sec-CH-UA-Platform-Version", "\"$androidVersion.0.0\"")
    }

    private fun resolveWithWebView(originalRequest: Request, oldCookie: Cookie?, cleanUserAgent: String) {
        val latch = CountDownLatch(1)

        var webview: WebView? = null
        var attachedToWindow = false
        var parentView: android.view.ViewGroup? = null

        var challengeFound = false
        var cloudflareBypassed = false
        var isWebViewOutdated = false

        val origRequestUrl = originalRequest.url.toString()
        val headers = parseHeaders(originalRequest.headers)

        executor.execute {
            val activity = ActivityTracker.activeActivity?.get()
            val webViewContext = activity ?: context
            val createdWebView = createWebView(originalRequest, webViewContext).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(1080, 1920)
                measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY),
                )
                layout(0, 0, 1080, 1920)

                // Render visible to pass Cloudflare Turnstile anti-clickjacking opacity checks
                alpha = 1.0f

                requestFocus()
                onResume()
                resumeTimers()
            }
            webview = createdWebView

            createdWebView.addJavascriptInterface(
                object {
                    @android.webkit.JavascriptInterface
                    fun interactiveBegin() {
                        logcat(LogPriority.INFO) { "[CLOUDFLARE] JS event interactiveBegin received" }
                        challengeFound = true
                    }
                },
                "chimanew",
            )
            createdWebView.addJavascriptInterface(
                object {
                    @android.webkit.JavascriptInterface
                    fun interactiveBegin() {
                        challengeFound = true
                    }
                },
                "anizen",
            )

            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                try {
                    parentView = activity.findViewById(android.R.id.content) as? android.view.ViewGroup
                    parentView?.addView(createdWebView, 0, android.view.ViewGroup.LayoutParams(1080, 1920))
                    attachedToWindow = true
                    logcat(LogPriority.INFO) { "[CLOUDFLARE] WebView attached to Activity window (1080x1920, alpha=1.0)" }
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e) { "[CLOUDFLARE] Failed to attach WebView to Activity, falling back to detached" }
                }
            }

            createdWebView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    view.evaluateJavascript(
                        """
                        try {
                            if ('webdriver' in navigator && navigator.webdriver) {
                                Object.defineProperty(navigator, 'webdriver', { get: () => false });
                            }
                            Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });
                            Object.defineProperty(navigator, 'plugins', { get: () => [
                                { description: "Portable Document Format", filename: "internal-pdf-viewer", name: "Chromium PDF Viewer" }
                            ] });
                            window.chrome = { runtime: {}, loadTimes: function() {}, csi: function() {} };

                            // Override document focus & visibility
                            Object.defineProperty(document, 'hidden', { get: () => false });
                            Object.defineProperty(document, 'visibilityState', { get: () => 'visible' });
                            Object.defineProperty(document, 'hasFocus', { get: () => () => true });
                            window.hasFocus = () => true;

                            // Listen for Cloudflare interactive challenges
                            window.addEventListener('message', function(e) {
                                if (e.data && (e.data === 'interactiveBegin' || (typeof e.data === 'object' && e.data.event === 'interactiveBegin'))) {
                                    if (window.chimanew && window.chimanew.interactiveBegin) {
                                        window.chimanew.interactiveBegin();
                                    } else if (window.anizen && window.anizen.interactiveBegin) {
                                        window.anizen.interactiveBegin();
                                    }
                                }
                            });
                        } catch (e) {}
                        """.trimIndent(),
                        null,
                    )
                }

                override fun onPageFinished(view: WebView, url: String) {
                    fun isCloudFlareBypassed(): Boolean {
                        return cookieManager.get(origRequestUrl.toHttpUrl())
                            .firstOrNull { it.name == "cf_clearance" }
                            .let { it != null && it != oldCookie }
                    }

                    if (isCloudFlareBypassed()) {
                        logcat(LogPriority.INFO) { "[CLOUDFLARE] Clearance cookie already acquired on page finish for $origRequestUrl" }
                        cloudflareBypassed = true
                        NetworkHelper.lastSolveAtMs = System.currentTimeMillis()
                        NetworkHelper.rememberSolveUa(origRequestUrl.toHttpUrl().host, cleanUserAgent)
                        latch.countDown()
                        return
                    }

                    val title = (view.title ?: "").lowercase()
                    if (title.contains("just a moment") ||
                        title.contains("attention required") ||
                        title.contains("un instant") ||
                        title.contains("einen moment") ||
                        title.contains("un momento") ||
                        title.contains("один момент")
                    ) {
                        logcat(LogPriority.INFO) { "[CLOUDFLARE] Detected challenge page title: '$title'" }
                        challengeFound = true
                    }

                    if (url == origRequestUrl && !challengeFound) {
                        logcat(LogPriority.INFO) { "[CLOUDFLARE] Page finished with no challenge detected on first load" }
                        latch.countDown()
                        return
                    }

                    // Dispatch native touch events through Android pipeline to bypass Turnstile
                    CoroutineScope(Dispatchers.Default).launch {
                        if (CloudflareSolver.solve(view)) {
                            for (i in 0 until 10) {
                                if (isCloudFlareBypassed()) {
                                    logcat(LogPriority.INFO) {
                                        "[CLOUDFLARE] Cloudflare bypass verified! cf_clearance successfully stored for ${origRequestUrl.toHttpUrl().host}"
                                    }
                                    cloudflareBypassed = true
                                    NetworkHelper.lastSolveAtMs = System.currentTimeMillis()
                                    NetworkHelper.rememberSolveUa(origRequestUrl.toHttpUrl().host, cleanUserAgent)
                                    latch.countDown()
                                    return@launch
                                }
                                delay(500)
                            }
                        }
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?,
                ) {
                    if (request?.isForMainFrame == true) {
                        if (errorResponse?.statusCode in ERROR_CODES) {
                            logcat(LogPriority.INFO) {
                                "[CLOUDFLARE] onReceivedHttpError statusCode=${errorResponse?.statusCode} (challenge page confirmed)"
                            }
                            challengeFound = true
                        } else {
                            latch.countDown()
                        }
                    }
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) {
                        logcat(LogPriority.WARN) {
                            "[CLOUDFLARE] onReceivedError main frame error: ${error.description}"
                        }
                        latch.countDown()
                    }
                }
            }

            webview?.loadUrl(origRequestUrl, headers)
        }

        val pollTimer = java.util.Timer()
        pollTimer.schedule(
            object : java.util.TimerTask() {
                override fun run() {
                    val currentCookie = cookieManager.get(origRequestUrl.toHttpUrl())
                        .firstOrNull { it.name == "cf_clearance" }
                    if (currentCookie != null && currentCookie != oldCookie) {
                        logcat(LogPriority.INFO) {
                            "[CLOUDFLARE] Poller detected new cf_clearance cookie for ${origRequestUrl.toHttpUrl().host}"
                        }
                        cloudflareBypassed = true
                        NetworkHelper.lastSolveAtMs = System.currentTimeMillis()
                        NetworkHelper.rememberSolveUa(origRequestUrl.toHttpUrl().host, cleanUserAgent)
                        latch.countDown()
                        pollTimer.cancel()
                        return
                    }

                    executor.execute {
                        webview?.evaluateJavascript(
                            """
                        (function() {
                            try {
                                var href = (document.location && document.location.href) || '';
                                if (href === '' || href === 'about:blank') return 'wait';
                                if (document.readyState !== 'interactive' && document.readyState !== 'complete') return 'wait';
                                var t = (document.title || '').toLowerCase();
                                if (t.indexOf('attention required') !== -1 || t.indexOf('access denied') !== -1) return 'error';
                                if (t.indexOf('just a moment') !== -1 || t.indexOf('un instant') !== -1 ||
                                    t.indexOf('einen moment') !== -1 || t.indexOf('un momento') !== -1 ||
                                    t.indexOf('один момент') !== -1) return 'wait';
                                if (document.querySelector('#challenge-running, #challenge-stage, #cf-challenge-running, .cf-browser-verification, #turnstile-wrapper, #cf-please-wait, script[src*="challenge-platform"]')) return 'wait';
                                if (!document.body || document.body.children.length === 0) return 'wait';
                                return 'ok';
                            } catch (e) { return 'wait'; }
                        })()
                            """.trimIndent(),
                        ) { state ->
                            if (state == "\"ok\"") {
                                val current = cookieManager.get(origRequestUrl.toHttpUrl())
                                    .firstOrNull { it.name == "cf_clearance" }
                                if (current != null && current != oldCookie) {
                                    logcat(LogPriority.INFO) {
                                        "[CLOUDFLARE] Poller verified completion for ${origRequestUrl.toHttpUrl().host}"
                                    }
                                    cloudflareBypassed = true
                                    NetworkHelper.lastSolveAtMs = System.currentTimeMillis()
                                    NetworkHelper.rememberSolveUa(origRequestUrl.toHttpUrl().host, cleanUserAgent)
                                    latch.countDown()
                                    pollTimer.cancel()
                                }
                            }
                        }
                    }
                }
            },
            0L,
            1000L,
        )

        try {
            latch.awaitFor30Seconds()
        } finally {
            pollTimer.cancel()
        }

        executor.execute {
            if (!cloudflareBypassed) {
                isWebViewOutdated = webview?.isOutdated() == true
            }

            webview?.run {
                if (attachedToWindow) {
                    try {
                        parentView?.removeView(this)
                    } catch (_: Exception) {}
                }
                stopLoading()
                destroy()
            }
            android.webkit.CookieManager.getInstance().flush()
        }

        if (!cloudflareBypassed) {
            if (isWebViewOutdated) {
                context.toast(MR.strings.information_webview_outdated, Toast.LENGTH_LONG)
            }
            logcat(LogPriority.ERROR) {
                "[CLOUDFLARE] Bypass failed for $origRequestUrl: no clearance cookie acquired within deadline"
            }
            throw CloudflareBypassException()
        }
    }
}

private val ERROR_CODES = listOf(403, 503)
private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare")
private val COOKIE_NAMES = listOf("cf_clearance")
private const val SOLVE_GRACE_MS = 90_000L

private val ANIYOMI_USER_AGENT_REGEX = Regex("\\s+Aniyomi/\\S+", RegexOption.IGNORE_CASE)
private val ANIZEN_USER_AGENT_REGEX = Regex("\\s+AniZen/\\S+", RegexOption.IGNORE_CASE)
private val TACHIYOMI_USER_AGENT_REGEX = Regex("\\s+Tachiyomi/\\S+", RegexOption.IGNORE_CASE)
private val CHIMANEW_USER_AGENT_REGEX = Regex("\\s+Chimanew/\\S+", RegexOption.IGNORE_CASE)
private val CHIMAHON_USER_AGENT_REGEX = Regex("\\s+Chimahon/\\S+", RegexOption.IGNORE_CASE)
private val MIHON_USER_AGENT_REGEX = Regex("\\s+Mihon/\\S+", RegexOption.IGNORE_CASE)
private val CHROME_VERSION_REGEX = Regex("Chrome/(\\d+)")

class CloudflareBypassException(message: String = "Cloudflare verification required") : Exception(message)

object ActivityTracker : android.app.Application.ActivityLifecycleCallbacks {
    var activeActivity: java.lang.ref.WeakReference<android.app.Activity>? = null

    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
    override fun onActivityStarted(activity: android.app.Activity) {}
    override fun onActivityResumed(activity: android.app.Activity) {
        activeActivity = java.lang.ref.WeakReference(activity)
    }
    override fun onActivityPaused(activity: android.app.Activity) {
        if (activeActivity?.get() == activity) {
            activeActivity = null
        }
    }
    override fun onActivityStopped(activity: android.app.Activity) {}
    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
    override fun onActivityDestroyed(activity: android.app.Activity) {}
}
