package ceui.pixiv.network

import ceui.pixiv.network.contract.LanguageProvider
import ceui.pixiv.network.contract.TokenProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class HeaderInterceptor(
    private val tokenProvider: TokenProvider? = null,
    private val languageProvider: LanguageProvider? = null,
) : Interceptor {

    companion object {
        const val EXPLICIT_AUTHORIZATION_HEADER = "X-Shaft-Explicit-Authorization"

        fun shouldInjectSessionAuthorization(request: Request): Boolean =
            request.header(EXPLICIT_AUTHORIZATION_HEADER) != "1" &&
                request.header(NetworkConstants.HEADER_AUTH) == null

        // 对齐 Pixiv iOS 官方客户端抓包（8.6.10 / iOS 26.5 / iPhone16,2），改版本号只改这里
        const val APP_VERSION = "8.6.10"
        const val APP_OS_VERSION = "26.5"
        const val DEVICE_MODEL = "iPhone16,2"
        const val USER_AGENT = "PixivIOSApp/$APP_VERSION (iOS $APP_OS_VERSION; $DEVICE_MODEL)"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(
            addHeader(
                request.newBuilder(),
                includeSessionAuthorization = shouldInjectSessionAuthorization(request),
            ).build()
        )
    }

    private fun addHeader(
        before: Request.Builder,
        includeSessionAuthorization: Boolean,
    ): Request.Builder {
        val requestNonce = RequestNonce.build()
        if (includeSessionAuthorization) {
            val bearerToken = tokenProvider?.getBearerToken().orEmpty()
            if (bearerToken.isNotEmpty()) {
                val headerVal = if (bearerToken.startsWith(NetworkConstants.TOKEN_HEAD)) {
                    bearerToken
                } else {
                    NetworkConstants.TOKEN_HEAD + bearerToken
                }
                before.addHeader(NetworkConstants.HEADER_AUTH, headerVal)
            }
        }
        val lang = languageProvider?.getAcceptLanguage() ?: "zh_CN"
        before.addHeader("accept-language", lang)
            .addHeader("app-accept-language", lang)
            .addHeader("app-os", "ios")
            .addHeader("app-os-version", APP_OS_VERSION)
            .addHeader("app-version", APP_VERSION)
            .addHeader("x-client-time", requestNonce.xClientTime)
            .addHeader("x-client-hash", requestNonce.xClientHash)
        before.addHeader("user-agent", USER_AGENT)
        return before
    }
}
