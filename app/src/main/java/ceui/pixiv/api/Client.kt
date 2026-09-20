package ceui.pixiv.api

import ceui.lisa.activities.Shaft
import ceui.lisa.helper.LanguageHelper
import ceui.lisa.http.CronetInterceptor
import ceui.lisa.http.HttpDns
import ceui.pixiv.auth.AuthSessionManager
import ceui.pixiv.auth.MediaAuthSessionManager
import ceui.pixiv.network.NetworkConstants
import ceui.pixiv.network.contract.CookieProvider
import ceui.pixiv.network.contract.LanguageProvider
import ceui.pixiv.network.contract.NetworkConfigProvider
import ceui.pixiv.network.contract.TokenProvider
import ceui.pixiv.safe.auth.BearerInterceptor
import ceui.pixiv.safe.auth.SessionProvider
import ceui.pixiv.safe.auth.TokenAuthenticator
import ceui.pixiv.session.SessionManager
import ceui.pixiv.shaftapi.MediaApi
import ceui.pixiv.shaftapi.MediaHttpTransport
import ceui.pixiv.shaftapi.PixshaftApi
import ceui.pixiv.shaftapi.ShaftHmac
import ceui.pixiv.shaftapi.TranslateUserAgentInterceptor
import com.tencent.mmkv.MMKV
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.function.BooleanSupplier

object Client {

    init {
        initProviders()
    }

    fun initProviders() {
        HttpDns.secureDnsSupplier = BooleanSupplier {
            Shaft.sSettings?.isUseSecureDns == true
        }
        PixivClient.manager = PixivClientManager(
            tokenProvider = object : TokenProvider {
                override fun getBearerToken(): String? = SessionManager.getBearerTokenOrEmpty()
                override fun refreshTokenBlocking(staleToken: String): String? =
                    SessionManager.refreshAccessToken(staleToken)
                override fun onSessionRevoked() {
                    SessionManager.postUpdateSession(null)
                }
            },
            configProvider = object : NetworkConfigProvider {
                override val isDirectConnect: Boolean
                    get() = Shaft.sSettings?.isDirectConnect == true
                override val isUseAppApiProxy: Boolean
                    get() = Shaft.sSettings?.isUseAppApiProxy == true
                override val appApiProxyUrl: String
                    get() = Shaft.sSettings?.appApiProxy.orEmpty()
                override val isUseSecureDns: Boolean
                    get() = Shaft.sSettings?.isUseSecureDns == true
            },
            languageProvider = object : LanguageProvider {
                override fun getAcceptLanguage(): String =
                    LanguageHelper.getRequestHeaderAcceptLanguageFromAppLanguage()
            },
            cookieProvider = object : CookieProvider {
                override fun getWebCookie(): String =
                    runCatching { MMKV.defaultMMKV().getString(SessionManager.COOKIE_KEY, "") }.getOrNull().orEmpty()
            },
            cronetEngineSupplier = {
                runCatching { CronetInterceptor.getEngine(Shaft.getContext()) }.getOrNull()
            }
        )
    }

    @Volatile private var _appApi: API? = null
    @Volatile private var _webApi: PixivWebApi? = null

    val appApi: API get() = _appApi ?: PixivClient.appApi
    val webApi: PixivWebApi get() = _webApi ?: PixivClient.webApi
    val comicApi: ComicApi get() = PixivClient.comicApi
    val fanboxApi: FanboxApi get() = PixivClient.fanboxApi

    fun reset() {
        _appApi = null
        _webApi = null
        PixivClient.reset()
    }

    val moonAPI: MoonAPI by lazy {
        clientManager.createMoonAPIService(MoonAPI::class.java)
    }

    val pixshaft: PixshaftApi by lazy {
        clientManager.createPixshaftService(PixshaftApi::class.java)
    }

    val plazaAPI: ceui.pixiv.plaza.PlazaApi by lazy {
        clientManager.createMediaService(ceui.pixiv.plaza.PlazaApi::class.java)
    }

    val mediaAPI: MediaApi by lazy {
        clientManager.createMediaService(MediaApi::class.java)
    }

    private val clientManager = ClientManager()
}

class ClientManager : PixivClientManager() {

    companion object {
        const val APP_API_HOST = NetworkConstants.APP_API_HOST
        const val WEB_API_HOST = NetworkConstants.WEB_API_HOST
        const val COMIC_API_HOST = NetworkConstants.COMIC_API_HOST
        const val FANBOX_API_HOST = NetworkConstants.FANBOX_API_HOST
        const val HEADER_AUTH = NetworkConstants.HEADER_AUTH
        const val TOKEN_HEAD = NetworkConstants.TOKEN_HEAD
        const val TOKEN_ERROR_1 = NetworkConstants.TOKEN_ERROR_1
        const val TOKEN_ERROR_2 = NetworkConstants.TOKEN_ERROR_2
        const val WEB_USER_AGENT = NetworkConstants.WEB_USER_AGENT

        const val NETEASY_API_HOST = "http://192.243.123.124:3000"
        const val MOON_API_HOST = "https://shaft.api:8443/"
        const val MOON_BACKEND_HOSTNAME = "shaft.api"
        const val MOON_BACKEND_IP = "111.229.197.181"
        const val PIXSHAFT_API_HOST = "https://pixshaft.com/"
        const val MEDIA_API_HOST = "https://api.pixshaft.com/"
        const val PIXSHAFT_TRANSLATE_READ_TIMEOUT_SECONDS = 120
        const val REQUIEST_TIME = 10L
    }

    fun <T> createPixshaftService(service: Class<T>): T =
        createFirstPartyService(service, PIXSHAFT_API_HOST, AuthSessionManager)

    fun <T> createMediaService(service: Class<T>): T =
        createFirstPartyService(service, MEDIA_API_HOST, MediaAuthSessionManager)

    private fun <T> createFirstPartyService(
        service: Class<T>,
        baseUrl: String,
        sessions: SessionProvider,
    ): T {
        val httpBuilder =
            (if (baseUrl == MEDIA_API_HOST) MediaHttpTransport.apiClient.newBuilder()
                else OkHttpClient.Builder())
                .connectTimeout(6, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .addInterceptor(BearerInterceptor(sessions))
                .authenticator(TokenAuthenticator(sessions))
                .addInterceptor(TranslateUserAgentInterceptor())
                .addInterceptor { chain ->
                    val req = chain.request()
                    val path = req.url.encodedPath
                    val message =
                        when {
                            !ShaftHmac.isConfigured -> null
                            path.contains("/v1/account/") || path.endsWith("/v1/push/ack") ->
                                req.body?.let { body ->
                                    Buffer().also { body.writeTo(it) }.readUtf8()
                                }
                            path.endsWith("/v1/config") -> req.url.queryParameter("uid")
                            else -> null
                        }
                    if (message == null) {
                        chain.proceed(req)
                    } else {
                        val sig = ShaftHmac.signHex(message)
                        chain.proceed(req.newBuilder().header("X-Shaft-Sign", sig).build())
                    }
                }
                .addInterceptor { chain ->
                    val req = chain.request()
                    if (req.url.encodedPath.endsWith("/v1/account/translate")) {
                        chain
                            .withReadTimeout(
                                PIXSHAFT_TRANSLATE_READ_TIMEOUT_SECONDS,
                                TimeUnit.SECONDS,
                            )
                            .proceed(req)
                    } else {
                        chain.proceed(req)
                    }
                }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpBuilder.build())
            .build()
            .create(service)
    }

    fun <T> createMoonAPIService(service: Class<T>): T {
        val moonDns =
            object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return if (hostname == MOON_BACKEND_HOSTNAME) {
                        listOf(InetAddress.getByName(MOON_BACKEND_IP))
                    } else {
                        Dns.SYSTEM.lookup(hostname)
                    }
                }
            }

        val httpBuilder =
            OkHttpClient.Builder()
                .connectTimeout(REQUIEST_TIME, TimeUnit.SECONDS)
                .writeTimeout(REQUIEST_TIME, TimeUnit.SECONDS)
                .readTimeout(REQUIEST_TIME, TimeUnit.SECONDS)
                .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .dns(moonDns)
                .proxy(Proxy.NO_PROXY)
        return Retrofit.Builder()
            .baseUrl(MOON_API_HOST)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpBuilder.build())
            .build()
            .create(service)
    }
}
