package ceui.pixiv.network

import ceui.pixiv.network.contract.CookieProvider
import ceui.pixiv.network.contract.LanguageProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class WebHeaderInterceptor(
    private val cookieProvider: CookieProvider? = null,
    private val languageProvider: LanguageProvider? = null,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(
            addHeader(
                request.newBuilder()
            ).build()
        )
    }

    private fun addHeader(before: Request.Builder): Request.Builder {
        val rawCookie = cookieProvider?.getWebCookie().orEmpty()
        val cookies = CookieNormalizer.normalize(rawCookie)
        val lang = languageProvider?.getAcceptLanguage() ?: "zh_CN"
        before.addHeader("accept-language", lang)
            .addHeader("Host", "www.pixiv.net")
            .addHeader("Cookie", cookies)
            .addHeader("Referer", "https://www.pixiv.net/")
            .addHeader("User-Agent", NetworkConstants.WEB_USER_AGENT)
        return before
    }
}
