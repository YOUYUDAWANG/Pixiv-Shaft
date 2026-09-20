package ceui.pixiv.network.contract

/**
 * Contract providing localization language header for Pixiv API requests.
 */
interface LanguageProvider {
    /**
     * Returns standard Accept-Language value (e.g. "zh_CN", "en_US", "ja_JP").
     */
    fun getAcceptLanguage(): String
}
