package ceui.pixiv.api

import ceui.lisa.http.AppApiProxyInterceptor
import ceui.lisa.http.AppApiTimeouts
import ceui.lisa.http.CronetInterceptor
import ceui.lisa.http.IPv4OnlyDns
import ceui.lisa.http.WebApiTimeouts
import ceui.pixiv.network.FanboxHeaderInterceptor
import ceui.pixiv.network.HeaderInterceptor
import ceui.pixiv.network.NetworkConstants
import ceui.pixiv.network.RequestLogInterceptor
import ceui.pixiv.network.TokenFetcherInterceptor
import ceui.pixiv.network.WebHeaderInterceptor
import ceui.pixiv.network.contract.CookieProvider
import ceui.pixiv.network.contract.LanguageProvider
import ceui.pixiv.network.contract.NetworkConfigProvider
import ceui.pixiv.network.contract.TokenProvider
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.chromium.net.CronetEngine
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

open class PixivClientManager(
    var tokenProvider: TokenProvider? = null,
    var configProvider: NetworkConfigProvider? = null,
    var languageProvider: LanguageProvider? = null,
    var cookieProvider: CookieProvider? = null,
    var cronetEngineSupplier: (() -> CronetEngine?)? = null,
) {
    companion object {
        const val REQUIEST_TIME = 10L
    }

    protected open fun applyAppApiProxy(builder: OkHttpClient.Builder) {
        val config = configProvider
        if (config != null && config.isUseAppApiProxy) {
            builder.addInterceptor(AppApiProxyInterceptor(config))
        }
    }

    protected open fun applyDirectConnect(builder: OkHttpClient.Builder) {
        val config = configProvider
        if (config != null && config.isDirectConnect) {
            val engine = cronetEngineSupplier?.invoke()
            if (engine != null) {
                builder.addInterceptor(CronetInterceptor(engine))
            }
        }
    }

    fun <T> createAPPAPI(service: Class<T>): T {
        val okhttpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(AppApiTimeouts.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppApiTimeouts.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppApiTimeouts.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .dns(IPv4OnlyDns)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))

        RequestLogInterceptor.installOn(okhttpClientBuilder, "Net/AppApi")
        okhttpClientBuilder.addInterceptor(HeaderInterceptor(tokenProvider, languageProvider))
        okhttpClientBuilder.addInterceptor(TokenFetcherInterceptor(tokenProvider))
        applyAppApiProxy(okhttpClientBuilder)
        applyDirectConnect(okhttpClientBuilder)

        return Retrofit.Builder()
            .baseUrl(NetworkConstants.APP_API_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okhttpClientBuilder.build())
            .build()
            .create(service)
    }

    fun <T> createWebAPIService(service: Class<T>): T {
        val httpBuilder = OkHttpClient.Builder()
            .connectTimeout(WebApiTimeouts.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WebApiTimeouts.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(WebApiTimeouts.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .dns(IPv4OnlyDns)
            .protocols(listOf(Protocol.HTTP_1_1))

        RequestLogInterceptor.installOn(httpBuilder, "Net/WebApi")
        httpBuilder.addInterceptor(WebHeaderInterceptor(cookieProvider, languageProvider))
        applyDirectConnect(httpBuilder)

        return Retrofit.Builder()
            .baseUrl(NetworkConstants.WEB_API_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpBuilder.build())
            .build()
            .create(service)
    }

    fun <T> createComicService(service: Class<T>): T {
        val httpBuilder = OkHttpClient.Builder()
            .connectTimeout(REQUIEST_TIME, TimeUnit.SECONDS)
            .writeTimeout(REQUIEST_TIME, TimeUnit.SECONDS)
            .readTimeout(REQUIEST_TIME, TimeUnit.SECONDS)
            .dns(IPv4OnlyDns)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))

        RequestLogInterceptor.installOn(httpBuilder, "Net/Comic")
        httpBuilder.addInterceptor(HeaderInterceptor(tokenProvider, languageProvider))
        httpBuilder.addInterceptor(TokenFetcherInterceptor(tokenProvider))
        applyAppApiProxy(httpBuilder)
        applyDirectConnect(httpBuilder)

        return Retrofit.Builder()
            .baseUrl(NetworkConstants.COMIC_API_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpBuilder.build())
            .build()
            .create(service)
    }

    fun <T> createFanboxService(service: Class<T>): T {
        val httpBuilder = OkHttpClient.Builder()
            .connectTimeout(REQUIEST_TIME, TimeUnit.SECONDS)
            .writeTimeout(REQUIEST_TIME, TimeUnit.SECONDS)
            .readTimeout(REQUIEST_TIME, TimeUnit.SECONDS)
            .dns(IPv4OnlyDns)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))

        RequestLogInterceptor.installOn(httpBuilder, "Net/Fanbox")
        httpBuilder.addInterceptor(FanboxHeaderInterceptor())
        applyDirectConnect(httpBuilder)

        return Retrofit.Builder()
            .baseUrl(NetworkConstants.FANBOX_API_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpBuilder.build())
            .build()
            .create(service)
    }
}
