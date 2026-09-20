package ceui.pixiv.api

object PixivClient {
    @Volatile
    var manager: PixivClientManager = PixivClientManager()

    @Volatile private var _appApi: API? = null
    @Volatile private var _webApi: PixivWebApi? = null
    @Volatile private var _comicApi: ComicApi? = null
    @Volatile private var _fanboxApi: FanboxApi? = null

    val appApi: API
        get() = _appApi ?: synchronized(this) {
            _appApi ?: manager.createAPPAPI(API::class.java).also { _appApi = it }
        }

    val webApi: PixivWebApi
        get() = _webApi ?: synchronized(this) {
            _webApi ?: manager.createWebAPIService(PixivWebApi::class.java).also { _webApi = it }
        }

    val comicApi: ComicApi
        get() = _comicApi ?: synchronized(this) {
            _comicApi ?: manager.createComicService(ComicApi::class.java).also { _comicApi = it }
        }

    val fanboxApi: FanboxApi
        get() = _fanboxApi ?: synchronized(this) {
            _fanboxApi ?: manager.createFanboxService(FanboxApi::class.java).also { _fanboxApi = it }
        }

    @Synchronized
    fun reset() {
        _appApi = null
        _webApi = null
        _comicApi = null
        _fanboxApi = null
    }
}
