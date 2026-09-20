package ceui.pixiv.network

import ceui.pixiv.network.contract.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class TokenFetcherInterceptor(
    private val tokenProvider: TokenProvider? = null,
) : Interceptor {

    private companion object {
        const val TOKEN_ERROR_PEEK_BYTES = 4096L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val explicitAuthorization = originalRequest.header(HeaderInterceptor.EXPLICIT_AUTHORIZATION_HEADER) == "1"
        val request = if (explicitAuthorization) {
            originalRequest.newBuilder()
                .removeHeader(HeaderInterceptor.EXPLICIT_AUTHORIZATION_HEADER)
                .build()
        } else {
            originalRequest
        }
        val response = chain.proceed(request)

        return if (!explicitAuthorization && response.code == 400) {
            val gson = response.peekBody(TOKEN_ERROR_PEEK_BYTES).string()
            val currentToken = tokenProvider?.getBearerToken()
            if (!currentToken.isNullOrEmpty() &&
                (gson.contains(NetworkConstants.TOKEN_ERROR_1) || gson.contains(NetworkConstants.TOKEN_ERROR_2))) {
                val tokenForThisRequest = request.header(NetworkConstants.HEADER_AUTH)
                    ?.removePrefix(NetworkConstants.TOKEN_HEAD) ?: ""
                Timber.tag("TokenRefresh").d(
                    "[%s] 400 token error on %s %s → asking for refresh",
                    Thread.currentThread().name, request.method, request.url.encodedPath,
                )
                val refreshedAccessToken = tokenProvider.refreshTokenBlocking(tokenForThisRequest)
                if (refreshedAccessToken != null) {
                    Timber.tag("TokenRefresh").d(
                        "[%s] replaying %s %s with refreshed token",
                        Thread.currentThread().name, request.method, request.url.encodedPath,
                    )
                    response.close()
                    val authHeaderVal = if (refreshedAccessToken.startsWith(NetworkConstants.TOKEN_HEAD)) {
                        refreshedAccessToken
                    } else {
                        NetworkConstants.TOKEN_HEAD + refreshedAccessToken
                    }
                    val newRequest = request
                        .newBuilder()
                        .header(NetworkConstants.HEADER_AUTH, authHeaderVal)
                        .build()
                    chain.proceed(newRequest)
                } else {
                    Timber.tag("TokenRefresh").w(
                        "[%s] no refreshed token for %s %s → returning original 400",
                        Thread.currentThread().name, request.method, request.url.encodedPath,
                    )
                    response
                }
            } else {
                response
            }
        } else {
            response
        }
    }
}
