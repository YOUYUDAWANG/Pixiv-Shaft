package ceui.pixiv.network.contract

/**
 * High-level network connection mode, supporting standard proxying and anti-blocking direct connections.
 */
enum class NetworkMode(val code: String) {
    Standard("standard"),
    DirectCompat("compat"),
    DirectEch("ech"),
    CustomProxy("proxy");

    companion object {
        fun fromCode(code: String?): NetworkMode =
            entries.firstOrNull { it.code == code } ?: Standard
    }
}
