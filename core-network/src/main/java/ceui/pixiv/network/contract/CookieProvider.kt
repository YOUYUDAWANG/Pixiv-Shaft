package ceui.pixiv.network.contract

interface CookieProvider {
    fun getWebCookie(): String
    fun getFanboxCookie(): String = ""
}
