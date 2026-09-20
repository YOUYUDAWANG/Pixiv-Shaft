package ceui.pixiv.network

import android.webkit.CookieManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * FANBOX 的请求头。
 *
 * - `Origin` 是硬性要求:少了它 api.fanbox.cc 一律 400,跟登不登录无关。
 * - cookie 直接从 WebView 的 [CookieManager] 现取,不另存一份 —— 用户在
 *   FANBOX 页面里登录/登出,这边下一个请求就同步了,不存在两份状态对不上的问题。
 */
class FanboxHeaderInterceptor : Interceptor {

    companion object {
        const val FANBOX_ORIGIN = "https://www.fanbox.cc"
        private const val SESSION_COOKIE = "FANBOXSESSID="

        fun currentCookie(): String? =
            runCatching { CookieManager.getInstance().getCookie(FANBOX_ORIGIN) }.getOrNull()

        fun hasSession(): Boolean = currentCookie()?.contains(SESSION_COOKIE) == true
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("Origin", FANBOX_ORIGIN)
            .header("Referer", "$FANBOX_ORIGIN/")
            .header("Accept", "application/json")
            .header("User-Agent", NetworkConstants.WEB_USER_AGENT)
        currentCookie()?.takeIf { it.isNotEmpty() }?.let { builder.header("Cookie", it) }
        return chain.proceed(builder.build())
    }
}
